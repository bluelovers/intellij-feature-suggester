package org.jetbrains.plugins.feature.suggester.suggesters

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.jetbrains.plugins.feature.suggester.FeatureSuggesterBundle
import org.jetbrains.plugins.feature.suggester.NoSuggestion
import org.jetbrains.plugins.feature.suggester.Suggestion
import org.jetbrains.plugins.feature.suggester.actions.EditorFindAction
import org.jetbrains.plugins.feature.suggester.actions.EditorFocusGainedAction
import org.jetbrains.plugins.feature.suggester.history.UserActionsHistory
import org.jetbrains.plugins.feature.suggester.suggesters.lang.LanguageSupport

class FileStructureSuggester : AbstractFeatureSuggester() {
    override val id: String = "File structure"
    override val suggestingActionDisplayName: String = FeatureSuggesterBundle.message("file.structure.name")

    override val message = FeatureSuggesterBundle.message("file.structure.message")
    override val suggestingActionId = "FileStructurePopup"
    override val suggestingTipFileName = "FileStructurePopup.html"

    override val languages = listOf("JAVA", "kotlin", "Python", "ECMAScript 6")

    override fun getSuggestion(actions: UserActionsHistory): Suggestion {
        if (actions.size < 2) return NoSuggestion
        val action = actions.lastOrNull()!!
        val language = action.language ?: return NoSuggestion
        val langSupport = LanguageSupport.getForLanguage(language) ?: return NoSuggestion
        when (action) {
            is EditorFocusGainedAction -> {
                if (actions.get(1) !is EditorFindAction) return NoSuggestion // check that previous action is Find
                val psiFile = action.psiFile ?: return NoSuggestion
                val project = action.project ?: return NoSuggestion
                val editor = action.editor ?: return NoSuggestion

                val findModel = getFindModel(project)
                val textToFind = findModel.stringToFind
                val definition = langSupport.getDefinitionOnCaret(psiFile, editor.caretModel.offset)
                if (definition is PsiNamedElement && langSupport.isFileStructureElement(definition) &&
                    definition.name?.contains(textToFind, !findModel.isCaseSensitive) == true
                ) {
                    return createSuggestion()
                }
            }
            else -> NoSuggestion
        }

        return NoSuggestion
    }

    private fun LanguageSupport.getDefinitionOnCaret(psiFile: PsiFile, caretOffset: Int): PsiElement? {
        val offset = caretOffset - 1
        if (offset < 0) return null
        val curElement = psiFile.findElementAt(offset)
        return if (curElement != null && isIdentifier(curElement)) {
            curElement.parent
        } else {
            null
        }
    }

    private fun getFindModel(project: Project): FindModel {
        val findManager = FindManager.getInstance(project)
        val findModel = FindModel()
        findModel.copyFrom(findManager.findInFileModel)
        return findModel
    }
}
