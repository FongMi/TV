package com.fongmi.gradle

import groovy.json.JsonOutput
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import javax.inject.Inject
import java.security.MessageDigest

import static com.fongmi.gradle.AbiApkPackaging.PYTHON_LIBRARIES

abstract class PrepareRuntimeTask extends DefaultTask {

    private final FileSystemOperations fileSystemOperations

    @Inject
    PrepareRuntimeTask(FileSystemOperations fileSystemOperations) {
        this.fileSystemOperations = fileSystemOperations
        jniDirectory.set(outputDirectory.dir('lib'))
        assetsDirectory.set(outputDirectory.dir('assets'))
    }

    @Input
    abstract Property<String> getModule()

    @Input
    abstract Property<String> getAbi()

    @Input
    abstract Property<Integer> getVersionCode()

    @Input
    abstract Property<String> getVersionName()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getInputArchive()

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getBridgeArchive()

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getCxxAar()

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getFfmpegAar()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Internal
    abstract DirectoryProperty getJniDirectory()

    @Internal
    abstract DirectoryProperty getAssetsDirectory()

    @TaskAction
    void prepareRuntime() {
        def output = outputDirectory.get().asFile
        fileSystemOperations.delete { delete output }
        try {
            verifyVersion()
            def module = this.module.get()
            def abi = this.abi.get()
            if (!AbiApkPackaging.RUNTIME_PROFILES.containsKey(module) || !AbiApkPackaging.ABI_FLAVORS.containsValue(abi)) throw new GradleException('Invalid runtime module or ABI')
            preparePayload(output, module, abi)
        } catch (Exception error) {
            fileSystemOperations.delete { delete output }
            throw error
        }
    }

    private void verifyVersion() {
        if (versionCode.get() <= 0 || versionName.get().trim().isEmpty()) throw new GradleException('Runtime version must have a positive code and non-empty name')
    }

    private void preparePayload(File output, String module, String abi) {
        if (!(module in ['python', 'nodejs', 'thunder'])) jniDirectory.get().asFile.mkdirs()
        assetsDirectory.get().asFile.mkdirs()
        def files = []
        def hostLibraries = module == 'mpv' ? MpvNativeLibraries.hostLibraries(inputArchive.get().asFile, ffmpegAar.get().asFile, abi) : null
        collectRuntimeFiles(output, module, abi, files)
        writeRuntimeMetadata(output, module, files, abi, hostLibraries)
    }

    private void collectRuntimeFiles(File output, String module, String abi, List<Map> files) {
        if (module == 'bdj') files.add(writeFile(output, AbiApkPackaging.RUNTIME_PROFILES.bdj.archive, inputArchive.get().asFile))
        else prepareArchive(output, module, abi, files)
        if (module == 'quickjs') readQuickJsHelpers(files)
    }

    private void readQuickJsHelpers(List<Map> files) {
        ZipFile.builder().setFile(bridgeArchive.get().asFile).get().withCloseable { source ->
            source.entries.each { entry ->
                if (!entry.directory && entry.name.startsWith('assets/js/')) files.add(PrepareRuntimeTask.readPayload(source, entry, null, entry.name))
            }
        }
    }

    private void prepareArchive(File output, String module, String abi, List<Map> files) {
        ZipFile.builder().setFile(inputArchive.get().asFile).get().withCloseable { source ->
            if (module == 'python') PreparePythonAarTask.verifyPrepared(source, cxxAar.get().asFile, abi)
            def entries = source.entries
            while (entries.hasMoreElements()) PrepareRuntimeTask.collectArchiveEntry(source, entries.nextElement(), output, module, abi, files)
        }
    }

    private static void collectArchiveEntry(ZipFile source, ZipArchiveEntry entry, File output, String module, String abi, List<Map> files) {
        if (entry.directory) return
        def name = runtimePath(entry.name, module, abi)
        if (name == null) return
        files.add(readPayload(source, entry, module in ['python', 'nodejs', 'thunder'] ? null : output, name))
    }

    private static String runtimePath(String name, String module, String abi) {
        if (module == 'python' && name.startsWith('assets/chaquopy/')) return name
        if (module == 'nodejs' && name.startsWith('assets/nodejs/')) return name
        if (!name.startsWith("jni/${abi}/")) return null
        def library = name.substring(name.lastIndexOf('/') + 1)
        return library in nativeLibraries(module) ? "lib/${abi}/${library}".toString() : null
    }

