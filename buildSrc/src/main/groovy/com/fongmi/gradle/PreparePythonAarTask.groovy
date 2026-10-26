package com.fongmi.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

import javax.inject.Inject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.CRC32

abstract class PreparePythonAarTask extends DefaultTask {

    private static final String ASSETS = 'assets/chaquopy/'
    private static final String CXX = 'chaquopy/lib/libc++_shared.so'
    private final ExecOperations execOperations

    @Inject
    PreparePythonAarTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    @Input
    abstract Property<String> getAbi()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getInputArchive()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getCxxAar()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getPatchelfFile()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getAppArchive()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getNativeArchive()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getNativeMetadata()

    @Input
    abstract Property<String> getRuntimeVersion()

    @OutputFile
    abstract RegularFileProperty getOutputArchive()

    @TaskAction
    void preparePythonAar() {
        def input = inputArchive.get().asFile
        def output = outputArchive.get().asFile
        if ([input, cxxAar.get().asFile, patchelfFile.get().asFile, appArchive.get().asFile, nativeArchive.get().asFile, nativeMetadata.get().asFile].any { it.canonicalFile == output.canonicalFile }) throw new GradleException('Python output must not replace an input')
        Files.deleteIfExists(output.toPath())
        def python = new File(temporaryDir, 'libpython3.10.so')
        def operations = execOperations
        try {
            ZipFile.builder().setFile(input).get().withCloseable { source ->
                def build = PreparePythonAarTask.readBuild(source, abi.get(), false)
                def nativeLibraries = PreparePythonAarTask.readNativeArchive(nativeArchive.get().asFile, nativeMetadata.get().asFile, abi.get(), runtimeVersion.get())
                def assets = PreparePythonAarTask.prepareAssets(source, build, abi.get(), cxxAar.get().asFile, appArchive.get().asFile, nativeLibraries)
                PreparePythonAarTask.writePreparedArchive(source, output, python, build, assets,
                        abi.get(), patchelfFile.get().asFile, operations)
            }
            ZipFile.builder().setFile(output).get().withCloseable { source -> PreparePythonAarTask.verifyPrepared(source, cxxAar.get().asFile, abi.get()) }
        } catch (Exception error) {
            Files.deleteIfExists(output.toPath())
            throw error
        } finally {
            Files.deleteIfExists(python.toPath())
        }
    }

    private static Map prepareAssets(ZipFile source, Map build, String abi, File cxxAar, File appArchive, Map nativeLibraries) {
        def requirements = PreparePythonAarTask.cxxRequirements(source, abi)
        def cxx = PreparePythonAarTask.readCxx(cxxAar, abi)
        def replacements = nativeLibraries.findAll { path, bytes -> path.startsWith('jni/') }
        for (def name : [requirements.path, ASSETS + 'requirements-common.imy'].unique()) replacements[name] = PreparePythonAarTask.prepareRequirements(PreparePythonAarTask.readEntry(source, name), cxx)
        PreparePythonAarTask.prepareNativeAssets(source, replacements, nativeLibraries, abi)
        build.assets = build.assets.findAll { path, hash -> path != 'app.imy' }
        def appBytes = appArchive.bytes
        if (appBytes.length <= 0 || appBytes.length > 64 * 1024 * 1024) throw new GradleException('Invalid Python helper archive')
        build.assets['app.imy'] = PreparePythonAarTask.sha1(appBytes)
        replacements.findAll { name, bytes -> name.startsWith(ASSETS) }.each { name, bytes -> build.assets[name.substring(ASSETS.length())] = PreparePythonAarTask.sha1(bytes) }
        return [replacements: replacements, appBytes: appBytes]
    }

