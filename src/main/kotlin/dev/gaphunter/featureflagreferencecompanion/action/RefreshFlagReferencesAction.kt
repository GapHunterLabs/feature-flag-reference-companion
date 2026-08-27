package dev.gaphunter.featureflagreferencecompanion.action

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import dev.gaphunter.featureflagreferencecompanion.index.FlagReferenceIndex

/**
 * The one and only trigger for [FlagReferenceIndex.refresh] -- a
 * project-wide scan is real work, so it runs only when the user
 * explicitly asks for it (Find Action: "Refresh Feature Flag
 * References", or Tools menu), never on every keystroke. Same "manual
 * refresh, heavy computation off the hot path" principle already used
 * by `circular-dependency-companion`.
 *
 * Runs the scan in a background [Task.Backgroundable] wrapped in a read
 * action (never on the EDT), then restarts the daemon highlighting
 * pass on completion so already-open editors
 * immediately reflect the freshly counted references without requiring
 * the user to touch each file.
 */
class RefreshFlagReferencesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val index = FlagReferenceIndex.getInstance(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Refreshing Feature Flag References", false) {
            override fun run(indicator: ProgressIndicator) {
                ReadAction.run<Throwable> { index.refresh() }
            }

            override fun onSuccess() {
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        DaemonCodeAnalyzer.getInstance(project).restart()
                    }
                }
            }
        })
    }
}
