package com.raven

import android.app.Application
import android.content.Context
import com.raven.data.db.RavenDatabase
import com.raven.data.model.repository.RoomModelRepository
import com.raven.data.model.storage.AndroidModelPathResolver
import com.raven.data.model.storage.FileModelStorage
import com.raven.data.model.storage.ModelPathResolver
import com.raven.data.model.storage.ModelStorage
import com.raven.data.model.validation.BasicGgufValidator
import com.raven.data.model.validation.GgufValidator
import com.raven.domain.model.repository.ModelRepository
import com.raven.domain.model.usecase.ImportModelUseCase
import com.raven.domain.model.usecase.SelectAndCopyModelFileUseCase
import com.raven.inference.local.LlamaCppInferenceRuntime
import com.raven.inference.local.LocalInferenceRuntime
import com.raven.inference.local.LocalLlmProvider
import com.raven.inference.provider.LlmProvider
import com.raven.inference.router.ProviderRouter

/**
 * Raven's composition root.
 *
 * Exactly one instance of each long-lived dependency is created here, so the UI never
 * builds repositories, runtime adapters or providers itself. The wiring follows
 * docs/ARCHITECTURE.md:
 *
 *   UI -> ViewModel -> ConversationEngine -> ProviderRouter -> LlmProvider
 *      -> LocalLlmProvider -> LlamaCppInferenceRuntime -> JNI -> llama.cpp
 */
class RavenApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Holds Raven's singletons. Constructed once per process.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: RavenDatabase = RavenDatabase.getInstance(appContext)

    val modelRepository: ModelRepository = RoomModelRepository(database.modelDao())

    val modelStorage: ModelStorage = FileModelStorage(AndroidModelPathResolver(appContext))

    val pathResolver: ModelPathResolver = AndroidModelPathResolver(appContext)

    val ggufValidator: GgufValidator = BasicGgufValidator(appContext, modelStorage::calculateSha256)

    val importModelUseCase: ImportModelUseCase =
        ImportModelUseCase(modelStorage, ggufValidator, modelRepository)

    val selectModelFileUseCase: SelectAndCopyModelFileUseCase =
        SelectAndCopyModelFileUseCase(appContext)

    /** The native llama.cpp adapter. Only the local provider may touch this. */
    private val localRuntime: LocalInferenceRuntime =
        LlamaCppInferenceRuntime(appContext, pathResolver)

    val localProvider: LlmProvider = LocalLlmProvider(modelRepository, localRuntime)

    val providerRouter: ProviderRouter = ProviderRouter().apply {
        registerProvider(localProvider)
    }
}