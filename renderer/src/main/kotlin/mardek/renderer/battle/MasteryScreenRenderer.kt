package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.renderer.RenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderBoxButton
import mardek.state.ingame.UsedPartyMember
import mardek.state.ingame.area.loot.BattleLoot
import mardek.state.util.Rectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal fun renderMasteryScreen(
	context: RenderContext, loot: BattleLoot,
	party: List<UsedPartyMember>, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val colorBatch = context.addColorBatch(100)
	val ovalBatch = context.addOvalBatch(20)
	val kimBatch = context.addKim3Batch(10)
	val imageBatch = context.addImageBatch(40)
	val simpleTextBatch = context.addTextBatch(500)
	val fancyTextBatch = context.addFancyTextBatch(10)

	val scale = max(1, (region.height / 170f).roundToInt())
	colorBatch.fill(
		region.minX, region.minY, region.maxX, region.minY + 11 * scale,
		srgbToLinear(rgb(22, 13, 9)),
	)
	simpleTextBatch.drawString(
		"Skills Mastered", region.minX + 8f * scale, region.minY + 9f * scale,
		7f * scale, context.bundle.getFont(context.content.fonts.large2.index),
		srgbToLinear(rgb(128, 80, 36)), TextAlignment.LEFT,
	)
	colorBatch.fill(
		region.minX, region.minY + 11 * scale, region.maxX, region.minY + 12 * scale - 1,
		srgbToLinear(rgb(66, 50, 34)),
	)

	val font = context.bundle.getFont(context.content.fonts.basic2.index)
	val textColor = srgbToLinear(rgb(235, 200, 130))
	val shadowColor = srgbToLinear(rgb(85, 50, 20))
	val relativeTime = System.nanoTime() - referenceTime
	for (member in party) {
		val minX = region.minX + 5 * scale + 55 * scale * member.index
		var spriteIndex = 0
		val animationPeriod = 1_000_000_000L
		if ((relativeTime % animationPeriod) >= animationPeriod / 2) spriteIndex += 1
		kimBatch.simple(
			minX + scale, region.minY + 15 * scale, scale,
			member.character.areaSprites.sprites[spriteIndex].index,
		)

		val elementColor = srgbToLinear(member.character.element.color)
		val leftElementColor = changeAlpha(elementColor, 50)
		val rightElementColor = changeAlpha(elementColor, 5)
		colorBatch.gradientUnaligned(
			minX + 18 * scale, region.minY + 23 * scale, leftElementColor,
			minX + 50 * scale, region.minY + 23 * scale, rightElementColor,
			minX + 45 * scale, region.minY + 19 * scale, rightElementColor,
			minX + 18 * scale, region.minY + 19 * scale, leftElementColor,
		)
		simpleTextBatch.drawShadowedString(
			member.character.name, minX + 19f * scale, region.minY + 22f * scale,
			4f * scale, font, textColor, 0, 0f, shadowColor,
			0.5f * scale, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			"Level ${member.state.currentLevel}", minX + 19f * scale, region.minY + 30f * scale,
			4f * scale, font, textColor, 0, 0f, shadowColor,
			0.5f * scale, TextAlignment.LEFT,
		)

		val bottomColor = srgbToLinear(rgb(89, 72, 42))
		val topColor = srgbToLinear(rgb(41, 31, 17))
		gradientWithBorder(
			colorBatch, minX, region.minY + 35 * scale,
			minX + 50 * scale, region.maxY - 10 * scale,
			1, 1,
			srgbToLinear(rgb(206, 191, 146)),
			bottomColor, bottomColor, topColor,
		)

		val masteredSkills = loot.masteredSkills[member.character] ?: hashSetOf()
		for ((skillIndex, skill) in masteredSkills.withIndex()) {
			val baseY = region.minY + 38 * scale + 10 * scale * skillIndex
			val backgroundColor = srgbToLinear(rgb(41, 31, 18))
			colorBatch.fill(
				minX + 4 * scale, baseY + 2 * scale,
				minX + 46 * scale, baseY + 8 * scale - 1,
				backgroundColor,
			)
			ovalBatch.antiAliased(
				minX, baseY, minX + 4 * scale - 1, baseY + 10 * scale,
				minX + 4f * scale, baseY + 5f * scale,
				2.75f * scale, 2.75f * scale, 0.1f, backgroundColor,
			)
			ovalBatch.antiAliased(
				minX + 46 * scale + 1, baseY, minX + 55 * scale, baseY + 10 * scale,
				minX + 46f * scale + 1, baseY + 5f * scale,
				2.75f * scale, 2.75f * scale, 0.1f, backgroundColor,
			)

			val icon = when (skill) {
				is ActiveSkill -> context.content.ui.activeStarIcon
				is PassiveSkill -> context.content.ui.passiveIcon
				is ReactionSkill -> {
					when (skill.type) {
						ReactionSkillType.MeleeAttack -> context.content.ui.meleeAttackIcon
						ReactionSkillType.MeleeDefense -> context.content.ui.meleeDefenseIcon
						ReactionSkillType.RangedAttack -> context.content.ui.rangedAttackIcon
						ReactionSkillType.RangedDefense -> context.content.ui.rangedDefenseIcon
					}
				}
				else -> throw RuntimeException("Unexpected skill $skill")
			}
			imageBatch.simpleScale(
				minX + 2f * scale, baseY + 1f * scale,
				8f * scale / icon.height, icon.index,
			)
			simpleTextBatch.drawShadowedString(
				skill.name, minX + 12f * scale, baseY + 6.5f * scale,
				3.5f * scale, font, textColor, 0, 0f, shadowColor,
				0.6f * scale, TextAlignment.LEFT,
			)
		}

		val minBoxSize = 15f * scale
		val maxBoxSize = 1.03f * minBoxSize
		val boxSizePeriod = 1_000_000_000L
		val relativeTime = ((System.nanoTime() - referenceTime) % boxSizePeriod).toFloat() / boxSizePeriod
		val floatBoxSize = minBoxSize + (2f * abs(0.5f - relativeTime)) * (maxBoxSize - minBoxSize)
		val boxSize = floatBoxSize.roundToInt()
		val boxOffset = (5f * scale + minBoxSize + 0.5f * (boxSize - minBoxSize)).roundToInt()
		val boxX = region.maxX - boxOffset
		val boxY = region.maxY - boxOffset
		renderBoxButton(
			colorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, context.bundle, context.content.fonts,
			minBoxSize, boxX, boxY,
		)
	}

	return Pair(colorBatch, simpleTextBatch)
}