    private static List<String> nativeLibraries(String module) {
        if (module == 'python') return PYTHON_LIBRARIES
        if (module == 'thunder') return AbiApkPackaging.THUNDER_LIBRARIES
        if (module == 'nodejs') return ['libnode.so', 'libnodebridge.so']
        if (module == 'mpv') return AbiApkPackaging.MPV_LIBRARIES
        return [AbiApkPackaging.RUNTIME_PROFILES[module].library]
    }

    protected void writeRuntimeMetadata(File output, String module, List<Map> files, String abi, Map hostLibraries = null) {
        if (files.collect { it.path }.toSet().size() != files.size()) throw new GradleException('Duplicate runtime payload')
        def profile = AbiApkPackaging.RUNTIME_PROFILES[module]
        def path = module == 'bdj' ? profile.archive : "lib/${abi}/${profile.library}".toString()
        def entry = files.find { it.path == path }
        if (!entry) throw new GradleException("Missing ${module} payload ${path}")
        if (module != 'bdj') {
            def names = nativeLibraries(module)
            if (files.findAll { it.path.startsWith('lib/') }.collect { it.path }.toSet() != names.collect { "lib/${abi}/${it}".toString() }.toSet()) throw new GradleException("Invalid ${module} native libraries for ${abi}")
        }
        def metadata = [module: module, packageName: profile.packageName,
                        versionCode: versionCode.get(), versionName: versionName.get(),
                        compatibilityId: profile.compatibilityId, abi: abi,
                        sha256: entry.sha256, size: entry.size]
        if (profile.entryClass != null) {
            metadata.entryClass = profile.entryClass
            metadata.javaApi = 1
        }
        metadata[module == 'bdj' ? 'archive' : 'library'] = path
        if (module == 'mpv') metadata.hostLibraries = hostLibraries
        metadata.files = files.sort { it.path }
        def bytes = JsonOutput.toJson(metadata).getBytes('UTF-8')
        if (bytes.length > 64 * 1024) throw new GradleException("Runtime metadata too large for ${module}")
        writeBytes(output, 'assets/runtime.json', bytes)
    }

    private static Map readPayload(ZipFile source, ZipArchiveEntry entry, File output, String path) {
        verifyPath(path)
        if (entry.size <= 0 || entry.size > 128L * 1024 * 1024) throw new GradleException("Invalid runtime payload ${path}")
        def digest = MessageDigest.getInstance('SHA-256')
        source.getInputStream(entry).withCloseable { input ->
            (output == null ? OutputStream.nullOutputStream() : outputFile(output, path).newOutputStream()).withCloseable { stream ->
                def buffer = new byte[8192]
                int count
                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count)
                    stream.write(buffer, 0, count)
                }
            }
        }
        return [path: path, size: entry.size, sha256: digest.digest().encodeHex().toString()]
    }

    private static Map writeFile(File output, String path, File file) {
        if (!file.isFile() || file.length() <= 0 || file.length() > 128L * 1024 * 1024) throw new GradleException("Invalid runtime payload ${file}")
        return writeBytes(output, path, file.bytes)
    }

    private static Map writeBytes(File output, String path, byte[] bytes) {
        if (bytes.length <= 0 || bytes.length > 128L * 1024 * 1024) throw new GradleException("Invalid runtime payload ${path}")
        outputFile(output, path).bytes = bytes
        return [path: path, size: bytes.length, sha256: MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString()]
    }

    private static File outputFile(File directory, String path) {
        verifyPath(path)
        def root = directory.toPath().toAbsolutePath().normalize()
        def target = root.resolve(path).normalize()
        if (!target.startsWith(root)) throw new GradleException("Invalid runtime payload path ${path}")
        if (target.toFile().exists()) throw new GradleException("Duplicate runtime payload ${path}")
        target.parent.toFile().mkdirs()
        return target.toFile()
    }

    private static void verifyPath(String path) {
        if (path.contains('\\') || path.split('/').any { it in ['', '.', '..'] || it.contains(':') }) throw new GradleException("Invalid runtime payload path ${path}")
    }
}