    private static Map readNativeArchive(File archive, File metadata, String abi, String version) {
        def report = new JsonSlurper().parse(metadata).abis[abi]
        if (!(report instanceof Map) || report.abi != abi || report.inputs?.chaquopyVersion != version || !report.inputs?.sources?.python?.version?.startsWith('3.10.') || !(report.libraries instanceof List)) throw new GradleException('Incompatible Python native metadata')
        if (report.sha256 != MessageDigest.getInstance('SHA-256').digest(archive.bytes).encodeHex().toString()) throw new GradleException('Python native archive hash mismatch')
        ZipFile.builder().setFile(archive).get().withCloseable { source ->
            def names = entryNames(source)
            def libraries = AbiApkPackaging.PYTHON_LIBRARIES.collect { "jni/${abi}/${it}".toString() }.toSet()
            if (names.findAll { it.startsWith('jni/') }.toSet() != libraries || !names.contains('bridge/chaquopy.so') || names.any { !it.startsWith("jni/${abi}/") && it != 'bridge/chaquopy.so' && !it.startsWith('dynload/') }) throw new GradleException('Invalid Python native layout')
            if (report.libraries.collect { it.path }.toSet() != names) throw new GradleException('Incomplete Python native metadata')
            def result = [:]
            report.libraries.each { library ->
                def bytes = readEntry(source, library.path)
                if (library.sha256 != MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString()) throw new GradleException("Invalid Python native hash ${library.path}")
                verifyNativeLibrary(bytes, abi, library.path)
                result[library.path] = bytes
            }
            return result
        }
    }

    private static void verifyNativeLibrary(byte[] bytes, String abi, String path) {
        MpvNativeLibraries.needed(bytes, abi)
        boolean wide = abi == 'arm64-v8a'
        def data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        long headers = wide ? data.getLong(32) : Integer.toUnsignedLong(data.getInt(28))
        int stride = Short.toUnsignedInt(data.getShort(wide ? 54 : 42))
        int count = Short.toUnsignedInt(data.getShort(wide ? 56 : 44))
        for (int index = 0; index < count; index++) {
            int position = Math.toIntExact(headers + (long) index * stride)
            int type = data.getInt(position)
            long offset = wide ? data.getLong(position + 8) : Integer.toUnsignedLong(data.getInt(position + 4))
            long address = wide ? data.getLong(position + 16) : Integer.toUnsignedLong(data.getInt(position + 8))
            long size = wide ? data.getLong(position + 40) : Integer.toUnsignedLong(data.getInt(position + 20))
            long alignment = wide ? data.getLong(position + 48) : Integer.toUnsignedLong(data.getInt(position + 28))
            if (type == 1 && (alignment < 16384 || (address - offset) % 16384 != 0)) throw new GradleException("${path}: LOAD must support 16 KB pages")
            if (type == 0x6474e552 && (address + size) % 16384 != 0) throw new GradleException("${path}: RELRO must support 16 KB pages")
        }
    }

    private static void prepareNativeAssets(ZipFile source, Map replacements, Map nativeLibraries, String abi) {
        entryNames(source).findAll { it.startsWith(ASSETS + "bootstrap-native/${abi}/") && it.endsWith('.so') }.each { path ->
            def library = path.endsWith('/java/chaquopy.so') ? 'bridge/chaquopy.so' : 'dynload/' + path.substring(path.lastIndexOf('/') + 1)
            replacements[path] = nativeLibrary(nativeLibraries, library)
        }
        def path = ASSETS + "stdlib-${abi}.imy"
        replacements[path] = prepareStdlib(readEntry(source, path), nativeLibraries)
    }

    private static byte[] nativeLibrary(Map nativeLibraries, String path) {
        if (!nativeLibraries.containsKey(path)) throw new GradleException("Missing Python native library ${path}")
        return nativeLibraries[path]
    }

