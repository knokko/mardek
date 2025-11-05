package mardek.game.battle

import mardek.content.stats.CombatStat
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestInfoModal {

	fun testRendering(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			campaign.characterStates[heroDeugan]!!.equipment[4] = content.items.items.find { it.flashName == "Dragon Amulet" }!!

			startSimpleBattle(campaign)
			val state = InGameState(campaign, "test")

			val input = InputManager()
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)

			val monsterSkinColor = arrayOf(Color(85, 56, 133))
			val balmungHandleColor = arrayOf(Color(255, 203, 101))

			val greenTextColor = arrayOf(Color(102, 255, 0))
			val redTextColor = arrayOf(Color(255, 169, 169))
			val blueTextColor = arrayOf(Color(85, 237, 255))

			val battle = campaign.currentArea!!.activeBattle!!
			val monster = battle.livingOpponents()[0]
			battle.livingPlayers()[1].statModifiers[CombatStat.Agility] = -10

			testRendering(
				state, 1600, 900, "battle-modal-before",
				monsterSkinColor,
				redTextColor + blueTextColor
			)

			var infoBlock = monster.renderInfo.renderedInfoBlock!!
			input.postEvent(MouseMoveEvent(
				infoBlock.minX + infoBlock.width / 2, infoBlock.minY + infoBlock.height / 2
			))
			input.postEvent(pressKeyEvent(InputKey.Click))
			input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)

			assertSame(monster, battle.openCombatantInfo)
			testRendering(
				state, 1600, 900, "battle-modal-monster",
				monsterSkinColor + redTextColor + blueTextColor + greenTextColor,
				emptyArray()
			)

			infoBlock = battle.livingPlayers()[1].renderInfo.renderedInfoBlock!!
			input.postEvent(MouseMoveEvent(
				infoBlock.minX + infoBlock.width / 2, infoBlock.minY + infoBlock.height / 2
			))
			input.postEvent(pressKeyEvent(InputKey.Click))
			input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)

			assertSame(battle.livingPlayers()[1], battle.openCombatantInfo)
			testRendering(
				state, 1600, 900, "battle-modal-deugan",
				monsterSkinColor + balmungHandleColor +
						redTextColor + greenTextColor + blueTextColor, emptyArray()
			)

			input.postEvent(pressKeyEvent(InputKey.Click))
			input.postEvent(releaseKeyEvent(InputKey.Click))
			state.update(context)
			assertNull(battle.openCombatantInfo)
		}
	}
}
