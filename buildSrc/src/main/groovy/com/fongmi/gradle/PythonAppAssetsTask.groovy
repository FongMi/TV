package com.fongmi.gradle

import groovy.json.JsonOutput
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

import java.security.MessageDigest

@CacheableTask
abstract class PythonAppAssetsTask extends Zip {

    @OutputDirectory
    abstract DirectoryProperty getAssetsDirectory()

    PythonAppAssetsTask() {
        archiveFileName.set('app.imy')
        destinationDirectory.set(assetsDirectory.dir('chaquopy'))
        preserveFileTimestamps = false
        reproducibleFileOrder = true
        metadataCharset = 'UTF-8'
        exclude '**/__pycache__/**', '**/*.pyc'
    }

    @Override
    @TaskAction
    protected void copy() {
        super.copy()
        def hash = MessageDigest.getInstance('SHA-1').digest(archiveFile.get().asFile.bytes).encodeHex().toString()
        new File(destinationDirectory.get().asFile, 'build.json').setText(JsonOutput.toJson([assets: ['app.imy': hash]]), 'UTF-8')
    }
}
