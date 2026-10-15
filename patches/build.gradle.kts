group = "app.wazeytm"

patches {
    about {
        name = "Waze YT Music Patches"
        description = "Lets Waze use a repackaged (Morphe/ReVanced patched) YouTube Music app for its built-in music controls"
        source = "git@github.com:Pilotkosinus/morphe-waze-patches.git"
        author = "Pilotkosinus"
        contact = "na"
        website = "https://github.com/Pilotkosinus/morphe-waze-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
