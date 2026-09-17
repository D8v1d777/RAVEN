package com.raven.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.raven.AppContainer
import com.raven.RavenApplication
import com.raven.data.model.storage.ModelStorage
import com.raven.data.model.validation.DeviceSuitability
import com.raven.data.model.validation.GgufValidator
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.repository.ModelRepository
import com.raven.domain.model.usecase.ImportModelUseCase
import com.raven.domain.model.usecase.SelectAndCopyModelFileUseCase
import com.raven.inference.router.ProviderRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Real model management for the local provider: list, import, load, unload, delete.
 *
 * Every reported state reflects an actual operation. A load is only attempted after the
 * device suitability check has run, and failures surface the underlying reason.
 */
class ModelCatalogViewModel(
    private val modelRepository: ModelRepository,
    private val modelStorage: ModelStorage,
    private val ggufValidator: GgufValidator,
    private val importModelUseCase: ImportModelUseCase,
    private val selectModelFileUseCase: SelectAndCopyModelFileUseCase,
    private val router: ProviderRouter,
) : ViewModel() {

    private val _state = MutableStateFlow(ModelCatalogState())
    val state: StateFlow<ModelCatalogState> = _state.asStateFlow()

    /** Guards the one-shot "remember my model" restore. */
    private var restoreAttempted = false

    init {
        // Live list straight from the registry: no polling and no manual refresh needed.
        viewModelScope.launch {
            modelRepository.observeAllModels().collect { models ->
                val ordered = models.sortedByDescending { it.lastUsedAtEpochMs ?: it.installedAtEpochMs }
                _state.update {
                    it.copy(
                        models = ordered,
                        storageUsedBytes = ordered.sumOf { model -> model.fileSizeBytes },
                    )
                }
            }
        }

        viewModelScope.launch {
            runCatching { modelStorage.availableStorageBytes() }.onSuccess { available ->
                _state.update { it.copy(storageAvailableBytes = available) }
            }
        }

        // The provider is the single source of truth for what is actually in memory.
        viewModelScope.launch {
            router.getSelectedProvider()?.selectedModel?.collect { descriptor ->
                _state.update {
                    it.copy(loadedModelId = descriptor?.id, loadedModelLabel = descriptor?.displayName)
                }
            }
        }
    }

    /**
     * Loads the model the user used last, once per process, so Raven is usable immediately.
     * Never runs when a model is already loaded, and reports a real failure if the load fails.
     */
    fun restoreLastUsedModel() {
        if (restoreAttempted) return
        restoreAttempted = true

        val current = _state.value
        if (current.loadedModelId != null || current.isWorking) return

        val candidate = current.models.firstOrNull() ?: return
        loadModel(candidate.id.value)
    }

    fun loadModel(modelId: String) {
        if (_state.value.isWorking || _state.value.loadedModelId == modelId) return

        viewModelScope.launch {
            val id = runCatching { ModelId(modelId) }.getOrNull()
            val metadata = id?.let { modelRepository.getModelById(it) }
            if (metadata == null) {
                _state.update { it.copy(errorMessage = "That model is no longer installed.") }
                return@launch
            }

            val suitability = ggufValidator.estimateDeviceSuitability(
                metadata.capabilities,
                metadata.fileSizeBytes,
            )

            if (suitability is DeviceSuitability.Unsupported) {
                _state.update {
                    it.copy(
                        errorMessage = "${metadata.displayName} cannot run on this device. " +
                            suitability.reason,
                    )
                }
                return@launch
            }

            val warning = when (suitability) {
                is DeviceSuitability.Caution -> suitability.reason
                is DeviceSuitability.NotRecommended -> suitability.reason
                else -> null
            }

            _state.update {
                it.copy(
                    isWorking = true,
                    workLabel = "Loading ${metadata.displayName} into memory\u2026",
                    errorMessage = null,
                    warningMessage = warning,
                )
            }

            val provider = router.getSelectedProvider()
            val result = provider?.selectModel(modelId)
                ?: Result.failure(IllegalStateException("No local provider is registered"))

            _state.update {
                it.copy(
                    isWorking = false,
                    workLabel = null,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }

            runCatching { modelStorage.availableStorageBytes() }.onSuccess { available ->
                _state.update { it.copy(storageAvailableBytes = available) }
            }
        }
    }

    fun unloadModel() {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, workLabel = "Releasing model memory\u2026") }
            val result = router.getSelectedProvider()?.unload()
            _state.update {
                it.copy(
                    isWorking = false,
                    workLabel = null,
                    errorMessage = result?.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun deleteModel(modelId: String) {
        if (_state.value.isWorking) return

        viewModelScope.launch {
            val id = runCatching { ModelId(modelId) }.getOrNull()
            if (id == null) {
                _state.update { it.copy(errorMessage = "Invalid model identifier.") }
                return@launch
            }

            _state.update {
                it.copy(isWorking = true, workLabel = "Deleting model\u2026", errorMessage = null)
            }

            // A model file cannot be deleted while llama.cpp still has it loaded.
            if (_state.value.loadedModelId == modelId) {
                router.getSelectedProvider()?.unload()
            }

            val failure = runCatching {
                modelStorage.deleteModel(id)
                modelRepository.deleteModel(id)
            }.exceptionOrNull()

            _state.update {
                it.copy(
                    isWorking = false,
                    workLabel = null,
                    errorMessage = failure?.message,
                    statusMessage = if (failure == null) "Model deleted." else null,
                )
            }
        }
    }

    /**
     * Imports a GGUF chosen in the system document picker and loads it when validation
     * succeeds, so the user can start talking immediately.
     */
    fun importModelFromUri(uri: Uri) {
        if (_state.value.isWorking) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isWorking = true,
                    workLabel = "Copying model file\u2026",
                    errorMessage = null,
                    statusMessage = null,
                )
            }

            try {
                val displayName = selectModelFileUseCase.getDisplayName(uri)
                val localFile = selectModelFileUseCase.copyUriToLocalFile(uri, displayName)
                _state.update { it.copy(workLabel = "Validating and registering\u2026") }

                val metadata = importModelUseCase.importModel(localFile)
                localFile.delete()

                _state.update {
                    it.copy(
                        isWorking = false,
                        workLabel = null,
                        statusMessage = "Imported ${metadata.displayName}",
                    )
                }

                loadModel(metadata.id.value)
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isWorking = false,
                        workLabel = null,
                        errorMessage = error.message ?: "Import failed.",
                    )
                }
            }
        }
    }

    fun dismissMessages() {
        _state.update { it.copy(errorMessage = null, statusMessage = null, warningMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RavenApplication
                val container: AppContainer = application.container
                ModelCatalogViewModel(
                    modelRepository = container.modelRepository,
                    modelStorage = container.modelStorage,
                    ggufValidator = container.ggufValidator,
                    importModelUseCase = container.importModelUseCase,
                    selectModelFileUseCase = container.selectModelFileUseCase,
                    router = container.providerRouter,
                )
            }
        }
    }
}

data class ModelCatalogState(
    val models: List<ModelMetadata> = emptyList(),
    val loadedModelId: String? = null,
    val loadedModelLabel: String? = null,
    val isWorking: Boolean = false,
    val workLabel: String? = null,
    val statusMessage: String? = null,
    val warningMessage: String? = null,
    val errorMessage: String? = null,
    val storageUsedBytes: Long = 0L,
    val storageAvailableBytes: Long = 0L,
) {
    val hasModels: Boolean get() = models.isNotEmpty()
}