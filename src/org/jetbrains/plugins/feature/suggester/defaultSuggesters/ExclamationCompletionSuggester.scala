package org.jetbrains.plugins.feature.suggester.defaultSuggesters

import org.jetbrains.plugins.feature.suggester.{NoSuggestion, Suggestion, FeatureSuggester}
import org.jetbrains.plugins.feature.suggester.changes._
import com.intellij.psi.{PsiElement, PsiType, PsiPrefixExpression, PsiExpression}
import org.jetbrains.plugins.feature.suggester.changes.ChildRemovedAction
import org.jetbrains.plugins.feature.suggester.changes.ChildReplacedAction
import org.jetbrains.plugins.feature.suggester.changes.ChildAddedAction
import scala.Some
import com.intellij.openapi.command.CommandProcessor
import com.intellij.featureStatistics.ProductivityFeaturesRegistry

/**
 * @author Alefas
 * @since 23.05.13
 */
class ExclamationCompletionSuggester extends FeatureSuggester {
  val POPUP_MESSAGE = "Why no to finish completion by '!' character"
  val DESCRIPTOR_ID = "editing.completion.finishByDotEtc"

  private case class PossibleExclaimingExpression(action: UserAction, expressionText: String, startOffset: Int)

  private var exclaimingExpression: Option[PossibleExclaimingExpression] = None
  private var lastCompletionCall: Long = 0

  def getSuggestion(actions: List[UserAction]): Suggestion = {
    val commandName = CommandProcessor.getInstance().getCurrentCommandName
    if (commandName == "Choose Lookup Item") {
      lastCompletionCall = System.currentTimeMillis()
    }
    actions.last match {
      case ChildAddedAction(_, Expression(expression)) =>
        updateStartOffset(expression.getTextRange.getStartOffset, expression.getTextRange.getEndOffset, 0)
        checkExpression(expression, actions.last)
      case ChildAddedAction(_, element) =>
        updateStartOffset(element.getTextRange.getStartOffset, element.getTextRange.getEndOffset, 0)
      case ChildReplacedAction(_, prefixExpression: PsiPrefixExpression, expression: PsiExpression) =>
        updateStartOffset(prefixExpression.getTextRange.getStartOffset, prefixExpression.getTextRange.getEndOffset, expression.getTextLength)
        if (checkExpressionExclaimed(prefixExpression, expression, actions)) {
          exclaimingExpression = None
          return SuggestingUtil.createSuggestion(DESCRIPTOR_ID, POPUP_MESSAGE)
        }
      case ChildReplacedAction(_, Expression(expression), oldElement) =>
        updateStartOffset(expression.getTextRange.getStartOffset, expression.getTextRange.getEndOffset, oldElement.getTextLength)
        checkExpression(expression, actions.last)
      case ChildReplacedAction(_, element, oldElement) =>
        updateStartOffset(element.getTextRange.getStartOffset, element.getTextRange.getEndOffset, oldElement.getTextLength)
      case ChildRemovedAction(_, _) =>
        //we can't recalculate offset in this case
        exclaimingExpression = None
      case ChildMovedAction(parent, _, oldParent) =>
        //we can't recalculate offset in this case
        exclaimingExpression = None
      case _ =>
    }
    NoSuggestion
  }

  def getId: String = "Exclamation completion suggester"

  private def checkExpression(expression: PsiExpression, action: UserAction) {
    if (expression.isInstanceOf[PsiPrefixExpression]) return
    val expressionType = expression.getType
    if (expressionType != PsiType.BOOLEAN) return
    //let's check that this change is after completion action
    val delta = System.currentTimeMillis() - lastCompletionCall
    if (delta > 50L) return
    exclaimingExpression = Some(PossibleExclaimingExpression(action, expression.getText, expression.getTextRange.getStartOffset))
  }

  private def updateStartOffset(startOffset: Int, endOffset: Int, oldLenght: Int) {
    exclaimingExpression match {
      case Some(PossibleExclaimingExpression(action, expressionText, offset)) =>
        if (startOffset < offset && endOffset > offset) {
          exclaimingExpression = None
        } else if (startOffset < offset && endOffset <= offset) {
          exclaimingExpression = Some(PossibleExclaimingExpression(action, expressionText, offset + endOffset - startOffset - oldLenght))
        }
      case _ => //nothing to do
    }
  }

  private def checkExpressionExclaimed(prefixExpression: PsiPrefixExpression, expression: PsiExpression, actions: List[UserAction]): Boolean = {
    exclaimingExpression match {
      case Some(PossibleExclaimingExpression(action, expressionText, startOffset)) =>
        if (expression.getText != expressionText) return false
        if (prefixExpression.getText != "!" + expressionText) return false
        if (prefixExpression.getTextRange.getStartOffset != startOffset) return false
        if (!actions.takeRight(10).contains(action)) {
          //looks like action is too old, let's remove it just in case
          exclaimingExpression = None
          return false
        }
        return true
      case _ =>
    }
    false
  }
}

object Expression {
  def unapply(_element: PsiElement): Option[PsiExpression] = {
    val initialTextRange = _element.getTextRange
    var element: PsiElement = _element
    while (element != null && element.getTextRange == initialTextRange) {
      element match {
        case expression: PsiExpression => return Some(expression)
        case _ => element = element.getParent
      }
    }
    None
  }
}
