pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Insangram"

// Insangram uses a single Gradle module with strictly separated source
// packages (core / domain / data / feature). See docs/ARCHITECTURE.md for the
// rationale and for the layer boundary rules that are enforced by review.
include(":app")
