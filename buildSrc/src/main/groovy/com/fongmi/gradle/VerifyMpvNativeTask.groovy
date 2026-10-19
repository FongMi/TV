package com.fongmi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyMpvNativeTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getMpvArchive()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getFfmpegArchive()

    @Input
    abstract ListProperty<String> getAbis()

    @TaskAction
    void verifyNativeLibraries() {
        for (def abi : abis.get()) MpvNativeLibraries.hostLibraries(mpvArchive.get().asFile, ffmpegArchive.get().asFile, abi)
    }
}
