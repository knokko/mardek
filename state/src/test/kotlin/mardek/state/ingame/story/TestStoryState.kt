package mardek.state.ingame.story

import mardek.content.Content
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.story.FixedTimelineVariable
import mardek.content.story.Timeline
import mardek.content.story.TimelineAssignment
import mardek.content.story.TimelineCharacterStateValue
import mardek.content.story.TimelineIntValue
import mardek.content.story.TimelineNode
import mardek.content.story.TimelineOptionalPlayerValue
import mardek.content.story.TimelineUnitValue
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private fun simpleNode(
	name: String,
	children: Array<TimelineNode> = emptyArray(),
	variables: Array<TimelineAssignment<*>> = emptyArray(),
	activatesTimelines: Array<Timeline> = emptyArray(),
	isAbstract: Boolean = false,
	ignoresTimelineActivation: Boolean = false,
) = TimelineNode(
	id = UUID.randomUUID(),
	name = name,
	children = children,
	variables = variables,
	activatesTimelines = activatesTimelines,
	isAbstract = isAbstract,
	ignoresTimelineActivation = ignoresTimelineActivation,
)

class TestStoryState {

	private val simpleIntVariable = FixedTimelineVariable<Int>()

	private val content = Content()
	private val mardek = PlayableCharacter()
	private val deugan = PlayableCharacter()
	private val emela = PlayableCharacter()

	private val needsNodeA = simpleNode(
		name = "NeedsNodeA",
		variables = arrayOf(
			TimelineAssignment(simpleIntVariable, TimelineIntValue(35), priority = 10)
		)
	)
	private val needsNodeBA = simpleNode(
		name = "NeedsNodeBA",
		variables = arrayOf(
			TimelineAssignment(
				simpleIntVariable,
				TimelineIntValue(3),
				appliesToFutureNodes = true,
				priority = 4,
			)
		),
		ignoresTimelineActivation = true,
	)
	private val needsNodeBB = simpleNode(
		name = "NeedsNodeBB",
		variables = arrayOf(
			TimelineAssignment(
				simpleIntVariable,
				TimelineIntValue(-3),
				appliesToFutureNodes = true,
				priority = 5,
			)
		)
	)
	private val needsNodeBC = simpleNode(name = "NeedsNodeBC")
	private val needsNodeB = simpleNode(
		name = "NeedsNodeB",
		children = arrayOf(needsNodeBA, needsNodeBB, needsNodeBC),
		isAbstract = true,
	)
	private val needsRoot = simpleNode(
		name = "NeedsRoot",
		children = arrayOf(needsNodeA, needsNodeB),
		isAbstract = true,
	)
	private val needsTimeline = Timeline(
		id = UUID.randomUUID(),
		name = "NeedsActivation",
		root = needsRoot,
		needsActivation = true,
	)

	private val mainNodeA = simpleNode(name = "MainNodeA",)
	private val mainNodeBA = simpleNode(name = "MainNodeBA",)
	private val mainNodeB = simpleNode(
		name = "MainNodeB",
		children = arrayOf(mainNodeBA),
		variables = arrayOf(
			TimelineAssignment(simpleIntVariable, TimelineIntValue(5), priority = 1)
		),
	)
	private val mainNodeCA = simpleNode(
		name = "MainNodeCA",
		variables = arrayOf(
			TimelineAssignment(
				simpleIntVariable,
				TimelineIntValue(8),
				appliesToFutureNodes = true,
				priority = 2
			)
		)
	)
	private val mainNodeC = simpleNode(
		name = "MainNodeC",
		children = arrayOf(mainNodeCA),
	)
	private val mainNodeDAA = simpleNode(
		name = "MainNodeDAA",
		variables = arrayOf(
			TimelineAssignment(
				content.story.fixedVariables.forcedPartyMembers[2],
				TimelineOptionalPlayerValue(deugan),
			),
		)
	)
	private val mainNodeDA = simpleNode(
		name = "MainNodeDA",
		children = arrayOf(mainNodeDAA),
		variables = arrayOf(
			TimelineAssignment(
				content.story.fixedVariables.forcedPartyMembers[1],
				TimelineOptionalPlayerValue(deugan),
			),
		)
	)
	private val mainNodeD = simpleNode(
		name = "MainNodeD",
		children = arrayOf(mainNodeDA),
		variables = arrayOf(
			TimelineAssignment(deugan.isAvailable, TimelineUnitValue()),
			TimelineAssignment(deugan.stateVariable, TimelineCharacterStateValue(CharacterState())),
		)
	)
	private val mainNodeEA = simpleNode(
		name = "MainNodeEA",
		activatesTimelines = arrayOf(needsTimeline),
	)
	private val mainNodeE = simpleNode(
		name = "MainNodeE",
		children = arrayOf(mainNodeEA),
		variables = arrayOf(
			TimelineAssignment(simpleIntVariable, TimelineIntValue(1), priority = 1)
		),
	)
	private val mainNodeF = simpleNode(
		name = "MainNodeF",
		variables = arrayOf(
			TimelineAssignment(simpleIntVariable, TimelineIntValue(2), priority = 2)
		),
	)
	private val mainRoot = simpleNode(
		name = "MainRoot",
		children = arrayOf(mainNodeA, mainNodeB, mainNodeC, mainNodeD, mainNodeE, mainNodeF),
		variables = arrayOf(
			TimelineAssignment(simpleIntVariable, TimelineIntValue(12)),
			TimelineAssignment(
				content.story.fixedVariables.forcedPartyMembers[0],
				TimelineOptionalPlayerValue(mardek),
			),
			TimelineAssignment(mardek.stateVariable, TimelineCharacterStateValue(CharacterState())),
		),
	)
	private val mainTimeline = Timeline(
		id = UUID.randomUUID(),
		name = "MainTimeline",
		root = mainRoot,
		needsActivation = false,
	)

