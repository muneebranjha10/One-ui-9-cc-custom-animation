import java.io.File

plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: "${System.getProperty("user.home")}/Android/Sdk"
    val platformsDir = File(sdkRoot, "platforms")
    val androidJar = platformsDir.listFiles()
        ?.filter { it.isDirectory && File(it, "android.jar").exists() }
        ?.sortedByDescending { it.name }
        ?.firstOrNull()
        ?.let { File(it, "android.jar") }

    if (androidJar != null && androidJar.exists()) {
        compileOnly(files(androidJar))
    }
}
