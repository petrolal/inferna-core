package org.inferna.dashboard

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.concurrency.AppExecutorUtil
import org.inferna.icons.InfernaIcons
import org.inferna.spring.SpringBeanResolver
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.SwingConstants
import javax.swing.Timer

class InfernaDashboardToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        private val PANEL_BACKGROUND = JBColor(0xF5F0F0, 0x0A0405)
        private val STATS_BACKGROUND = JBColor(0xEAE0E0, 0x140709)
        private val STATS_BORDER = JBColor(0xD8C4C4, 0x2D0E12)
        private val HEADER_FOREGROUND = JBColor(0xB8142E, 0xE6192E)
        private val SUBSYSTEM_FOREGROUND = JBColor(0x961020, 0xB81424)
        private val PROJECT_FOREGROUND = JBColor(0x221414, 0xE3D1D1)
        private val SECONDARY_FOREGROUND = JBColor(0x6E5A5A, 0x7A6363)
        private const val MEMORY_REFRESH_MS = 2000
    }

    override val icon: Icon
        get() = InfernaIcons.Dashboard

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(InfernaIcons.Dashboard)
        toolWindow.stripeTitle = "Inferna Dashboard"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setIcon(InfernaIcons.Dashboard)

        val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            background = PANEL_BACKGROUND
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        }

        val headerLabel = JBLabel("INFERNA BIOMETRICS", InfernaIcons.Dashboard, SwingConstants.CENTER).apply {
            foreground = HEADER_FOREGROUND
            font = font.deriveFont(Font.BOLD, 14f)
            iconTextGap = 8
        }

        val memoryLabel = JBLabel(formatMemoryStats()).apply { foreground = SECONDARY_FOREGROUND }
        val compilationLabel = JBLabel("Compilation: not run yet").apply { foreground = SUBSYSTEM_FOREGROUND }
        val springBeansLabel = JBLabel("Spring beans detected: scanning...").apply { foreground = SECONDARY_FOREGROUND }

        val statsPanel = JBPanel<JBPanel<*>>(GridLayout(4, 1, 8, 8)).apply {
            background = STATS_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STATS_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )

            add(JBLabel("Project: ${project.name}").apply { foreground = PROJECT_FOREGROUND })
            add(compilationLabel)
            add(memoryLabel)
            add(springBeansLabel)
        }

        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(statsPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "Overview", false)
        toolWindow.contentManager.addContent(content)

        val memoryTimer = Timer(MEMORY_REFRESH_MS) { memoryLabel.text = formatMemoryStats() }
        memoryTimer.start()

        refreshSpringBeanStatsAsync(project, springBeansLabel)

        val compilationListener = object : CompilationStatusListener {
            override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
                compilationLabel.text = formatCompilationStats(aborted, errors, warnings)
                refreshSpringBeanStatsAsync(project, springBeansLabel)
            }
        }
        project.messageBus.connect(content).subscribe(CompilerTopics.COMPILATION_STATUS, compilationListener)

        content.setDisposer {
            memoryTimer.stop()
        }
    }

    private fun refreshSpringBeanStatsAsync(project: Project, label: JBLabel) {
        if (project.isDisposed) return
        ReadAction.nonBlocking<Int> {
            SpringBeanResolver.countProjectBeans(project)
        }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { count ->
                label.text = "Spring beans detected: $count"
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun formatMemoryStats(): String {
        val runtime = Runtime.getRuntime()
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb
        return "JVM Memory: ${usedMemoryMb}MB / ${totalMemoryMb}MB"
    }

    private fun formatCompilationStats(aborted: Boolean, errors: Int, warnings: Int): String {
        return when {
            aborted -> "Compilation: aborted"
            errors > 0 -> "Compilation: $errors error(s), $warnings warning(s)"
            else -> "Compilation: clean ($warnings warning(s))"
        }
    }
}

