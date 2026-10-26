package com.fongmi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest

abstract class VerifyPythonWheelsTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getWheels()

    @Input
    abstract MapProperty<String, String> getExpectedHashes()

    @TaskAction
    void verifyWheels() {
        def files = wheels.files
        def hashes = expectedHashes.get()
        if (hashes.isEmpty() || files.size() != hashes.size() || files.collect { it.name }.toSet() != hashes.keySet()) throw new GradleException('Python wheel inputs do not match the manifest')
        for (def file : files) verifyWheel(file, hashes[file.name])
    }

    private static void verifyWheel(File file, String expected) {
        def hash = MessageDigest.getInstance('SHA-256').digest(file.bytes).encodeHex().toString()
        if (hash != expected) throw new GradleException("Python wheel hash mismatch: ${file.name}")
    }
}
