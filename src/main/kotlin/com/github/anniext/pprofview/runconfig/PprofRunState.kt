package com.github.anniext.pprofview.runconfig

import com.github.anniext.pprofview.actions.VisualizationType
import com.github.anniext.pprofview.services.PprofVisualizationService
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * Pprof 运行状态
 */
class PprofRunState(
    environment: ExecutionEnvironment,
    private val configuration: PprofConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val logger = thisLogger()
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        
        // 添加构建标志
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // 根据运行种类添加参数
        val runKind = PprofRunKind.fromString(configuration.runKind)
        when (runKind) {
            PprofRunKind.FILE -> {
                if (configuration.filePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.filePath)
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                }
            }
        }
        
        // 如果启用了 pprof 且是运行时采样模式，注入 pprof 初始化文件（放在用户文件之后）
        var pprofInitFile: File? = null
        logger.info("Pprof 配置: enablePprof=${configuration.enablePprof}, collectionMode=${configuration.collectionMode}")
        if (configuration.enablePprof && 
            PprofCollectionMode.fromString(configuration.collectionMode) == PprofCollectionMode.RUNTIME_SAMPLING) {
            logger.info("开始注入 pprof 初始化文件...")
            pprofInitFile = injectPprofInit()
            if (pprofInitFile != null) {
                commandLine.addParameter(pprofInitFile.absolutePath)
                logger.info("已注入 pprof 初始化文件: ${pprofInitFile.absolutePath}")
            } else {
                logger.warn("注入 pprof 初始化文件失败")
            }
        } else {
            logger.info("跳过 pprof 注入")
        }
        
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
            logger.info("设置 PPROF_OUTPUT_DIR=${outputDir.absolutePath}")
            
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
            logger.info("Profile types: ${configuration.profileTypes}")
            configuration.profileTypes.split(",").forEach { typeStr ->
                val type = PprofProfileType.fromString(typeStr.trim())
                if (type != null) {
                    when (type) {
                        PprofProfileType.CPU -> {
                            commandLine.environment["PPROF_ENABLE_CPU"] = "true"
                            logger.info("启用 CPU profiling")
                        }
                        PprofProfileType.HEAP -> {
                            commandLine.environment["PPROF_ENABLE_HEAP"] = "true"
                            logger.info("启用 HEAP profiling")
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
        
        // 打印完整的命令行用于调试
        logger.info("执行命令: ${commandLine.commandLineString}")
        logger.info("工作目录: ${commandLine.workDirectory}")
        logger.info("环境变量: ${commandLine.environment.filter { it.key.startsWith("PPROF_") }}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // 如果启用了自动打开结果，启动后台任务监控 pprof 文件生成
        logger.info("autoOpenResult=${configuration.autoOpenResult}")
        if (configuration.enablePprof && configuration.autoOpenResult) {
            val outputDir = getOutputDirectory()
            logger.info("启动后台监控任务，输出目录: ${outputDir.absolutePath}")
            
            // 使用定时器定期检查文件
            val timer = java.util.Timer()
            val checkTask = object : java.util.TimerTask() {
                private var lastFileCount = 0
                private var stableCount = 0
                
                override fun run() {
                    val pprofFiles = outputDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".pprof")
                    }
                    
                    val currentCount = pprofFiles?.size ?: 0
                    
                    // 如果文件数量稳定（连续 3 次检查都相同且大于 0），说明采样完成
                    if (currentCount > 0 && currentCount == lastFileCount) {
                        stableCount++
                        if (stableCount >= 3) {
                            logger.info("检测到 pprof 文件生成完成，共 $currentCount 个文件")
                            cancel()
                            timer.cancel()
                            
                            // 清理临时文件
                            pprofInitFile?.delete()
                            
                            // 打开可视化
                            autoOpenVisualization(outputDir)
                        }
                    } else {
                        stableCount = 0
                    }
                    
                    lastFileCount = currentCount
                }
            }
            
            // 每 2 秒检查一次，最多检查 5 分钟
            timer.schedule(checkTask, 2000, 2000)
            
            // 5 分钟后自动取消
            java.util.Timer().schedule(object : java.util.TimerTask() {
                override fun run() {
                    timer.cancel()
                    pprofInitFile?.delete()
                }
            }, 300000)
            
            // 同时注册进程终止监听器作为备用
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("进程已终止，退出码: ${event.exitCode}")
                    timer.cancel()
                    pprofInitFile?.delete()
                }
            })
        } else if (pprofInitFile != null) {
            // 即使不自动打开，也要清理临时文件
            logger.info("不自动打开，仅注册清理监听器")
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("进程已终止（仅清理），退出码: ${event.exitCode}")
                    pprofInitFile?.delete()
                }
            })
        }
        
        return processHandler
    }
    
    /**
     * 注入 pprof 初始化文件
     */
    private fun injectPprofInit(): File? {
        val logger = thisLogger()
        try {
            // 从资源中读取 pprof_init.go 模板
            val inputStream = javaClass.classLoader.getResourceAsStream("pprof_runtime/pprof_init.go")
                ?: return null
            
            // 确定目标目录 - 必须与用户代码在同一目录
            val targetDir = when (PprofRunKind.fromString(configuration.runKind)) {
                PprofRunKind.FILE -> {
                    if (configuration.filePath.isNotEmpty()) {
                        File(configuration.filePath).parentFile
                    } else null
                }
                PprofRunKind.DIRECTORY -> {
                    if (configuration.directoryPath.isNotEmpty()) {
                        File(configuration.directoryPath)
                    } else null
                }
                else -> null
            } ?: File(configuration.workingDirectory).takeIf { it.exists() } ?: return null
            
            // 在目标目录创建临时文件（不能以 . 开头，否则会被 Go 构建工具忽略）
            val tempFile = File(targetDir, "zzz_pprofview_init_${System.currentTimeMillis()}.go")
            
            // 写入内容
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            logger.info("创建 pprof 初始化文件: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            logger.error("无法注入 pprof 初始化文件", e)
            return null
        }
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
    
    /**
     * 自动打开可视化
     */
    private fun autoOpenVisualization(outputDir: File) {
        val logger = thisLogger()
        ApplicationManager.getApplication().invokeLater {
            val project = environment.project
            
            // 查找生成的 pprof 文件
            val pprofFiles = outputDir.listFiles { file ->
                file.isFile && file.name.endsWith(".pprof")
            }
            
            if (pprofFiles.isNullOrEmpty()) {
                logger.warn("未找到生成的 pprof 文件: ${outputDir.absolutePath}")
                return@invokeLater
            }
            
            logger.info("找到 ${pprofFiles.size} 个 pprof 文件")
            
            // 刷新文件系统
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputDir)
            
            // 为每个 pprof 文件生成文本报告并显示在 pprof Output 窗口
            pprofFiles.sortedBy { it.name }.forEach { file ->
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                if (virtualFile != null) {
                    logger.info("生成 ${file.name} 的文本报告")
                    // 使用 TEXT 类型在 pprof Output 窗口显示
                    val visualizationService = project.service<PprofVisualizationService>()
                    visualizationService.visualize(virtualFile, VisualizationType.TEXT)
                }
            }
        }
    }
}
