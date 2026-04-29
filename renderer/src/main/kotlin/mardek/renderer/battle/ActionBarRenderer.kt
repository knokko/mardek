package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.KimSprite
import mardek.renderer.MardekTextStyles
import mardek.renderer.area.AreaSpriteBatch
import mardek.renderer.menu.determinePointerOffset
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionFlee
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleMoveSelectionWait
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import kotlin.math.max

internal fun renderActionBar(
	renderMode: ActionBarRenderMode, battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch?,
	ovalBatch: Vk2dOvalBatch, lateOvalBatch: Vk2dOvalBatch,
	spriteBatch: AreaSpriteBatch, imageBatch: Vk2dImageBatch?, textBatch: Vk2dSimpleTextBatch?, region: Rectangle,
) {
	battleContext.run {
		val stateMachine = battle.state

		val marginY = region.height / 15
		val marginX = 3 * marginY
		val iconPositions = if (battle.battle.canFlee) IntArray(5) else IntArray(4)
		val disabledPositions = mutableSetOf<Int>()
		val highDashX = region.minX + 2 * region.width / 3
		val lowDashX = highDashX - region.height

		if (stateMachine !is BattleStateMachine.SelectMove) return
		val selectedMove = stateMachine.selectedMove

		if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) return
		if (selectedMove is BattleMoveSelectionSkill && selectedMove.target != null) return
		if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) return

		val selectedIndex = when (selectedMove) {
			is BattleMoveSelectionAttack -> 0
			is BattleMoveSelectionSkill -> 1
			is BattleMoveSelectionItem -> 2
			BattleMoveSelectionWait -> 3
			BattleMoveSelectionFlee -> 4
		}
		val isPassive = when (selectedMove) {
			is BattleMoveSelectionSkill -> selectedMove.skill == null
			is BattleMoveSelectionItem -> selectedMove.item == null
			else -> true
		}

		val iconY = region.minY + marginY
		val iconSize = region.height - 2 * marginY

		val player = stateMachine.onTurn
		if (renderMode == ActionBarRenderMode.Background) {
			imageBatch!!.simpleScale(
				region.maxX - region.height - marginY.toFloat(), iconY.toFloat(),
				iconSize.toFloat() / player.element.thickSprite.height,
				player.element.thickSprite.index
			)
		}

		run {
			var x = lowDashX - region.height
			for (index in 0 until 5) {
				if (index == 4 && !battle.battle.canFlee) break
				if (index == selectedIndex) x -= region.width / 5
				iconPositions[index] = x
				x -= region.height + marginX
			}

			if (player.statusEffects.any { it.blocksBasicAttacks }) disabledPositions.add(iconPositions[0])
			if (player.statusEffects.any { it.blocksMeleeSkills } && player.statusEffects.any { it.blocksRangedSkills }) {
				disabledPositions.add(iconPositions[1])
			}
			if (!state.characterStates[player.player]!!.inventory.any { it != null && it.item.consumable != null }) {
				disabledPositions.add(iconPositions[2])
			}

			fun renderIcon(icon: KimSprite, x: Int, selected: Boolean) {
				if (selected && renderMode != ActionBarRenderMode.Foreground) return
				if (!selected && renderMode != ActionBarRenderMode.BlurredBackground) return
				spriteBatch.draw(
					icon, x, iconY,
					iconSize.toFloat() / icon.height,
					0, if (disabledPositions.contains(x)) 0.1f else 1f,
				)
			}
			renderIcon(player.getWeapon(updateContext)!!.sprite, iconPositions[0], selectedIndex == 0)
			renderIcon(player.player.characterClass.skillClass.icon, iconPositions[1], selectedIndex == 1)
			renderIcon(context.content.ui.consumableIcon, iconPositions[2], selectedIndex == 2)
			renderIcon(context.content.ui.waitIcon, iconPositions[3], selectedIndex == 3)
			if (battle.battle.canFlee) renderIcon(context.content.ui.fleeIcon, iconPositions[4], selectedIndex == 4)
		}

		val pointerScale = region.height.toFloat() / context.content.ui.pointer.width
		if (isPassive && renderMode == ActionBarRenderMode.Background) {
			imageBatch!!.rotated(
				iconPositions[selectedIndex] + iconSize * 0.5f,
				region.minY - 0.4f * region.height + 0.1f * region.height * determinePointerOffset(),
				270f, pointerScale, context.content.ui.pointer.index, 0, -1,
			)
		}

		val lineWidth = max(1, region.height / 30)
		val lineColor = srgbToLinear(rgb(208, 193, 142))

		if (renderMode == ActionBarRenderMode.Background) {
			colorBatch!!.fillUnaligned(
				region.minX, region.maxY, lowDashX, region.maxY,
				highDashX, region.minY, region.minX, region.minY,
				srgbToLinear(rgba(40, 30, 20, 230)),
			)

			val minX = iconPositions[selectedIndex] + region.height / 2
			val width = region.width / 3
			val gradientColor = changeAlpha(lineColor, 35)
			colorBatch.gradient(
				minX, region.minY + marginY, minX + width, region.maxY - marginY,
				gradientColor, 0, gradientColor
			)

			colorBatch.fillUnaligned(
				lowDashX, region.maxY, region.maxX, region.maxY,
				region.maxX, region.minY, highDashX, region.minY,
				srgbToLinear(rgb(82, 62, 37))
			)

			val leftElementColor = changeAlpha(player.element.color, 5)
			val rightElementColor = changeAlpha(player.element.color, 50)
			colorBatch.gradientUnaligned(
				lowDashX, region.maxY - marginY, leftElementColor,
				region.maxX, region.maxY - marginY, rightElementColor,
				region.maxX, region.minY + marginY, rightElementColor,
				highDashX + marginX - 2 * marginY, region.minY + marginY, leftElementColor,
			)
			colorBatch.fill(region.minX, region.minY, region.maxX, region.minY + lineWidth - 1, lineColor)
			colorBatch.fill(region.minX, 1 + region.maxY - lineWidth, region.maxX, region.maxY, lineColor)
			colorBatch.fillUnaligned(
				lowDashX, region.maxY, lowDashX + 3 * lineWidth - 1, region.maxY,
				highDashX + 3 * lineWidth - 1, region.minY, highDashX, region.minY, lineColor
			)
		}

		for (x in iconPositions) {
			val radius = 0.5f * region.height - marginY
			if (x == iconPositions[selectedIndex] && renderMode == ActionBarRenderMode.Foreground) {
				var circleColor = srgbToLinear(rgb(37, 58, 107))
				var brightCircleColor = srgbToLinear(rgb(6, 82, 155))
				var selectedLineColor = srgbToLinear(rgb(165, 205, 255))
				if (disabledPositions.contains(x)) {
					circleColor = srgbToLinear(rgb(33, 34, 45))
					brightCircleColor = circleColor
					selectedLineColor = srgbToLinear(rgb(66, 90, 116))
				}
				ovalBatch.complex(
					x - region.height, region.minY - region.height,
					x + 2 * region.height, region.maxY + region.height,
					x + radius, region.minY + marginY + radius, radius, radius,
					circleColor, circleColor, brightCircleColor, selectedLineColor, 0,
					0.8f, 0.95f, 1f, 1.15f,
				)
				val outerColor = changeAlpha(brightCircleColor, 0.3f + 0.3f * determinePointerOffset())
				val fadingColor = multiplyAlpha(outerColor, 0.2f)
				lateOvalBatch.complex(
					x - region.height, region.minY - region.height,
					x + 2 * region.height, region.maxY + region.height,
					x + radius, region.minY + marginY + radius, radius, radius,
					0, 0, outerColor, fadingColor, 0,
					0.95f, 1f, 1.5f, 1.5f,
				)

				val text = when (selectedIndex) {
					0 -> "Attack"
					1 -> player.player.characterClass.skillClass.name
					2 -> "Items"
					3 -> "Wait"
					4 -> "Flee"
					else -> throw Error("Unexpected selectedIndex $selectedIndex")
				}

				val font = context.bundle.getFont(context.content.fonts.large2.index)
				var style = MardekTextStyles.ActionBar.ACTION
				if (disabledPositions.contains(x)) style = style.multiplyAlpha(0.15f)
				textBatch!!.drawString(
					text, x + region.height.toFloat(), region.maxY - 0.25f * region.height,
					0.45f * region.height, font, style, TextAlignment.LEFT,
				)
			}

			if (x != iconPositions[selectedIndex] && renderMode == ActionBarRenderMode.BlurredBackground) {
				var circleColor = srgbToLinear(rgb(89, 69, 46))
				var brightCircleColor = srgbToLinear(rgb(137, 107, 67))
				var ovalLineColor = lineColor
				if (disabledPositions.contains(x)) {
					circleColor = srgbToLinear(rgb(48, 37, 27))
					brightCircleColor = circleColor
					ovalLineColor = srgbToLinear(rgb(108, 93, 66))
				}
				ovalBatch.complex(
					x - 1, region.minY, x + region.height, region.maxY,
					x + radius, region.minY + marginY + radius, radius, radius,
					circleColor, circleColor, brightCircleColor, ovalLineColor, 0,
					0.85f, 0.95f, 1f, 1.05f,
				)
			}
		}

		if (renderMode == ActionBarRenderMode.Background) {
			val font = context.bundle.getFont(context.content.fonts.fat.index)
			textBatch!!.drawShadowedString(
				player.player.name, region.maxX - region.height - 3f * marginX,
				region.maxY - region.height * 0.3f, region.height * 0.5f, font,
				MardekTextStyles.ActionBar.NAME, TextAlignment.RIGHT,
			)
		}
	}
}

enum class ActionBarRenderMode {
	Background, BlurredBackground, Foreground
}
