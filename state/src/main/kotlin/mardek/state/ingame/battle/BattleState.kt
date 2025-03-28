package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.state.ingame.CampaignState

@BitStruct(backwardCompatible = true)
class BattleState(
	@BitField(id = 0)
	val battle: Battle,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceField(stable = true, label = "playable characters")
	val players: Array<PlayableCharacter?>,

	campaignState: CampaignState,
) {

	@BitField(id = 2)
	val enemyStates = Array(4) { index ->
		val enemy = battle.enemies[index] ?: return@Array null
		CombatantState(enemy)
	}

	@BitField(id = 3)
	val playerStates = Array(4) { index ->
		val player = players[index] ?: return@Array null
		CombatantState(player, campaignState.characterStates[player]!!)
	}

	val startTime = System.nanoTime()

	@Suppress("unused")
	private constructor() : this(Battle(), emptyArray(), CampaignState())

	fun processKeyPress(key: InputKey) {

	}
}
