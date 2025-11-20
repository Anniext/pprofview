package com.github.anniext.pprofview.services

import com.github.anniext.pprofview.actions.VisualizationType
import com.github.anniext.pprofview.toolWindow.PprofOutputPanel
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.regex.Pattern

/**
 * pprof 可视化服务
 * 负责调用 go tool pprof 进行数据可视化
 */
@Service(Service.Level.PROJECT)
class PprofVisualizationService(private val project: Project) {
    private val logger = thisLogger()
    
    /**
     * 可视化 pprof 文件
     */
    fun visualize(file: VirtualFile, type: VisualizationType) {
        logger.info("开始可视化 pprof 文件: ${file.path}, 类型: ${type.name}")
        
        when (type) {
            VisualizationType.WEB -> visualizeInBrowser(file)
            VisualizationType.TEXT -> visualizeAsText(file)
            VisualizationType.GRAPH -> generateSvg(file, "graph")
            VisualizationType.FLAMEGRAPH -> generateSvg(file, "flame")
            VisualizationType.TOP -> showTop(file)
            VisualizationType.LIST -> showList(file)
            VisualizationType.PEEK -> showPeek(file)
        }
    }
    
    /**
     * 在浏览器中打开交互式可视化
     */
    private fun visualizeInBrowser(file: VirtualFile) {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "pprof", "-http=:0", file.path)
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.STDERR) {
                        // 匹配 "Serving web UI on http://localhost:xxxxx"
                        val pattern = Pattern.compile("http://[^\\s]+")
                        val matcher = pattern.matcher(text)
                        if (matcher.find()) {
                            val url = matcher.group()
                            logger.info("检测到 pprof web 服务地址: $url")
                            openInBrowser(url)
                            
                            showNotification(
                                "pprof 可视化已启动",
                                "浏览器将自动打开 $url\n关闭浏览器后，进程会自动停止",
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("pprof web 服务已停止")
                }
            })
            
            processHandler.startNotify()
            
            showNotification(
                "正在启动 pprof Web 服务",
                "请稍候，浏览器将自动打开...",
                NotificationType.INFORMATION
            )
        } catch (e: Exception) {
            logger.error("启动 pprof web 服务失败", e)
            showNotification(
                "启动失败",
                "无法启动 pprof web 服务: ${e.message}\n请确保已安装 Go 工具链",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 显示文本格式报告
     */
    private fun visualizeAsText(file: VirtualFile) {
        executeAndShowOutput(file, listOf("-text"), "文本报告")
    }
    
    /**
     * 生成 SVG 图表
     */
    private fun generateSvg(file: VirtualFile, graphType: String) {
        val outputFile = File(file.parent.path, "${file.nameWithoutExtension}_$graphType.svg")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "pprof", "-$graphType", "-output=${outputFile.absolutePath}", file.path)
        
        try {
            val process = commandLine.createProcess()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && outputFile.exists()) {
                logger.info("SVG 文件已生成: ${outputFile.absolutePath}")
                
                // 刷新文件系统
                file.parent.refresh(false, false)
                
                // 在浏览器中打开
                openInBrowser(outputFile.toURI().toString())
                
                showNotification(
                    "SVG 已生成",
                    "文件保存在: ${outputFile.absolutePath}",
                    NotificationType.INFORMATION
                )
            } else {
                showNotification(
                    "生成失败",
                    "无法生成 SVG 文件，退出码: $exitCode",
                    NotificationType.ERROR
                )
            }
        } catch (e: Exception) {
            logger.error("生成 SVG 失败", e)
            showNotification(
                "生成失败",
                "无法生成 SVG: ${e.message}",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 显示 Top 函数
     */
    private fun showTop(file: VirtualFile) {
        executeAndShowOutput(file, listOf("-top"), "Top 函数")
    }
    
    /**
     * 显示函数列表
     */
    private fun showList(file: VirtualFile) {
        executeAndShowOutput(file, listOf("-list=."), "函数列表")
    }
    
    /**
     * 显示简要信息
     */
    private fun showPeek(file: VirtualFile) {
        executeAndShowOutput(file, listOf("-peek=."), "简要信息")
    }
    
    /**
     * 执行命令并显示输出
     */
    private fun executeAndShowOutput(file: VirtualFile, args: List<String>, title: String) {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("tool")
        commandLine.addParameter("pprof")
        commandLine.addParameters(args)
        commandLine.addParameter(file.path)
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            val output = StringBuilder()
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    output.append(event.text)
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode == 0) {
                        // 在工具窗口中显示输出
                        showOutputInToolWindow(title, output.toString())
                    } else {
                        showNotification(
                            "执行失败",
                            "命令执行失败，退出码: ${event.exitCode}",
                            NotificationType.ERROR
                        )
                    }
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error("执行 pprof 命令失败", e)
            showNotification(
                "执行失败",
                "无法执行 pprof 命令: ${e.message}",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 在工具窗口中显示输出
     */
    private fun showOutputInToolWindow(title: String, content: String) {
        ApplicationManager.getApplication().invokeLater {
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("pprof Output")
            toolWindow?.show {
                // 获取输出面板并添加内容（带可视化）
                val outputPanel = PprofOutputPanel.getInstance(project)
                outputPanel?.addOutputWithVisualization(title, content)
            }
            
            // 同时显示通知
            val lines = content.lines().take(5)
            val preview = lines.joinToString("\n")
            showNotification(
                title,
                preview + if (content.lines().size > 5) "\n...\n查看 pprof Output 工具窗口获取完整输出和可视化图表" else "",
                NotificationType.INFORMATION
            )
        }
        
        logger.info("$title 输出:\n$content")
    }
    
    /**
     * 在浏览器中打开 URL
     */
    private fun openInBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logger.error("无法打开浏览器", e)
        }
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
}
