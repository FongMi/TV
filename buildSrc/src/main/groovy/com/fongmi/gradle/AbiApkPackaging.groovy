package com.fongmi.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class AbiApkPackaging {

    static final Map<String, String> ABI_FLAVORS = [arm64: 'arm64-v8a', arm32: 'armeabi-v7a'].asImmutable()
    static final List<String> PYTHON_LIBRARIES = ['libchaquopy_java.so', 'libcrypto_chaquopy.so', 'libcrypto_python.so', 'libpython3.10.so', 'libpython3.so', 'libsqlite3_chaquopy.so', 'libsqlite3_python.so', 'libssl_chaquopy.so', 'libssl_python.so'].asImmutable()
    static final List<String> THUNDER_LIBRARIES = ['libxl_stat.so', 'libxl_thunder_sdk.so'].asImmutable()
    static final List<String> MPV_LIBRARIES = ['libmpv.so', 'libplayer.so'].asImmutable()
    static final Map<String, Map> RUNTIME_PROFILES = [
        python: [packageName: 'com.fongmi.runtime.python', compatibilityId: 'catvod-python-plugin-v2', entryClass: 'com.fongmi.chaquo.RuntimeEntry', library: 'libchaquopy_java.so', label: 'Python', property: 'tvPython'],
        nodejs: [packageName: 'com.fongmi.runtime.nodejs', compatibilityId: 'catvod-nodejs-plugin-v2', entryClass: 'com.fongmi.nodejs.RuntimeEntry', library: 'libnode.so', label: 'Node.js', property: 'tvNodeJs'],
        quickjs: [packageName: 'com.fongmi.runtime.quickjs', compatibilityId: 'fongmi-quickjs-ng-0.17-v11', entryClass: 'com.fongmi.quickjs.RuntimeEntry', library: 'libquickjs-wrapper.so', label: 'QuickJS', property: 'tvQuickJs'],
        thunder: [packageName: 'com.fongmi.runtime.thunder', compatibilityId: 'catvod-thunder-plugin-v1', entryClass: 'com.xunlei.downloadlib.RuntimeEntry', library: 'libxl_thunder_sdk.so', label: 'Thunder', property: 'tvThunder'],
        bdj: [packageName: 'com.fongmi.runtime.bdj', compatibilityId: 'libbluray-1.4.1-openjdk8-zero-v1', archive: 'assets/bdj/runtime.zip', label: 'BD-J', property: 'tvBdj'],
        mpv: [packageName: 'com.fongmi.runtime.mpv', compatibilityId: 'mpv-jni-ffmpeg-shared-v1', library: 'libplayer.so', label: 'mpv', property: 'tvMpv']
    ].asImmutable()

    static void configure(Project project, Object apkArtifact) {
        rejectRetiredModes(project)
        def components = project.extensions.getByName('androidComponents')
        configureDebugResourceCacheKey(project, components)
        components.onVariants(components.selector().all()) { variant ->
            def device = variant.productFlavors.find { it.first == 'device' }?.second
            if (!device) throw new GradleException("Missing device flavor for ${variant.name}")
            def abis = configureOutputs(variant, device)
            configureNativeLibraries(project, variant, abis)
            if (variant.buildType == 'release') configureReleaseExport(project, variant, device, apkArtifact)
        }
    }

    private static List<String> configureOutputs(def variant, String device) {
        return variant.outputs.collect { output -> configureOutput(variant, device, output) }
    }

    private static String configureOutput(def variant, String device, def output) {
        def abi = output.filters.find { it.filterType.toString() == 'ABI' }?.identifier
        if (!ABI_FLAVORS.containsValue(abi)) throw new GradleException("Missing or unsupported ABI for ${variant.name}")
        output.outputFileName.set(hostFileName(device, abi, variant.buildType))
        return abi
    }

    private static void configureNativeLibraries(Project project, def variant, List<String> abis) {
        variant.packaging.jniLibs.excludes.addAll((THUNDER_LIBRARIES + MPV_LIBRARIES + [RUNTIME_PROFILES.nodejs.library, RUNTIME_PROFILES.quickjs.library]).collect { "**/${it}".toString() })
        variant.packaging.jniLibs.keepDebugSymbols.add('**/libc++_shared.so')
        def validation = project.tasks.register("verify${variant.name.capitalize()}MpvNative", VerifyMpvNativeTask) { task ->
            task.mpvArchive.set(project.layout.projectDirectory.file('libs/lib-mpvplayer-release.aar'))
            task.ffmpegArchive.set(project.layout.projectDirectory.file('libs/lib-decoder-ffmpeg-release.aar'))
            task.abis.set(abis)
        }
        project.tasks.matching { it.name == "merge${variant.name.capitalize()}NativeLibs" }.configureEach { it.dependsOn(validation) }
    }

    private static void configureReleaseExport(Project project, def variant, String device, Object apkArtifact) {
        project.tasks.register("export${device.capitalize()}ReleaseApks", Copy) { task ->
            task.from(variant.artifacts.get(apkArtifact)) { include '*.apk' }
            task.into(project.rootProject.layout.projectDirectory.dir('Release/apk'))
        }
    }

    static String otherAbi(String abi) {
        if (!ABI_FLAVORS.containsValue(abi)) throw new GradleException("Unsupported ABI ${abi}")
        return ABI_FLAVORS.values().find { it != abi }
    }

    static String hostFileName(String device, String abi, String buildType) {
        return buildType == 'release' ? "${device}-${abi.replace('-', '_')}.apk" : "app-${device}-${abi}-${buildType}.apk"
    }

    private static void rejectRetiredModes(Project project) {
        if (project.providers.gradleProperty('tvRuntimeStandalone').isPresent()) throw new GradleException('tvRuntimeStandalone has been removed; build the app and independent runtime APKs')
        for (def name : ['tvRuntimeSplits', 'tvQuickJsStandalone']) if (project.providers.gradleProperty(name).map { it.toBoolean() }.getOrElse(false)) throw new GradleException("${name} has been removed; build independent runtime APKs instead")
    }

    private static void configureDebugResourceCacheKey(Project project, def components) {
        def injectedApi = project.providers.gradleProperty('android.injected.build.api').orElse('')
        def injectedAbis = project.providers.gradleProperty('android.injected.build.abi').orElse('')
        components.onVariants(components.selector().withBuildType('debug')) { variant ->
            def taskName = "process${variant.name.capitalize()}Resources"
            project.tasks.named({ it == taskName } as org.gradle.api.specs.Spec).configureEach { task ->
                task.inputs.property('androidInjectedBuildApi', injectedApi)
                task.inputs.property('androidInjectedBuildAbis', injectedAbis)
            }
        }
    }
}
