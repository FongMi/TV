package com.whl.quickjs.wrapper;

/**
 * Created by Harlon Wang on 2023/8/26.
 * 该类仅提供给 Native 层调用
 */
public abstract class ModuleLoader {
    /**
     * 模块加载模式：
     * True 会调用 {@link #getModuleBytecode(String)}
     * False 会调用 {@link #getModuleStringCode(String)}
     *
     * @return 是否字节码模式
     */
    public abstract boolean isBytecodeMode();

    /**
     * 获取字节码代码内容
     *
     * @param moduleName 模块路径名，例如 "xxx.js"
     * @return 代码内容
     */
    public abstract byte[] getModuleBytecode(String moduleName);

    /**
     * 获取字符串代码内容
     *
     * @param moduleName 模块路径名，例如 "xxx.js"
     * @return 代码内容
     */
    public abstract String getModuleStringCode(String moduleName);


    /**
     * 该方法返回结果会作为 moduleName 参数给到 {@link #getModuleBytecode(String)}
     * 或者 {@link #getModuleStringCode(String)} 中使用，默认返回 moduleName。
     * 一般可以在这里对模块名称进行转换处理。
     *
     * @param baseModuleName 使用 Import 的所在模块名称
     * @param moduleName     需要加载的模块名称
     * @return 模块名称
     */
    public String moduleNormalizeName(String baseModuleName, String moduleName) {
        return moduleName;
    }
}
