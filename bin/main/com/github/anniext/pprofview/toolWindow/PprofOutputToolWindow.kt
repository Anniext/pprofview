package com.github.anniext.pprofview.toolWindow

import com.github.anniext.pprofview.parser.PprofTextParser
import com.github.anniext.pprofview.ui.PprofChartPanel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * pprof 输出工具窗口
 */
class PprofOutputToolWindow : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val outputPanel = PprofOutputPanel(project)
        val content = ContentFactory.getInstance().createContent(outputPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean = true
}

/**
 * pprof 输出面板
 */
class PprofOutputPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = thisLogger()
    private val tabbedPane = JBTabbedPane()
    private val outputs = mutableMapOf<String, JTextArea>()
    
    init {
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 添加输出标签页（文本）
     */
    fun addOutput(title: String, content: String) {
        val textArea = JTextArea(content)
        textArea.isEditable = false
        textArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        
        val scrollPane = JBScrollPane(textArea)
        tabbedPane.addTab(title, scrollPane)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
        
        outputs[title] = textArea
    }
    
    /**
     * 添加输出标签页（带可视化）
     */
    fun addOutputWithVisualization(title: String, content: String) {
        try {
            // 解析文本报告
            val parser = PprofTextParser()
            val report = parser.parse(content)
            
            if (report.entries.isNotEmpty()) {
                // 创建包含文本和图表的面板
                val panel = JPanel(BorderLayout())
                
                // 创建子选项卡
                val subTabbedPane = JBTabbedPane()
                
                // 添加原始文本
                val textArea = JTextArea(content)
                textArea.isEditable = false
                textArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
                subTabbedPane.addTab("原始数据", JBScrollPane(textArea))
                
                // 添加图表
                val chartPanel = PprofChartPanel(report)
                subTabbedPane.addTab("可视化", chartPanel)
                
                panel.add(subTabbedPane, BorderLayout.CENTER)
                
                tabbedPane.addTab(title, panel)
                tabbedPane.selectedIndex = tabbedPane.tabCount - 1
                
                logger.info("已添加可视化标签页: $title")
            } else {
                // 如果解析失败，只显示文本
                addOutput(title, content)
            }
        } catch (e: Exception) {
            logger.error("创建可视化失败", e)
            // 降级到纯文本显示
            addOutput(title, content)
        }
    }
    
    /**
     * 添加自定义组件标签页
     */
    fun addComponent(title: String, component: JComponent) {
        tabbedPane.addTab(title, component)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
    }
    
    /**
     * 清除所有输出
     */
    fun clearAll() {
        tabbedPane.removeAll()
        outputs.clear()
    }
    
    companion object {
        /**
         * 获取工具窗口实例
         */
        fun getInstance(project: Project): PprofOutputPanel? {
            val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("pprof Output") ?: return null
            
            val content = toolWindow.contentManager.getContent(0) ?: return null
            return content.component as? PprofOutputPanel
        }
    }
}
