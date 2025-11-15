package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKimBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.BcSprite
import mardek.content.stats.CombatStat
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.state.util.Rectangle
import java.util.Locale
import kotlin.math.roundToInt

internal fun renderCombatantInfoPopup(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch, kimBatch: Vk2dKimBatch,
	imageBatch: Vk2dImageBatch, textBatch: Vk2dGlyphBatch, partBatch: AnimationPartBatch, region: Rectangle,
) {
	battleContext.run {
		val combatant = battle.openCombatantInfo ?: return

		val x1 = region.minX + region.width / 30
		val x2 = region.minX + region.width / 4
		val x3 = region.minX + region.width / 2
		val x4 = x3 + region.height / 9
		val y1 = region.minY
		val y2 = y1 + region.height / 9
		val maxY = region.boundY - region.height / 4
		val y3 = maxY - region.height / 9

		val shadowColor = srgbToLinear(rgb(61, 35, 18))

		// Render element icon
		run {
			val radius = region.height / 20
			val sprite = combatant.element.thickSprite
			imageBatch.simpleScale(
				x2 - radius.toFloat(), y1 + radius / 3f,
				2f * radius / sprite.width, sprite.index
			)
		}

		fun renderResistanceIcon(sprite: BcSprite, percentage: Int, minY: Int, column: Int) {
			imageBatch.coloredScale(
				x2 + region.height * 0.04f + column * region.height * 0.1f, minY.toFloat(),
				region.height / (30f * sprite.height), sprite.index, 0,
				rgba(1f, 1f, 1f, if (percentage == 0) 0.1f else 1f),
			)
		}

		for ((column, name) in arrayOf(
			"FIRE", "WATER", "AIR", "EARTH", "LIGHT", "DARK", "AETHER", "FIG", "PHYSICAL", "THAUMA"
		).withIndex()) {
			val element = context.content.stats.elements.find { it.properName == name }!!
			val percentage = (100f * combatant.getResistance(element, updateContext)).roundToInt()
			renderResistanceIcon(element.thickSprite, percentage, y3 + region.height / 55, column)
		}

		for ((column, name) in arrayOf(
			"PSN", "PAR", "NUM", "SIL", "CRS", "DRK", "CNF", "SLP", "ZOM", "BLD"
		).withIndex()) {
			val effect = context.content.stats.statusEffects.find { it.flashName == name }!!
			val percentage = combatant.getResistance(effect, updateContext)
			renderResistanceIcon(effect.icon, percentage, y3 + 2 * region.height / 31, column)
		}

		// Render thin left bar
		run {
			val upColor = srgbToLinear(rgba(40, 25, 10, 250))
			val downColor = srgbToLinear(rgba(50, 30, 15, 250))
			colorBatch.gradient(
				0, y1, x1, maxY,
				downColor, downColor, upColor
			)
		}

		// Render thick dark left bar
		colorBatch.fill(
			x1, y1, x2, maxY,
			srgbToLinear(rgb(24, 14, 10))
		)

		// Render main background
		run {
			val rightColor = srgbToLinear(rgba(99, 81, 49, 250))
			val leftColor = srgbToLinear(rgba(50, 40, 25, 230))
			colorBatch.gradient(
				x2, y1, region.width - 1, y3,
				leftColor, rightColor, leftColor
			)
		}

		// Render background of upper bar & lower bar
		run {
			val leftColor = srgbToLinear(rgba(55, 35, 15, 240))
			val rightColor = srgbToLinear(rgba(90, 60, 20, 240))
			colorBatch.gradientUnaligned(
				x3, y1, leftColor,
				x4, y2, leftColor,
				region.width, y2, rightColor,
				region.width, y1, rightColor,
			)
			colorBatch.gradient(
				x2, y3, region.width - 1, maxY,
				leftColor, rightColor, leftColor
			)
		}

		val unknownFont = context.bundle.getFont(context.content.fonts.basic2.index)

		// Render health
		val darkTextColor = srgbToLinear(rgb(149, 107, 62))
		val greenTextColor = srgbToLinear(rgb(102, 255, 0))
		run {
			val baseY = y1 + region.height / 20
			val shadowOffset = 0.002f * region.height
			textBatch.drawShadowedString(
				"HP:", x4 + 0.07f * region.height, baseY.toFloat(),
				0.025f * region.height, unknownFont, darkTextColor,
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.RIGHT,
			)
			ResourceBarRenderer(
				context, ResourceType.Health, Rectangle(
					x4 + region.height / 11, baseY - region.height / 50,
					2 * region.height / 9, region.height / 50,
				), colorBatch, textBatch,
			).renderBar(combatant.currentHealth, combatant.maxHealth)
			textBatch.drawShadowedString(
				"${combatant.currentHealth}/${combatant.maxHealth}", x4 + 0.33f * region.height,
				baseY.toFloat(), 0.0225f * region.height, unknownFont, greenTextColor,
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
		}

		// Render mana
		val blueTextColor = srgbToLinear(rgb(85, 237, 255))
		run {
			val baseY = y1 + region.height / 10
			val shadowOffset = 0.002f * region.height
			textBatch.drawShadowedString(
				"MP:", x4 + 0.07f * region.height, baseY.toFloat(),
				0.025f * region.height, unknownFont, darkTextColor, 0, 0f,
				shadowColor, shadowOffset,
				shadowOffset, TextAlignment.RIGHT,
			)
			ResourceBarRenderer(
				context, ResourceType.Mana, Rectangle(
					x4 + region.height / 11, baseY - region.height / 50,
					2 * region.height / 9, region.height / 50
				), colorBatch, textBatch
			).renderBar(combatant.currentMana, combatant.maxMana)
			textBatch.drawShadowedString(
				"${combatant.currentMana}/${combatant.maxMana}", x4 + region.height * 0.33f,
				baseY.toFloat(), region.height * 0.0225f, unknownFont, blueTextColor,
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
		}

		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val strongTextColor = srgbToLinear(rgb(255, 203, 51))
		val redTextColor = srgbToLinear(rgb(255, 169, 169))

		fun renderStatRow(stat: CombatStat, baseY: Float) {
			val heightA = region.height * 0.025f
			val shadowOffset = region.height * 0.002f
			val textX1 = x2 + region.height * 0.22f
			textBatch.drawShadowedString(
				stat.flashName.uppercase(Locale.ROOT), textX1, baseY, heightA, unknownFont,
				darkTextColor, 0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.RIGHT,
			)

			val textX2 = x2 + region.height * 0.35f
			var extra = combatant.getStat(stat, updateContext) - combatant.getNatural(stat)
			run {
				val equipment = combatant.getEquipment(updateContext)
				val weapon = equipment[0]?.equipment
				if (stat == CombatStat.Attack && weapon != null) extra -= weapon.getStat(stat)
				if (stat == CombatStat.MeleeDefense || stat == CombatStat.RangedDefense) {
					for (potentialArmor in equipment) {
						val armor = potentialArmor?.equipment
						if (armor != null) extra -= armor.getStat(stat)
					}
				}
			}

			textBatch.drawShadowedString(
				"${combatant.getStat(stat, updateContext)}${if (stat == CombatStat.Evasion) "%" else ""}",
				0.5f * (textX1 + textX2), baseY, heightA, unknownFont,
				if (extra == 0) baseTextColor else strongTextColor, 0, 0f,
				shadowColor, shadowOffset,
				shadowOffset, TextAlignment.CENTERED,
			)

			if (extra != 0) {
				textBatch.drawShadowedString(
					"${if (extra > 0) "+" else ""}$extra", textX2, baseY, heightA, unknownFont,
					if (extra > 0) greenTextColor else redTextColor, 0, 0f,
					shadowColor, shadowOffset,
					shadowOffset, TextAlignment.LEFT,
				)
			}
		}

		// Render stats
		run {
			val statY1 = y1 + 0.22f * region.height
			val statOffsetY = 0.05f * region.height
			renderStatRow(CombatStat.Strength, statY1)
			renderStatRow(CombatStat.Vitality, statY1 + statOffsetY)
			renderStatRow(CombatStat.Spirit, statY1 + 2 * statOffsetY)
			renderStatRow(CombatStat.Agility, statY1 + 3 * statOffsetY)

			val statY2 = y1 + 0.45f * region.height
			renderStatRow(CombatStat.Attack, statY2)
			renderStatRow(CombatStat.MeleeDefense, statY2 + statOffsetY)
			renderStatRow(CombatStat.RangedDefense, statY2 + 2 * statOffsetY)
			renderStatRow(CombatStat.Evasion, statY2 + 3 * statOffsetY)
		}

		// Render equipment names
		run {
			val baseX = x2 + region.height * 0.6f
			val baseY = y1 + region.height * 0.19f
			val textHeight = region.height * 0.025f
			val spriteHeight = region.height / 16f
			val offsetY = region.height * 0.0775f
			for ((index, item) in combatant.getEquipment(updateContext).withIndex()) {
				val shadowOffset = 0.1f * textHeight
				textBatch.drawShadowedString(
					item?.flashName ?: "-", baseX + 0.09f * region.height, baseY + index * offsetY,
					textHeight, unknownFont, baseTextColor, 0, 0f,
					shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
				)
				if (item != null) kimBatch.simple(
					baseX.roundToInt(), (baseY + index * offsetY - 0.04f * region.height).roundToInt(),
					spriteHeight / item.sprite.height, item.sprite.index
				)
			}
		}

		// Render name, level, and class
		run {
			val baseX = x2 + region.height * 0.06f
			val textHeight = region.height / 45f
			val baseY = y1 + region.height / 15f + textHeight * 0.5f
			val lineGap = region.height / 20f
			val shadowOffset = 0.1f * textHeight
			textBatch.drawShadowedString(
				combatant.getName(), baseX, baseY, textHeight,
				context.bundle.getFont(context.content.fonts.fat.index), baseTextColor,
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
			textBatch.drawShadowedString(
				"Level ${combatant.getLevel(updateContext)} ${combatant.getClassName()}",
				baseX, baseY + lineGap, textHeight, unknownFont, baseTextColor,
				0, 0f, shadowColor, shadowOffset,
				shadowOffset, TextAlignment.LEFT,
			)
			textBatch.drawShadowedString(
				combatant.getCreatureType().flashName, baseX, baseY + 2 * lineGap,
				textHeight, unknownFont, baseTextColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
		}

		fun renderResistance(percentage: Int, baseY: Float, column: Int) {
			if (percentage == 0) return
			val (color, value) = if (percentage < 0) Pair(redTextColor, -percentage)
			else if (percentage > 100) Pair(greenTextColor, percentage - 100)
			else Pair(blueTextColor, percentage)
			textBatch.drawString(
				value.toString(), x2 + region.height * 0.08f + 0.1f * column * region.height, baseY,
				region.height * 0.02f, unknownFont, color,
			)
		}

		fun renderElementalResistance(name: String, column: Int) {
			val element = context.content.stats.elements.find { it.properName == name }!!
			renderResistance(
				(100f * combatant.getResistance(element, updateContext)).roundToInt(),
				y3 + region.height * 0.045f, column
			)
		}

		for ((column, element) in arrayOf(
			"FIRE", "WATER", "AIR", "EARTH", "LIGHT", "DARK", "AETHER", "FIG", "PHYSICAL", "THAUMA"
		).withIndex()) renderElementalResistance(element, column)

		fun renderStatusResistance(name: String, column: Int) {
			val effect = context.content.stats.statusEffects.find { it.flashName == name }!!
			renderResistance(
				combatant.getResistance(effect, updateContext),
				y3 + region.height * 0.09f, column
			)
		}

		for ((column, effect) in arrayOf(
			"PSN", "PAR", "NUM", "SIL", "CRS", "DRK", "CNF", "SLP", "ZOM", "BLD"
		).withIndex()) renderStatusResistance(effect, column)

		textBatch.drawShadowedString(
			"RESISTANCES", region.boundX - region.height * 0.03f, y3 + 0.008f * region.height,
			0.02f * region.height, unknownFont, darkTextColor, 0, 0f,
			shadowColor, 0.002f * region.height,
			0.002f * region.height, TextAlignment.RIGHT,
		)

		CombatantRenderer(battleContext, partBatch, combatant, region, true).render()
	}
}
