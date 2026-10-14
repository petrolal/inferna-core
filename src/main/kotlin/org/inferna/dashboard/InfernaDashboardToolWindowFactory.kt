package org.inferna.dashboard

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.SwingConstants

class InfernaDashboardToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            background = Color(0x0A, 0x04, 0x05)
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        }

        val headerLabel = JBLabel("INFERNA BIOMETRICS", SwingConstants.CENTER).apply {
            foreground = Color(0xE6, 0x19, 0x2E)
            font = font.deriveFont(Font.BOLD, 14f)
        }

        val runtime = Runtime.getRuntime()
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        val statsPanel = JBPanel<JBPanel<*>>(GridLayout(4, 1, 8, 8)).apply {
            background = Color(0x14, 0x07, 0x09)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(0x2D, 0x0E, 0x12)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )

            add(JBLabel("Project: ${project.name}").apply { foreground = Color(0xE3, 0xD1, 0xD1) })
            add(JBLabel("Subsystem: Active").apply { foreground = Color(0xB8, 0x14, 0x24) })
            add(JBLabel("JVM Memory: ${usedMemoryMb}MB / ${totalMemoryMb}MB").apply { foreground = Color(0x7A, 0x63, 0x63) })
            add(JBLabel("Polyglot Engine: Java / Kotlin UAST").apply { foreground = Color(0x7A, 0x63, 0x63) })
        }

        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(statsPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "Overview", false)
        toolWindow.contentManager.addContent(content)
    }
}
