import org.gradle.internal.os.OperatingSystem

/**
 * Native library compilation for macOS Gesture Detector
 * This script compiles Objective-C code and packages it into the desktop resources
 */

val nativeLibName = "GestureBridge"

val compileNativeMacOS by tasks.registering(Exec::class) {
    group = "native"
    description = "Compiles Objective-C gesture detector for macOS"

    onlyIf {
        OperatingSystem.current().isMacOsX
    }

    // Capture values outside of task configuration
    val projectDir = layout.projectDirectory
    val buildDir = layout.buildDirectory

    val sourceFile = projectDir.file("src/macosMain/objc/src/GestureBridge.m").asFile
    val outputDir = buildDir.dir("native/libs").get().asFile
    val outputFile = outputDir.resolve("lib$nativeLibName.dylib")

    inputs.file(sourceFile)
    outputs.file(outputFile)

    doFirst {
        outputDir.mkdirs()
        logger.lifecycle("Compiling native macOS library: $sourceFile")
    }

    commandLine(
        "clang",
        "-dynamiclib",
        "-o",
        outputFile.absolutePath,
        sourceFile.absolutePath,
        "-framework",
        "Cocoa",
        "-install_name",
        "@rpath/lib$nativeLibName.dylib",
        "-Wno-deprecated-declarations",
    )

    doLast {
        logger.lifecycle("Native library compiled: $outputFile")
    }
}

val copyNativeLibToResources by tasks.registering(Copy::class) {
    group = "native"
    description = "Copies compiled native library to desktopMain resources"

    dependsOn(compileNativeMacOS)

    // Use layout API properly
    val projectDir = layout.projectDirectory
    val buildDir = layout.buildDirectory

    from(buildDir.dir("native/libs")) {
        include("lib$nativeLibName.dylib")
    }
    into(projectDir.dir("src/desktopMain/resources"))

    doLast {
        logger.lifecycle("Native library copied to desktopMain/resources")
    }
}

val cleanNativeLib by tasks.registering(Delete::class) {
    group = "native"
    description = "Cleans compiled native libraries"

    val buildDir = layout.buildDirectory
    val projectDir = layout.projectDirectory

    delete(buildDir.dir("native"))
    delete(projectDir.dir("src/desktopMain/resources"))
}

// Hook into standard build lifecycle
afterEvaluate {

    tasks.named("clean") {
        dependsOn(cleanNativeLib)
    }

    tasks.named("desktopProcessResources") {
        dependsOn(copyNativeLibToResources)
    }

    tasks.named("desktopJar") {
        dependsOn(copyNativeLibToResources)
    }
}

// Expose for manual builds
tasks.register("buildNativeLibs") {
    group = "native"
    description = "Builds all native libraries"
    dependsOn(copyNativeLibToResources)
}
