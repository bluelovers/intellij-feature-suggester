package org.jetbrains.plugins.feature.suggester.listeners

import com.intellij.codeInsight.completion.actions.CodeCompletionAction
import com.intellij.codeInsight.lookup.impl.actions.ChooseItemAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.BackspaceAction
import com.intellij.openapi.editor.actions.CopyAction
import com.intellij.openapi.editor.actions.CutAction
import com.intellij.openapi.editor.actions.EscapeAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.actions.PasteAction
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.plugins.feature.suggester.actions.BeforeCompletionChooseItemAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorBackspaceAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorCodeCompletionAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorCopyAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorCutAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorEscapeAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorFindAction
import org.jetbrains.plugins.feature.suggester.actions.BeforeEditorPasteAction
import org.jetbrains.plugins.feature.suggester.actions.CompletionChooseItemAction
import org.jetbrains.plugins.feature.suggester.actions.EditorBackspaceAction
import org.jetbrains.plugins.feature.suggester.actions.EditorCodeCompletionAction
import org.jetbrains.plugins.feature.suggester.actions.EditorCopyAction
import org.jetbrains.plugins.feature.suggester.actions.EditorCutAction
import org.jetbrains.plugins.feature.suggester.actions.EditorEscapeAction
import org.jetbrains.plugins.feature.suggester.actions.EditorFindAction
import org.jetbrains.plugins.feature.suggester.actions.EditorPasteAction
import org.jetbrains.plugins.feature.suggester.asString
import org.jetbrains.plugins.feature.suggester.getSelection
import org.jetbrains.plugins.feature.suggester.handleAction
import java.lang.ref.WeakReference

object EditorActionsListener : AnActionListener {
    private val copyPasteManager = CopyPasteManager.getInstance()

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        if (!action.isSupportedAction()) return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        when (action) {
            is CopyAction -> {
                val copiedText = copyPasteManager.contents?.asString() ?: return
                handleAction(
                    project,
                    EditorCopyAction(
                        text = copiedText,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is CutAction -> {
                val text = copyPasteManager.contents?.asString() ?: return
                handleAction(
                    project,
                    EditorCutAction(
                        text = text,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is PasteAction -> {
                val pastedText = copyPasteManager.contents?.asString() ?: return
                val caretOffset = editor.getCaretOffset()
                handleAction(
                    project,
                    EditorPasteAction(
                        text = pastedText,
                        caretOffset = caretOffset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is BackspaceAction -> {
                handleAction(
                    project,
                    EditorBackspaceAction(
                        textFragment = editor.getSelection(),
                        caretOffset = editor.getCaretOffset(),
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is IncrementalFindAction -> {
                handleAction(
                    project,
                    EditorFindAction(
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is CodeCompletionAction -> {
                handleAction(
                    project,
                    EditorCodeCompletionAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is ChooseItemAction.FocusedOnly -> {
                handleAction(
                    project,
                    CompletionChooseItemAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is EscapeAction -> {
                handleAction(
                    project,
                    EditorEscapeAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        if (!action.isSupportedAction()) return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        when (action) {
            is CopyAction -> {
                val selectedText = editor.getSelectedText() ?: return
                handleAction(
                    project,
                    BeforeEditorCopyAction(
                        text = selectedText,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is CutAction -> {
                handleAction(
                    project,
                    BeforeEditorCutAction(
                        textFragment = editor.getSelection(),
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is PasteAction -> {
                val pastedText = copyPasteManager.contents?.asString() ?: return
                val caretOffset = editor.getCaretOffset()
                handleAction(
                    project,
                    BeforeEditorPasteAction(
                        text = pastedText,
                        caretOffset = caretOffset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is BackspaceAction -> {
                handleAction(
                    project,
                    BeforeEditorBackspaceAction(
                        textFragment = editor.getSelection(),
                        caretOffset = editor.getCaretOffset(),
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is IncrementalFindAction -> {
                handleAction(
                    project,
                    BeforeEditorFindAction(
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is CodeCompletionAction -> {
                handleAction(
                    project,
                    BeforeEditorCodeCompletionAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is ChooseItemAction.FocusedOnly -> {
                handleAction(
                    project,
                    BeforeCompletionChooseItemAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
            is EscapeAction -> {
                handleAction(
                    project,
                    BeforeEditorEscapeAction(
                        caretOffset = editor.caretModel.offset,
                        editorRef = WeakReference(editor),
                        timeMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun Editor.getSelectedText(): String? {
        return selectionModel.selectedText
    }

    private fun Editor.getCaretOffset(): Int {
        return caretModel.offset
    }

    private fun AnAction.isSupportedAction(): Boolean {
        return this is CopyAction || this is CutAction ||
            this is PasteAction || this is BackspaceAction ||
            this is IncrementalFindAction || this is CodeCompletionAction ||
            this is ChooseItemAction.FocusedOnly || this is EscapeAction
    }
}