	private val state = CampaignState()
	private val updateContext = CampaignState.UpdateContext(
		GameStateUpdateContext(content, InputManager(), SoundQueue(), 1.seconds),
		"",
	)

	init {
		content.story.timelines.add(mainTimeline)
		content.story.timelines.add(needsTimeline)
		state.story.initialize(content)
	}

	@Test
	fun testIgnoreTransitionToThePast() {
		// Going from root -> A is possible
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeA)
		assertEquals(12, state.story.evaluate(simpleIntVariable))

		// Going from A -> B is possible
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeB)
		assertEquals(5, state.story.evaluate(simpleIntVariable))

		// Going from B -> A is not possible, so nothing should happen, and the variable should stay 5
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeA)
		assertEquals(5, state.story.evaluate(simpleIntVariable))
	}

	@Test
	fun testValidatePartyMembers() {
		state.party[0] = null
		state.party[1] = mardek
		state.party[2] = deugan
		state.party[3] = emela
		state.characterStates[deugan] = CharacterState()
		state.characterStates[deugan]!!.currentHealth = 12345

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeD)
		assertArrayEquals(arrayOf(mardek, null, deugan, null), state.party)
		assertEquals(CharacterState().currentHealth, state.characterStates[mardek]!!.currentHealth)
		assertEquals(12345, state.characterStates[deugan]!!.currentHealth)

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeDA)
		assertArrayEquals(arrayOf(mardek, deugan, null, null), state.party)
		assertEquals(CharacterState().currentHealth, state.characterStates[mardek]!!.currentHealth)
		assertEquals(12345, state.characterStates[deugan]!!.currentHealth)

		assertThrows<IllegalStateException> {
			state.performTimelineTransition(updateContext, mainTimeline, mainNodeDAA)
		}

		state.characterStates[mardek]!!.currentHealth = 2
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeE)
		assertArrayEquals(arrayOf(mardek, null, null, null), state.party)
		assertEquals(2, state.characterStates[mardek]!!.currentHealth)
	}

	@Test
	fun testAppliesToFutureNodesSimple() {
		assertEquals(12, state.story.evaluate(simpleIntVariable))
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeA)
		assertEquals(12, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeB)
		assertEquals(5, state.story.evaluate(simpleIntVariable))
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeBA)
		assertEquals(5, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeC)
		assertEquals(12, state.story.evaluate(simpleIntVariable))
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeCA)
		assertEquals(8, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeD)
		assertEquals(8, state.story.evaluate(simpleIntVariable))
		state.performTimelineTransition(updateContext, mainTimeline, mainNodeE)
		assertEquals(8, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeF)
		assertThrows<IllegalStateException> { state.story.evaluate(simpleIntVariable) }
	}

	@Test
	fun testAppliesToFutureNodesWithActivatedTimelinesAndIgnoresTimelineActivation() {
		assertEquals(12, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeEA)
		assertEquals(35, state.story.evaluate(simpleIntVariable))

		// Note that needsNodeB is abstract, so we go straight to needsNodeBA
		state.performTimelineTransition(updateContext, needsTimeline, needsNodeB)
		assertEquals(3, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, needsTimeline, needsNodeBB)
		assertEquals(-3, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, needsTimeline, needsNodeBC)
		assertEquals(-3, state.story.evaluate(simpleIntVariable))

		state.performTimelineTransition(updateContext, mainTimeline, mainNodeF)
		assertEquals(3, state.story.evaluate(simpleIntVariable))
	}

	@Test
	fun testDeepNestedTimelineActivationChain() {
		val inactiveTimelines = (0 until 10_000).map {
			Timeline(
				id = UUID.randomUUID(),
				name = "inactive$it",
				root = simpleNode("inactive root $it"),
				needsActivation = true,
			)
		}

		val activatedTimelines = mutableListOf<Timeline>()
		activatedTimelines.add(Timeline(
			id = UUID.randomUUID(),
			name = "activated-1",
			root = simpleNode(
				"activated root -1",
				variables = arrayOf(
					TimelineAssignment(simpleIntVariable, TimelineIntValue(17), priority = 10)
				)
			),
			needsActivation = true,
		))
		repeat(10_000) {
			activatedTimelines.add(Timeline(
				id = UUID.randomUUID(),
				name = "activated$it",
				root = simpleNode(
					"activated root $it",
					activatesTimelines = arrayOf(activatedTimelines.last())
				),
				needsActivation = it < 9999,
			))
		}
		assertEquals(10_000, activatedTimelines.count { it.needsActivation })

		for (timeline in (inactiveTimelines + activatedTimelines).shuffled(Random(123))) {
			content.story.timelines.add(timeline)
		}
		state.story.initialize(content)

		assertEquals(17, state.story.evaluate(simpleIntVariable))
	}
}
