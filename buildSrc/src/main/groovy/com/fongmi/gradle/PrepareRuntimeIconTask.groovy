package com.fongmi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class PrepareRuntimeIconTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract RegularFileProperty getIconFile()

    @OutputDirectory
    abstract DirectoryProperty getResourceDirectory()

    @TaskAction
    void prepareIcon() {
        def directory = resourceDirectory.get().asFile.toPath().resolve('drawable')
        Files.createDirectories(directory)
        Files.copy(iconFile.get().asFile.toPath(), directory.resolve(iconFile.get().asFile.name), StandardCopyOption.REPLACE_EXISTING)
    }
}