    private static byte[] prepareStdlib(byte[] archive, Map nativeLibraries) {
        ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(archive)).get().withCloseable { input ->
            def bytes = new ByteArrayOutputStream()
            new ZipArchiveOutputStream(bytes).withCloseable { output ->
                input.entries.each { entry ->
                    if (entry.name.endsWith('.so')) writeEntry(output, entry.name, nativeLibrary(nativeLibraries, 'dynload/' + entry.name.substring(entry.name.lastIndexOf('/') + 1)))
                    else input.getRawInputStream(entry).withCloseable { output.addRawArchiveEntry(new ZipArchiveEntry(entry), it) }
                }
            }
            return bytes.toByteArray()
        }
    }

    private static void writePreparedArchive(ZipFile source, File output, File python, Map build, Map assets,
                                             String abi, File patchelf, ExecOperations operations) {
        Files.createDirectories(output.parentFile.toPath())
        new ZipArchiveOutputStream(output).withCloseable { target ->
            source.entries.each { entry ->
                PreparePythonAarTask.writePreparedEntry(source, target, entry, python, build, assets, abi, patchelf, operations)
            }
            PreparePythonAarTask.writeEntry(target, ASSETS + 'app.imy', assets.appBytes)
        }
    }

    private static void writePreparedEntry(ZipFile source, ZipArchiveOutputStream target, ZipArchiveEntry entry,
                                           File python, Map build, Map assets, String abi, File patchelf, ExecOperations operations) {
        def name = entry.name
        if (entry.directory || name == ASSETS + 'app.imy' || !retained(name, abi)) return
        if (name == "jni/${abi}/libpython3.10.so") {
            writePythonLibrary(target, entry, assets.replacements[name], python, patchelf, operations, abi)
        } else if (assets.replacements.containsKey(name)) {
            writeEntry(target, name, assets.replacements[name])
        } else if (name == ASSETS + 'build.json') {
            writeEntry(target, name, JsonOutput.toJson(build).getBytes('UTF-8'))
        } else {
            source.getRawInputStream(entry).withCloseable { stream ->
                target.addRawArchiveEntry(new ZipArchiveEntry(entry), stream)
            }
        }
    }

    private static void writePythonLibrary(ZipArchiveOutputStream target, ZipArchiveEntry entry, byte[] bytes,
                                           File python, File patchelf, ExecOperations operations, String abi) {
        python.bytes = bytes
        operations.exec {
            commandLine patchelf.absolutePath, '--page-size', '16384',
                    '--set-soname', 'libpython3.10.so', python.absolutePath
        }
        if (python.length() <= 0 || python.length() > 128L * 1024 * 1024) throw new GradleException('Invalid Python native library')
        PreparePythonAarTask.verifyNativeLibrary(python.bytes, abi, entry.name)
        PreparePythonAarTask.writeEntry(target, entry.name, python.bytes)
    }

    static void verifyPrepared(ZipFile source, File cxxAar, String abi) {
        readBuild(source, abi, true)
        def requirements = cxxRequirements(source, abi)
        ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(requirements.bytes)).get().withCloseable { input ->
            if (!Arrays.equals(PreparePythonAarTask.readEntry(input, CXX), PreparePythonAarTask.readCxx(cxxAar, abi))) throw new GradleException('Python C++ runtime does not match the App')
        }
        if (abi == 'arm64-v8a') verifyPreparedNative(source, abi)
    }

    private static void verifyPreparedNative(ZipFile source, String abi) {
        entryNames(source).findAll { it.endsWith('.so') }.each { path -> verifyNativeLibrary(readEntry(source, path), abi, path) }
        for (def name : ['requirements-common.imy', "requirements-${abi}.imy", "stdlib-${abi}.imy"]) {
            def archive = readEntry(source, ASSETS + name)
            ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(archive)).get().withCloseable { input ->
                entryNames(input).findAll { it.endsWith('.so') }.each { path -> verifyNativeLibrary(readEntry(input, path), abi, name + "!/" + path) }
            }
        }
    }

    private static boolean retained(String path, String abi) {
        if (path == 'classes.jar' || path == 'proguard.txt' || path.startsWith('libs/')) return false
        if (path.startsWith('assets/')) return path.startsWith(ASSETS)
        if (path.startsWith('jni/')) return path in AbiApkPackaging.PYTHON_LIBRARIES.collect { "jni/${abi}/${it}".toString() }
        return true
    }

    private static Map readBuild(ZipFile source, String abi, boolean prepared) {
        if (!AbiApkPackaging.ABI_FLAVORS.containsValue(abi)) throw new GradleException('Invalid Python ABI')
        def names = entryNames(source)
        if (prepared && names.any { !retained(it, abi) }) throw new GradleException('Unexpected prepared Python payload')
        def libraries = AbiApkPackaging.PYTHON_LIBRARIES.collect { "jni/${abi}/${it}".toString() }.toSet()
        for (def path : libraries) {
            def entry = source.getEntry(path)
            if (!entry || entry.directory || entry.size <= 0 || entry.size > 128L * 1024 * 1024) throw new GradleException("Invalid Python native library ${path}")
        }
        def required = ['requirements-common.imy', "requirements-${abi}.imy".toString(), 'stdlib-common.imy', "stdlib-${abi}.imy".toString(), 'bootstrap.imy', 'cacert.pem']
        if (names.any { path -> ((path.startsWith(ASSETS + 'requirements-') || path.startsWith(ASSETS + 'stdlib-')) && !(path.substring(ASSETS.length()) in required)) || (path.startsWith(ASSETS + 'bootstrap-native/') && !path.startsWith(ASSETS + "bootstrap-native/${abi}/")) }) throw new GradleException("Unexpected Python assets for ${abi}")
        if (!names.containsAll(required.collect { ASSETS + it }) || !names.any { it.startsWith(ASSETS + "bootstrap-native/${abi}/") }) throw new GradleException("Incomplete Python assets for ${abi}")
        def build = new JsonSlurper().parse(readEntry(source, ASSETS + 'build.json'), 'UTF-8')
        if (build.python_version != '3.10' || !(build.assets instanceof Map)) throw new GradleException('Invalid Python build metadata')
        def assets = names.findAll { it.startsWith(ASSETS) && it != ASSETS + 'build.json' }.collect { it.substring(ASSETS.length()) }.toSet()
        if (assets != build.assets.keySet()) throw new GradleException('Incomplete Python asset hashes')
        build.assets.each { path, hash ->
            if (hash != PreparePythonAarTask.sha1(PreparePythonAarTask.readEntry(source, ASSETS + path))) throw new GradleException("Invalid Python asset hash ${path}")
        }
        return build
    }

    private static Set<String> entryNames(ZipFile source) {
        def names = new HashSet<String>()
        def files = new HashSet<String>()
        source.entries.each { entry -> PreparePythonAarTask.collectEntryName(entry, names, files) }
        return files
    }

    private static void collectEntryName(ZipArchiveEntry entry, Set<String> names, Set<String> files) {
        def path = entry.directory ? entry.name.substring(0, entry.name.length() - 1) : entry.name
        if (path.contains('\\') || path.split('/', -1).any { it in ['', '.', '..'] || it.contains(':') }) throw new GradleException("Invalid Python payload path ${entry.name}")
        if (!names.add(path)) throw new GradleException("Duplicate Python payload ${path}")
        if (!entry.directory) files.add(path)
    }

    private static Map cxxRequirements(ZipFile source, String abi) {
        def candidates = []
        for (def name : ['requirements-common.imy', "requirements-${abi}.imy".toString()]) {
            def path = ASSETS + name
            def bytes = readEntry(source, path)
            ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(bytes)).get().withCloseable { input ->
                if (PreparePythonAarTask.entryNames(input).contains(CXX)) candidates.add([path: path, bytes: bytes])
            }
        }
        if (candidates.size() != 1) throw new GradleException('Exactly one Python C++ runtime is required')
        return candidates[0]
    }

    private static byte[] readCxx(File archive, String abi) {
        ZipFile.builder().setFile(archive).get().withCloseable { readEntry(it, "jni/${abi}/libc++_shared.so".toString()) }
    }

    private static byte[] readEntry(ZipFile source, String path) {
        def entry = source.getEntry(path)
        if (!entry || entry.directory || entry.size <= 0 || entry.size > 128L * 1024 * 1024) throw new GradleException("Invalid Python payload ${path}")
        return source.getInputStream(entry).withCloseable { it.readAllBytes() }
    }

    private static byte[] prepareRequirements(byte[] requirements, byte[] cxx) {
        ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(requirements)).get().withCloseable { input ->
            def bytes = new ByteArrayOutputStream()
            new ZipArchiveOutputStream(bytes).withCloseable { output ->
                input.entries.each { entry ->
                    if (entry.name == 'yapftests' || entry.name.startsWith('yapftests/')) return
                    if (entry.name == CXX) PreparePythonAarTask.writeEntry(output, CXX, cxx)
                    else input.getRawInputStream(entry).withCloseable { output.addRawArchiveEntry(new ZipArchiveEntry(entry), it) }
                }
            }
            return bytes.toByteArray()
        }
    }

    private static String sha1(byte[] bytes) {
        return MessageDigest.getInstance('SHA-1').digest(bytes).encodeHex().toString()
    }

    private static void writeEntry(ZipArchiveOutputStream output, String path, byte[] bytes) {
        def crc = new CRC32()
        crc.update(bytes)
        def entry = new ZipArchiveEntry(path)
        entry.time = 0L
        entry.method = java.util.zip.ZipEntry.STORED
        entry.size = bytes.length
        entry.crc = crc.value
        output.putArchiveEntry(entry)
        output.write(bytes)
        output.closeArchiveEntry()
    }
}
