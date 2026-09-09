#!/usr/bin/env sh
# Copyright notice (optional)
# Gradle wrapper script for Raven project

# Determine the absolute path of the directory containing this script
APP_HOME=$(dirname "$0")
# Set GRADLE_HOME to the extracted Gradle distribution
GRADLE_HOME="$APP_HOME/gradle/wrapper/gradle-8.5"

# Execute Gradle with the provided arguments
exec "$GRADLE_HOME/bin/gradle" "$@"