package org.inferna.dashboard

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.messages.Topic
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import org.inferna.dashboard.actions.PurgeMemoryAction
import org.inferna.dashboard.actions.RefreshBiometricsAction
import org.inferna.dashboard.actions.RescanSpringAction
import org.inferna.dashboard.theme.BiopunkTheme
import org.inferna.dashboard.ui.BiometricSectionRenderer
import org.inferna.dashboard.ui.sections.GroovyBiometricsSection
import org.inferna.dashboard.ui.sections.JvmBiologicalSection
import org.inferna.dashboard.ui.sections.ProjectMatrixSection
import org.inferna.dashboard.ui.sections.ScalaBiometricsSection
import org.inferna.icons.InfernaIcons
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.Timer

class InfernaDashboardToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        private const val MEMORY_REFRESH_MS = 2000
    }

    override val icon: Icon get() = InfernaIcons.Dashboard

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(InfernaIcons.Dashboard)
        toolWindow.stripeTitle = "Inferna Biometrics"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Filtered once, up front: a section that isn't applicable (e.g. no Scala/Groovy
        // sources) never renders, so nothing downstream has to special-case it.
        val sections: List<BiometricSectionRenderer> = listOf(
            JvmBiologicalSection(),
            ProjectMatrixSection(),
            ScalaBiometricsSection(),
            GroovyBiometricsSection()
        ).filter { it.isApplicable(project) }

        val dslPanel = panel {
            customize(UnscaledGaps(8, 12, 8, 12))

            row {
                label(project.name.uppercase()).applyToComponent {
                    font = JBFont.h3().asBold()
                    foreground = BiopunkTheme.TEXT_PRIMARY
                }
            }

            // Each section draws its own title embedded inside a horizontal rule via
            // BiopunkTheme.biopunkGroup, so no manual separator() is needed between them.
            sections.forEach { section -> section.render(this, project, toolWindow.disposable) }
        }

        val refreshAll = { sections.forEach { it.refresh(project) } }

        val mainComponent = buildLayout(dslPanel, refreshAll)
        val content = ContentFactory.getInstance().createContent(mainComponent, null, false)
        toolWindow.contentManager.addContent(content)

        // Initial scan
        refreshAll()

        // Polling Timer
        val timer = Timer(MEMORY_REFRESH_MS) { refreshAll() }
        timer.start()

        // Compilation Listener
        val listener = object : CompilationStatusListener {
            override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
                refreshAll()
            }
        }
        @Suppress("UNCHECKED_CAST")
        val topic = CompilerTopics.COMPILATION_STATUS as Topic<CompilationStatusListener>
        project.messageBus.connect(content).subscribe(topic, listener)

        Disposer.register(content, Disposable { timer.stop() })
    }

    private fun buildLayout(content: JComponent, onRefreshAll: () -> Unit): JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshBiometricsAction(onRefreshAll))
            add(Separator.create())
            add(RescanSpringAction(onRefreshAll))
            add(PurgeMemoryAction())
        }

        val toolbar = ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLWINDOW_CONTENT,
            actionGroup,
            true
        ).apply {
            targetComponent = content
            component.border = BorderFactory.createEmptyBorder(4, 12, 4, 12)
            component.isOpaque = false
        }

        val scrollPane = JBScrollPane(
            content,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            border = BorderFactory.createEmptyBorder(4, 12, 4, 12)
            viewportBorder = null
            isOpaque = false
            viewport.isOpaque = false
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            isOpaque = false
            add(toolbar.component, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }
}