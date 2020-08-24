package org.jetbrains.plugins.feature.suggester

import com.intellij.internal.statistic.local.ActionsLocalSummary
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.plugins.feature.suggester.actions.Action
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

data class Selection(val startOffset: Int, val endOffset: Int, val text: String)

internal fun Editor.getSelection(): Selection? {
    with(selectionModel) {
        return if (selectedText != null) {
            Selection(selectionStart, selectionEnd, selectedText!!)
        } else {
            null
        }
    }
}

internal fun handleAction(project: Project, action: Action) {
    project.getService(FeatureSuggestersManager::class.java)
        ?.actionPerformed(action)
}

internal inline fun <reified T : PsiElement> PsiElement.getParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java)
}

internal fun PsiElement.getParentByPredicate(predicate: (PsiElement) -> Boolean): PsiElement? {
    return parents.find(predicate)
}

internal fun Transferable.asString(): String? {
    return try {
        getTransferData(DataFlavor.stringFlavor) as? String
    } catch (ex: Exception) {
        null
    }
}

@Suppress("UnstableApiUsage")
internal fun actionsLocalSummary(): ActionsLocalSummary {
    return ApplicationManager.getApplication().getService(ActionsLocalSummary::class.java)
}

internal fun createTipSuggestion(
    popupMessage: String,
    suggesterId: String,
    suggestingTipFilename: String
): Suggestion {
    return if (isRedoOrUndoRunning()) {
        NoSuggestion
    } else {
        TipSuggestion(popupMessage, suggesterId, suggestingTipFilename)
    }
}

internal fun createDocumentationSuggestion(
    popupMessage: String,
    suggesterId: String,
    suggestingDocUrl: String
): Suggestion {
    return if (isRedoOrUndoRunning()) {
        NoSuggestion
    } else {
        DocumentationSuggestion(popupMessage, suggesterId, suggestingDocUrl)
    }
}

private fun isRedoOrUndoRunning(): Boolean {
    val commandName = CommandProcessor.getInstance().currentCommandName
    return commandName != null && (commandName.startsWith("Redo") || commandName.startsWith("Undo"))
}