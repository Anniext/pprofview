package com.github.anniext.pprofview.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
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
    private val tabbedPane = JBTabbedPane()
    private val outputs = mutableMapOf<String, JTextArea>()
    
    init {
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 添加输出标签页
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
