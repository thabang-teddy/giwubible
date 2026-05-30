allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// Bump compileSdk on all plugin subprojects (not :app, which is already evaluated).
// This satisfies flutter_plugin_android_lifecycle's minCompileSdk = 36 requirement
// for plugins like file_picker whose own build.gradle still hardcodes 34.
subprojects {
    if (name != "app") {
        afterEvaluate {
            extensions.findByName("android")?.let {
                (it as com.android.build.gradle.LibraryExtension).compileSdk = 36
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
