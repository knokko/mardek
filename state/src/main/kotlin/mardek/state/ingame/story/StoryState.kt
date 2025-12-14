package mardek.state.ingame.story

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.Content
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.story.*

/**
 * The story-related part of the [mardek.state.ingame.CampaignState], which determines among others which
 * playable characters are available, and influences a lot of dialogue.
 */
@BitStruct(backwardCompatible = true)
class StoryState : BitPostInit {

	@BitField(id = 0)
	@NestedFieldSetting(path = "k", fieldName = "TIMELINES_KEY_PROPERTIES")
	@NestedFieldSetting(path = "v", fieldName = "TIMELINES_VALUE_PROPERTIES")
	private val timelines = HashMap<Timeline, TimelineNode>()

	override fun postInit(context: BitPostInit.Context) {
		val content = context.withParameters["content"] as Content
		initialize(content)
	}

	/**
	 * This method is meant for unit tests & BitPostInit only: it initializes the `timelines` node mapping.
	 */
	fun initialize(content: Content) {
		for (timeline in content.story.timelines) {
			if (!timelines.containsKey(timeline)) timelines[timeline] = timeline.root
			timelines[timeline] = timelines[timeline]!!.nonAbstractDescendant()
		}
	}

	/**
	 * This method must be called whenever a save file is loaded, as well as whenever the story state transitions to a
	 * new timeline node.
	 *
	 * This method checks whether the timeline state forces the player to put specific playable characters in the
	 * party, and if so, enforces this. Furthermore, this method will initialize the state of any playable character
	 * that is *available*, but does not have a state yet.
	 */
	fun validatePartyMembers(
		content: Content, party: Array<PlayableCharacter?>,
		characterStates: MutableMap<PlayableCharacter, CharacterState>
	) {
		val nodes = relevantNodes()

		for ((partyIndex, variable) in content.story.fixedVariables.forcedPartyMembers.withIndex()) {
			val forcedMember = evaluate(variable, nodes)
			if (forcedMember != null) {
				party[partyIndex] = forcedMember
				if (!characterStates.containsKey(forcedMember)) {
					characterStates[forcedMember] = evaluate(forcedMember.stateVariable, nodes) ?:
							throw IllegalStateException("Missing default state for forced $forcedMember")
				}

				for ((otherPartyIndex, otherVariable) in content.story.fixedVariables.forcedPartyMembers.withIndex()) {
					if (partyIndex == otherPartyIndex) continue
					if (party[otherPartyIndex] == forcedMember) party[otherPartyIndex] = null
					val otherForcedMember = evaluate(otherVariable, nodes)
					if (forcedMember === otherForcedMember) throw IllegalStateException(
						"$forcedMember is forced at both index $partyIndex and $otherPartyIndex"
					)
				}
				continue
			}

			val currentMember = party[partyIndex] ?: continue
			if (evaluate(currentMember.isAvailable, nodes) == null) {
				party[partyIndex] = null
			}
		}

		for (playableCharacter in content.playableCharacters.filter { !characterStates.containsKey(it) }) {

			if (evaluate(playableCharacter.isAvailable, nodes) != null ||
				evaluate(playableCharacter.isInventoryAvailable) != null
			) {
				characterStates[playableCharacter] = evaluate(playableCharacter.stateVariable, nodes) ?:
						throw IllegalStateException("Missing default state for $playableCharacter")
			}
		}
	}

	private fun relevantNodes(): RelevantNodes {
		val activeNodes = activeRelevantNodes()
		val activeNodeSet = activeNodes.toSet()

		val pastAssignments = mutableListOf<TimelineAssignment<*>>()
		val earlierActiveNodeSiblings = mutableListOf<TimelineNode>()
		val earlierInactiveNodeSiblings = mutableListOf<TimelineNode>()

		for (node in activeNodes) {
			val parentNode = node.parent ?: continue
			for (siblingIndex in 0 until node.parentIndex) {
				earlierActiveNodeSiblings.add(parentNode.children[siblingIndex])
			}
		}

		for (node in timelines.values) {
			if (node in activeNodeSet) continue
			val parentNode = node.parent ?: continue
			for (siblingIndex in 0 until node.parentIndex) {
				earlierInactiveNodeSiblings.add(parentNode.children[siblingIndex])
			}
		}

		while (earlierActiveNodeSiblings.isNotEmpty()) {
			val node = earlierActiveNodeSiblings.removeLast()
			pastAssignments.addAll(node.variables.filter { it.appliesToFutureNodes })
			earlierActiveNodeSiblings.addAll(node.children)
		}

		while (earlierInactiveNodeSiblings.isNotEmpty()) {
			val node = earlierInactiveNodeSiblings.removeLast()
			if (node.ignoresTimelineActivation) {
				pastAssignments.addAll(node.variables.filter { it.appliesToFutureNodes })
			}
			earlierInactiveNodeSiblings.addAll(node.children)
		}

		return RelevantNodes(activeNodes, pastAssignments)
	}

	private fun activeRelevantNodes(): Collection<TimelineNode> {

		val activeTimelines = timelines.keys.filter { !it.needsActivation }.toMutableSet()
		val remainingTimelines = ArrayList(activeTimelines)
		fun maybeActivateTimeline(timeline: Timeline) {
			if (activeTimelines.add(timeline)) remainingTimelines.add(timeline)
		}

		val activeNodes = mutableListOf<TimelineNode>()
		for (currentNode in timelines.values) {
			var node = currentNode
			while (true) {
				if (node.ignoresTimelineActivation) {
					activeNodes.add(node)
					for (activateTimeline in node.activatesTimelines) maybeActivateTimeline(activateTimeline)
				}
				node = node.parent ?: break
			}
		}

		while (remainingTimelines.isNotEmpty()) {
			val timeline = remainingTimelines.removeLast()
			var node: TimelineNode? = timelines[timeline] ?: throw IllegalStateException("Missing state for $timeline")
			while (node != null) {
				if (!node.ignoresTimelineActivation) {
					activeNodes.add(node)
					for (activateTimeline in node.activatesTimelines) maybeActivateTimeline(activateTimeline)
				}

				node = node.parent
			}
		}

		return activeNodes
	}

