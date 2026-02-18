package mardek.game.area

import mardek.game.TestingInstance
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.Duration.Companion.milliseconds

object TestWalking {

	fun testRandomNpcWalking(instance: TestingInstance) {
		instance.apply {
			val goznor = content.areas.areas.find { it.properties.rawName == "goznor" }!!

			// The NPCs should NOT move to any of the forbidden points
			val forbiddenPoints = mutableListOf(
				// Bridge to the houses of Mardek and Deugan
				AreaPosition(2, 17), AreaPosition(2, 21),

				// Bridge to the shops and the sewers
				AreaPosition(24, 16), AreaPosition(26, 16),

				// Fence to the magic shop
				AreaPosition(32, 19), AreaPosition(32, 21),

				// Fence to the inventor house and sewers
				AreaPosition(34, 7), AreaPosition(34, 5),

				// The save crystal
				AreaPosition(21, 14),
			)
			forbiddenPoints += goznor.objects.transitions.map { AreaPosition(it.x, it.y) }
			forbiddenPoints += goznor.objects.transitions.map { AreaPosition(it.x, it.y - 1) }
			forbiddenPoints += goznor.objects.doors.map { AreaPosition(it.x, 1 + it.y) }
			for (y in goznor.minTileY .. goznor.maxTileY) {
				for (x in goznor.minTileX .. goznor.maxTileX) {
					if (!goznor.canWalkOnTile(x, y)) forbiddenPoints.add(AreaPosition(x, y))
				}
			}

			repeat(50) {
				val state = InGameState(simpleCampaignState(), "test walking")
				val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 100.milliseconds)
				performTimelineTransition(
					updateContext, state.campaign, "MainTimeline",
					"Searching for the fallen 'star'"
				)
				val areaState = AreaState(
					goznor, state.campaign.story,
					AreaPosition(11, 16),
				)
				state.campaign.state = areaState

				val uniqueCharacterPositions = mutableSetOf<AreaPosition>()
				repeat(200) {
					state.update(updateContext)
					uniqueCharacterPositions.addAll(goznor.objects.characters.mapNotNull(
						areaState::getCharacterState
					).map { AreaPosition(it.x, it.y ) })
				}
				assertEquals(
					emptySet<AreaPosition>(),
					uniqueCharacterPositions.intersect(forbiddenPoints.toSet()),
				)
				assertTrue(uniqueCharacterPositions.size > 25)
			}
		}
	}
}
