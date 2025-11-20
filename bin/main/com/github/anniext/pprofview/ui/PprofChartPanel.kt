package com.github.anniext.pprofview.ui

import com.github.anniext.pprofview.parser.PprofTextReport
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import javax.swing.*

/**
 * pprof 图表面板
 * 用于可视化展示 pprof 数据
 */
class PprofChartPanel(private val report: PprofTextReport) : JBPanel<PprofChartPanel>(BorderLayout()) {
    
    init {
        // 创建选项卡面板
        val tabbedPane = JTabbedPane()
        
        // 添加柱状图
        tabbedPane.addTab("柱状图", createBarChartPanel())
        
        // 添加饼图
        tabbedPane.addTab("饼图", createPieChartPanel())
        
        // 添加表格视图
        tabbedPane.addTab("表格", createTablePanel())
        
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 创建柱状图面板
     */
    private fun createBarChartPanel(): JComponent {
        val panel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawBarChart(g as Graphics2D)
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制柱状图
     */
    private fun drawBarChart(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        val margin = 60
        val chartWidth = width - 2 * margin
        val chartHeight = height - 2 * margin
        
        // 取前 20 个条目
        val topEntries = report.entries.take(20)
        if (topEntries.isEmpty()) return
        
        // 绘制标题
        g.color = JBColor.BLACK
        g.font = Font("SansSerif", Font.BOLD, 16)
        g.drawString("Top ${topEntries.size} 函数性能分析 (${report.unit})", margin, margin - 30)
        
        // 绘制坐标轴
        g.color = JBColor.GRAY
        g.drawLine(margin, margin, margin, height - margin) // Y 轴
        g.drawLine(margin, height - margin, width - margin, height - margin) // X 轴
        
        // 计算柱状图参数
        val barWidth = chartWidth / topEntries.size
        val maxValue = topEntries.maxOfOrNull { it.flat } ?: 1L
        
        // 绘制柱状图
        topEntries.forEachIndexed { index, entry ->
            val barHeight = (entry.flat.toDouble() / maxValue * chartHeight).toInt()
            val x = margin + index * barWidth + barWidth / 4
            val y = height - margin - barHeight
            
            // 绘制柱子
            g.color = getBarColor(index)
            g.fillRect(x, y, barWidth / 2, barHeight)
            
            // 绘制边框
            g.color = JBColor.DARK_GRAY
            g.drawRect(x, y, barWidth / 2, barHeight)
            
            // 绘制数值
            g.color = JBColor.BLACK
            g.font = Font("SansSerif", Font.PLAIN, 10)
            val valueText = String.format("%.1f%%", entry.flatPercent)
            g.drawString(valueText, x, y - 5)
            
            // 绘制函数名 (旋转)
            g.font = Font("SansSerif", Font.PLAIN, 9)
            val fm = g.fontMetrics
            val funcName = truncateFunctionName(entry.functionName, 20)
            
            val transform = g.transform
            g.rotate(-Math.PI / 4, (x + barWidth / 4).toDouble(), (height - margin + 10).toDouble())
            g.drawString(funcName, x + barWidth / 4, height - margin + 10)
            g.transform = transform
        }
        
        // 绘制 Y 轴刻度
        g.color = JBColor.GRAY
        g.font = Font("SansSerif", Font.PLAIN, 10)
        for (i in 0..5) {
            val y = height - margin - (chartHeight * i / 5)
            val value = maxValue * i / 5
            g.drawLine(margin - 5, y, margin, y)
            g.drawString(formatValue(value), margin - 50, y + 5)
        }
    }
    
    /**
     * 创建饼图面板
     */
    private fun createPieChartPanel(): JComponent {
        val panel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawPieChart(g as Graphics2D)
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制饼图
     */
    private fun drawPieChart(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 3
        
        // 取前 10 个条目
        val topEntries = report.entries.take(10)
        if (topEntries.isEmpty()) return
        
        // 绘制标题
        g.color = JBColor.BLACK
        g.font = Font("SansSerif", Font.BOLD, 16)
        g.drawString("Top ${topEntries.size} 函数占比 (${report.unit})", 50, 30)
        
        // 计算总和
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // 绘制饼图
        var startAngle = 0.0
        topEntries.forEachIndexed { index, entry ->
            val angle = (entry.flat / total) * 360.0
            
            // 绘制扇形
            g.color = getBarColor(index)
            g.fillArc(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            // 绘制边框
            g.color = JBColor.DARK_GRAY
            g.drawArc(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            startAngle += angle
        }
        
        // 绘制图例
        val legendX = width - 250
        var legendY = 100
        
        topEntries.forEachIndexed { index, entry ->
            // 绘制颜色块
            g.color = getBarColor(index)
            g.fillRect(legendX, legendY, 20, 20)
            g.color = JBColor.DARK_GRAY
            g.drawRect(legendX, legendY, 20, 20)
            
            // 绘制文本
            g.color = JBColor.BLACK
            g.font = Font("SansSerif", Font.PLAIN, 11)
            val text = String.format("%.1f%% %s", entry.flatPercent, truncateFunctionName(entry.functionName, 25))
            g.drawString(text, legendX + 30, legendY + 15)
            
            legendY += 30
        }
    }
    
    /**
     * 创建表格面板
     */
    private fun createTablePanel(): JComponent {
        val columnNames = arrayOf("函数名", "Flat", "Flat%", "Sum%", "Cum", "Cum%")
        val data = report.entries.map { entry ->
            arrayOf(
                entry.functionName,
                formatValue(entry.flat),
                String.format("%.2f%%", entry.flatPercent),
                String.format("%.2f%%", entry.sumPercent),
                formatValue(entry.cum),
                String.format("%.2f%%", entry.cumPercent)
            )
        }.toTypedArray()
        
        val table = JTable(data, columnNames)
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        table.font = Font("Monospaced", Font.PLAIN, 12)
        
        return JBScrollPane(table)
    }
    
    /**
     * 获取柱状图颜色
     */
    private fun getBarColor(index: Int): Color {
        val colors = arrayOf(
            Color(66, 133, 244),   // 蓝色
            Color(234, 67, 53),    // 红色
            Color(251, 188, 5),    // 黄色
            Color(52, 168, 83),    // 绿色
            Color(255, 109, 0),    // 橙色
            Color(156, 39, 176),   // 紫色
            Color(0, 172, 193),    // 青色
            Color(255, 87, 34),    // 深橙色
            Color(121, 85, 72),    // 棕色
            Color(158, 158, 158)   // 灰色
        )
        return colors[index % colors.size]
    }
    
    /**
     * 截断函数名
     */
    private fun truncateFunctionName(name: String, maxLength: Int): String {
        if (name.length <= maxLength) return name
        
        // 尝试只保留函数名部分
        val parts = name.split(".")
        val funcName = parts.lastOrNull() ?: name
        
        return if (funcName.length <= maxLength) {
            funcName
        } else {
            funcName.substring(0, maxLength - 3) + "..."
        }
    }
    
    /**
     * 格式化数值
     */
    private fun formatValue(value: Long): String {
        return when {
            value >= 1000000 -> String.format("%.2fM", value / 1000000.0)
            value >= 1000 -> String.format("%.2fK", value / 1000.0)
            else -> value.toString()
        }
    }
}