	private fun <T> evaluate(variable: TimelineVariable<T>, nodes: RelevantNodes): T? {
		var mostImportantValue: T? = null
		var mostImportantPriority = Integer.MIN_VALUE
		var mostImportantConflict = false

		for (assignment in nodes.active.flatMap { it.variables.toList() } + nodes.past) {
			if (assignment.variable !== variable) continue
			if (assignment.priority < mostImportantPriority) continue

			@Suppress("UNCHECKED_CAST")
			mostImportantValue = (assignment.value as TimelineValue<T>).get()
			mostImportantConflict = assignment.priority == mostImportantPriority
			mostImportantPriority = assignment.priority
		}

		if (mostImportantConflict) throw IllegalStateException(
			"Conflicting values for $variable with priority $mostImportantPriority"
		)
		return mostImportantValue
	}

	private fun <T> evaluate(expression: TimelineExpression<T>, nodes: RelevantNodes): T {
		// TODO CHAP3 Make iterative rather than recursive
		if (expression is ConstantTimelineExpression) return expression.fixedValue.get()
		if (expression is GlobalTimelineExpression) return evaluate(expression.global.expression, nodes)
		if (expression is VariableTimelineExpression<*>) {
			val value = evaluate(expression.variable, nodes)
			@Suppress("UNCHECKED_CAST")
			return value as T
		}
		if (expression is ExpressionOrDefaultTimelineExpression) {
			val potentialResult = evaluate(expression.expression, nodes)
			return potentialResult ?: evaluate(expression.ifNull, nodes)
		}
		if (expression is SwitchCaseTimelineExpression<*, T>) {
			val input = evaluate(expression.input, nodes)
			for (switchCase in expression.cases) {
				if (input == evaluate(switchCase.inputToMatch, nodes)) {
					return evaluate(switchCase.outputWhenInputMatches, nodes)
				}
			}
			return evaluate(expression.defaultOutput, nodes)
		}
		if (expression is NegateTimelineCondition) {
			@Suppress("UNCHECKED_CAST")
			return !evaluate(expression.operand, nodes) as T
		}
		if (expression is AndTimelineCondition) {
			@Suppress("UNCHECKED_CAST")
			return expression.operands.all { evaluate(it, nodes) } as T
		}
		if (expression is DefinedVariableTimelineCondition) {
			@Suppress("UNCHECKED_CAST")
			return (evaluate(expression.variable) != null) as T
		}
		throw UnsupportedOperationException(
			"Unsupported expression type ${expression.javaClass} : $expression"
		)
	}

	/**
	 * *Evaluates* the given `variable` (based on the current story state), and returns its current value.
	 *
	 * Calling `evaluate(variable)` multiple times for the same variable will yield the same result, as long as the
	 * story state stays in the same timeline nodes.
	 */
	fun <T> evaluate(variable: TimelineVariable<T>) = evaluate(variable, relevantNodes())

	/**
	 * *Evaluates* the given `expression` (based on the current story state), and returns its resulting value.
	 *
	 * Calling `evaluate(expression)` multiple times for the same expression will yield the same result, as long as the
	 * story state stays in the same timeline nodes.
	 */
	fun <T> evaluate(expression: TimelineExpression<T>): T {
		return evaluate(expression, relevantNodes())
	}

	/**
	 * Gets the snapshot/status of all quests in `content`.
	 *
	 * Calling `getQuests` multiple times will yield the same result, as long as the story state stays in the same
	 * timeline nodes.
	 */
	fun getQuests(content: StoryContent): QuestsSnapshot {
		val nodes = relevantNodes()
		val activeQuests = mutableListOf<Quest>()
		val completedQuests = mutableListOf<Quest>()

		for (quest in content.quests) {
			if (evaluate(quest.wasCompleted, nodes) != null) {
				completedQuests.add(quest)
			} else if (evaluate(quest.isActive, nodes) != null) {
				activeQuests.add(quest)
			}
		}

		return QuestsSnapshot(activeQuests.toTypedArray(), completedQuests.toTypedArray())
	}

	/**
	 * Transitions the state of `timeline` to `newNode`, but only if `newNode` is *later* than the current node of
	 * `timeline`. Otherwise, nothing happens.
	 */
	internal fun transition(
		content: Content, party: Array<PlayableCharacter?>,
		characterStates: MutableMap<PlayableCharacter, CharacterState>,
		timeline: Timeline, newNode: TimelineNode
	) {
		val currentNode = timelines[timeline] ?: throw IllegalStateException("Missing state for timeline $timeline")
		if (timeline.isAfter(newNode, currentNode)) {
			timelines[timeline] = newNode.nonAbstractDescendant()
			validatePartyMembers(content, party, characterStates)
		}
	}

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "timelines")
		private const val TIMELINES_KEY_PROPERTIES = false

		@Suppress("unused")
		@ReferenceField(stable = true, label = "timeline nodes")
		private const val TIMELINES_VALUE_PROPERTIES = false
	}

	private class RelevantNodes(
		val active: Collection<TimelineNode>,
		val past: Collection<TimelineAssignment<*>>
	)
}
