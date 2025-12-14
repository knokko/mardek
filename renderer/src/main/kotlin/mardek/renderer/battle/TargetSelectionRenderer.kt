package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.stats.Element
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleSkillTargetAllAllies
import mardek.state.ingame.battle.BattleSkillTargetAllEnemies
import mardek.state.ingame.battle.BattleSkillTargetSingle
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderTargetSelection(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	imageBatch: Vk2dImageBatch, textBatch: MardekGlyphBatch, region: Rectangle,
) {
	battleContext.run {
		val stateMachine = battle.state
		val elementX = region.maxX - region.width / 3f
		val elementY = region.maxY - region.height / 10f

		if (stateMachine !is BattleStateMachine.SelectMove) return

		val selectedMove = stateMachine.selectedMove

		val target = if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) {
			selectedMove.target!!
		} else if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
			BattleSkillTargetSingle(selectedMove.target!!)
		} else return

		val weaponElement = stateMachine.onTurn.getEquipment(updateContext)[0]!!.element ?:
				context.content.stats.defaultWeaponElement
		val action = when (selectedMove) {
			is BattleMoveSelectionAttack -> {
				Action("Attack", 0, weaponElement)
			}

			is BattleMoveSelectionSkill -> {
				var manaCost = selectedMove.skill!!.manaCost
				if (target is BattleSkillTargetAllAllies || target is BattleSkillTargetAllEnemies) manaCost *= 2
				Action(selectedMove.skill!!.name, manaCost, selectedMove.skill!!.element)
			}

			else -> {
				Action(
					(selectedMove as BattleMoveSelectionItem).item!!.flashName, 0, weaponElement
				)
			}
		}

		run {
			val elementSprite = action.element.thickSprite
			val desiredHeight = region.height / 15f
			imageBatch.simpleScale(
				elementX, elementY,
				desiredHeight / elementSprite.height,
				elementSprite.index,
			)
		}

		val backgroundColor = rgba(0, 0, 0, 100)
		val minY = elementY.roundToInt() + region.height / 100
		val maxY = elementY.roundToInt() + region.height / 20 + region.height / 100

		val fatFont = context.bundle.getFont(context.content.fonts.fat.index)
		val textColor = srgbToLinear(rgb(238, 203, 127))

		val lowTargetingColor = srgbToLinear(rgb(126, 1, 1))
		val highTargetingColor = srgbToLinear(rgb(175, 61, 1))
		textBatch.drawFancyString(
			"TARGETING MODE", region.maxX - region.height * 0.1f,
			region.minY + region.height * 0.075f, 0.05f * region.height,
			fatFont, lowTargetingColor,
			srgbToLinear(rgb(180, 154, 110)),
			0.005f * region.height, TextAlignment.RIGHT,
			lowTargetingColor, highTargetingColor, highTargetingColor, highTargetingColor,
			0.5f, 0.5f, 1f, 1f,
		)

		val manaX = region.minX + region.width / 25
		val radius = 0.5f * (maxY - minY)
		ovalBatch.antiAliased(
			manaX - (1.2f * radius).roundToInt(), minY, manaX - 1, maxY,
			manaX.toFloat(), minY + radius, radius, radius,
			0.1f, backgroundColor,
		)
		val maxManaX = manaX + region.width / 4
		colorBatch.fill(manaX, minY, maxManaX, maxY, backgroundColor)
		ovalBatch.antiAliased(
			maxManaX + 1, minY,
			maxManaX + (1.2f * radius).roundToInt(), maxY,
			maxManaX.toFloat(), minY + radius, radius, radius,
			0.1f, backgroundColor,
		)

		val manaString = if (action.manaCost > 0) action.manaCost.toString() else "-"
		val manaColor = if (action.manaCost > stateMachine.onTurn.currentMana) {
			srgbToLinear(rgb(254, 81, 81))
		} else srgbToLinear(rgb(50, 203, 254))
		textBatch.drawString(
			"MP Cost:", manaX, minY + 6 * (maxY - minY) / 7,
			2 * (maxY - minY) / 3, fatFont, textColor,
		)
		textBatch.drawString(
			manaString, manaX + region.height / 4, minY + 6 * (maxY - minY) / 7,
			maxY - minY, fatFont, manaColor,
		)

		val ovalX1 = elementX.roundToInt() - region.width / 50
		val ovalX2 = elementX.roundToInt() + region.width / 4
		ovalBatch.antiAliased(
			ovalX1 - (1.2f * radius).roundToInt(), minY, ovalX1 - 1, maxY,
			ovalX1.toFloat(), minY + radius, radius, radius,
			0.1f, backgroundColor,
		)
		colorBatch.fill(ovalX1, minY, ovalX2, maxY, backgroundColor)
		ovalBatch.antiAliased(
			ovalX2 + 1, minY, ovalX2 + (1.2f * radius).roundToInt(), maxY,
			ovalX2.toFloat(), minY + radius, radius, radius,
			0.1f, backgroundColor,
		)

		textBatch.drawString(
			action.name, elementX + region.height / 15 + region.width / 50,
			minY + 6f * (maxY - minY) / 7, 2f * (maxY - minY) / 3, fatFont, textColor,
		)
	}
}

private class Action(val name: String, val manaCost: Int, val element: Element)
