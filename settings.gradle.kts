pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppliLoupGarou"

include(":engine")

// Le module Android n'est inclus que si le SDK Android est disponible
// (toujours le cas sur GitHub Actions et dans Android Studio).
val sdkDisponible = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").takeIf { it.exists() }?.readText()?.contains("sdk.dir") == true
if (sdkDisponible) include(":app")
