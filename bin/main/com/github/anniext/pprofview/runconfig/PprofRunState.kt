package com.github.anniext.pprofview.runconfig

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.io.FileUtil
import java.io.File

/**
 * Pprof 运行状态
 */
class PprofRunState(
    environment: ExecutionEnvironment,
    private val configuration: PprofConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        commandLine.addParameter(configuration.goFilePath)
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // 添加程序参数
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // 添加环境变量
        if (configuration.environmentVariables.isNotEmpty()) {
            configuration.environmentVariables.split(";").forEach { envVar ->
                val parts = envVar.split("=", limit = 2)
                if (parts.size == 2) {
                    commandLine.environment[parts[0]] = parts[1]
                }
            }
        }
        
        // 添加 pprof 相关的环境变量
        if (configuration.enablePprof) {
            val outputDir = getOutputDirectory()
            commandLine.environment["PPROF_OUTPUT_DIR"] = outputDir.absolutePath
            
            // 设置采样率
            if (configuration.memProfileRate > 0) {
                commandLine.environment["PPROF_MEM_RATE"] = configuration.memProfileRate.toString()
            }
            
            if (configuration.blockProfileRate > 0) {
                commandLine.environment["PPROF_BLOCK_RATE"] = configuration.blockProfileRate.toString()
            }
            
            if (configuration.mutexProfileFraction > 0) {
                commandLine.environment["PPROF_MUTEX_FRACTION"] = configuration.mutexProfileFraction.toString()
            }
            
            // 设置 CPU 采样持续时间
            commandLine.environment["PPROF_CPU_DURATION"] = configuration.cpuDuration.toString()
            
            // 设置启用的分析类型
            configuration.profileTypes.split(",").forEach { typeStr ->
                val type = PprofProfileType.fromString(typeStr.trim())
                if (type != null) {
                    when (type) {
                        PprofProfileType.CPU -> {
                            commandLine.environment["PPROF_ENABLE_CPU"] = "true"
                        }
                        PprofProfileType.HEAP -> {
                            commandLine.environment["PPROF_ENABLE_HEAP"] = "true"
                        }
                        PprofProfileType.GOROUTINE -> {
                            commandLine.environment["PPROF_ENABLE_GOROUTINE"] = "true"
                        }
                        PprofProfileType.BLOCK -> {
                            commandLine.environment["PPROF_ENABLE_BLOCK"] = "true"
                        }
                        PprofProfileType.MUTEX -> {
                            commandLine.environment["PPROF_ENABLE_MUTEX"] = "true"
                        }
                        PprofProfileType.ALLOCS -> {
                            commandLine.environment["PPROF_ENABLE_ALLOCS"] = "true"
                        }
                        else -> {
                            // 其他类型暂不支持
                        }
                    }
                }
            }
        }
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
    
    /**
     * 获取输出目录
     */
    private fun getOutputDirectory(): File {
        val dirPath = if (configuration.outputDirectory.isNotEmpty()) {
            configuration.outputDirectory
        } else {
            FileUtil.getTempDirectory()
        }
        
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }
}
