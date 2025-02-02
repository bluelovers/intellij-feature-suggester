package org.jetbrains.plugins.feature.suggester.suggesters

import com.intellij.lang.Language
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.XSourcePosition.isOnTheSameLine
import org.jetbrains.plugins.feature.suggester.FeatureSuggesterBundle
import org.jetbrains.plugins.feature.suggester.NoSuggestion
import org.jetbrains.plugins.feature.suggester.Suggestion
import org.jetbrains.plugins.feature.suggester.actions.BreakpointAddedAction
import org.jetbrains.plugins.feature.suggester.actions.BreakpointRemovedAction
import org.jetbrains.plugins.feature.suggester.actions.DebugSessionPausedAction
import org.jetbrains.plugins.feature.suggester.history.UserActionsHistory

class RunToCursorSuggester : AbstractFeatureSuggester() {
    override val id: String = "Run to cursor"
    override val suggestingActionDisplayName: String = FeatureSuggesterBundle.message("run.to.cursor.name")

    override val message = FeatureSuggesterBundle.message("run.to.cursor.message")
    override val suggestingActionId = "RunToCursor"
    override val suggestingDocUrl =
        "https://www.jetbrains.com/help/idea/stepping-through-the-program.html#run-to-cursor"

    override val languages = listOf(Language.ANY.id)

    private object State {
        var debugSessionPaused: Boolean = false
        var addedBreakpointPosition: XSourcePosition? = null
        var breakpointAddedTimeMillis: Long = 0L
        var isPausedOnBreakpoint: Boolean = false

        val isBreakpointAdded: Boolean
            get() = addedBreakpointPosition != null

        fun isOutOfDate(breakpointRemovedTimeMillis: Long): Boolean {
            return breakpointRemovedTimeMillis - breakpointAddedTimeMillis > MAX_TIME_MILLIS_BETWEEN_ACTIONS
        }

        fun reset() {
            debugSessionPaused = false
            addedBreakpointPosition = null
            breakpointAddedTimeMillis = 0L
            isPausedOnBreakpoint = false
        }
    }

    override fun getSuggestion(actions: UserActionsHistory): Suggestion {
        when (val action = actions.lastOrNull()) {
            is DebugSessionPausedAction -> {
                if (State.debugSessionPaused && isOnTheSameLine(action.position, State.addedBreakpointPosition)) {
                    State.isPausedOnBreakpoint = true
                } else {
                    State.reset()
                    State.debugSessionPaused = true
                }
            }
            is BreakpointAddedAction -> {
                if (!State.isBreakpointAdded) {
                    State.apply {
                        addedBreakpointPosition = action.position
                        breakpointAddedTimeMillis = action.timeMillis
                    }
                } else {
                    State.reset()
                }
            }
            is BreakpointRemovedAction -> {
                if (State.isPausedOnBreakpoint &&
                    isOnTheSameLine(action.position, State.addedBreakpointPosition) &&
                    !State.isOutOfDate(action.timeMillis)
                ) {
                    State.reset()
                    return createSuggestion()
                }
                State.reset()
            }
        }

        return NoSuggestion
    }

    companion object {
        const val MAX_TIME_MILLIS_BETWEEN_ACTIONS: Long = 5000L
    }
}
