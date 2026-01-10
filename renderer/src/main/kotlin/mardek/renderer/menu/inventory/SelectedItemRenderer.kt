package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.characters.CharacterState
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.stats.CombatStat
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.renderDescription
import mardek.renderer.util.renderFancyMasteredText
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.util.Rectangle
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderHoverItemProperties(
	inventoryContext: InventoryRenderContext,
	interaction: InventoryInteractionState,
	selectedCharacterState: CharacterState,
	minX: Int, startY: Int, maxX: Int, maxY: Int, scale: Int,
) {
	inventoryContext.run {
		val width = 1 + maxX - minX
		val barY = startY + scale * SIMPLE_SLOT_SIZE
		val tabsY = maxY - 15 * scale

		val barColorLight = srgbToLinear(rgb(127, 93, 50))
		val barColorDark = srgbToLinear(rgb(66, 56, 48))
		colorBatch.gradient(
			minX, startY, maxX, barY,
		barColorLight, barColorLight, barColorDark
		)

		val midColorLight = srgbToLinear(rgb(91, 74, 43))
		val midColorDark = srgbToLinear(rgb(60, 40, 28))
		colorBatch.gradient(
			minX, barY, maxX, tabsY,
			midColorLight, midColorDark, midColorLight
		)

		val basicFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val largeFont = context.bundle.getFont(context.content.fonts.large2.index)
		val strokeColor = 0
		val shadowColor = srgbToLinear(rgb(40, 20, 10))
		val strokeFactor = 0f
		val shadowFactor = 0.07f
		val lineColorLight = srgbToLinear(rgb(208, 193, 142))
		val lineColorDark = srgbToLinear(rgb(140, 94, 47))

		val hoverItem = interaction.hoveredSlot?.get()?.item
		if (hoverItem != null) {
			val equipment = hoverItem.equipment
			val element = hoverItem.element
			if (element != null) {
				val elementColor = srgbToLinear(element.color)
				val lowElementColor = changeAlpha(elementColor, 100)
				val highElementColor = changeAlpha(elementColor, 30)
				colorBatch.gradient(
					minX, startY + 5 * scale,
					maxX - 5 * scale, startY + 13 * scale,
					lowElementColor, 0, highElementColor
				)

				val horizontalElementScale = 0.7f * (1f + maxX - minX) / element.thinSprite.width
				val verticalElementScale = 0.9f * (1f + tabsY - barY) / element.thinSprite.height
				val elementScale = min(horizontalElementScale, verticalElementScale)
				val marginX = maxX - minX - elementScale * element.thinSprite.width
				val marginY = tabsY - barY - elementScale * element.thinSprite.height
				imageBatch.coloredScale(
					minX + marginX / 2f, barY + marginY / 2f,
					elementScale, element.thinSprite.index,
					0, rgba(1f, 1f, 1f, 0.075f)
				)
			}

			val titleColor = srgbToLinear(rgb(238, 203, 127))
			val titleHeight = 7f * scale
			textBatch.drawShadowedString(
				hoverItem.toString(), minX + 5f * scale, startY + 11f * scale,
				titleHeight, basicFont, titleColor, strokeColor, titleHeight * strokeFactor,
				shadowColor, shadowFactor * titleHeight, shadowFactor * titleHeight,
				TextAlignment.LEFT
			)

			val baseTextColor = srgbToLinear(rgb(207, 192, 141))
			val goodTextColor = srgbToLinear(rgb(152, 254, 0))
			val badTextColor = srgbToLinear(rgb(254, 84, 84))

			val baseStats = mutableListOf<CombatStat>()
			if (equipment != null) {
				if (equipment.weapon == null) {
					baseStats.add(CombatStat.MeleeDefense)
					baseStats.add(CombatStat.RangedDefense)
				} else baseStats.add(CombatStat.Attack)
			}

			val textMinX = 5 * scale
			var textY = barY + 13 * scale
			if (interaction.descriptionIndex == 0) {
				val descriptionHeight = 6f * scale
				textBatch.drawShadowedString(
					hoverItem.type.displayName, textMinX.toFloat(), textY.toFloat(),
					descriptionHeight, basicFont, baseTextColor,
					strokeColor, strokeFactor * descriptionHeight,
					shadowColor, shadowFactor * descriptionHeight,
					shadowFactor * descriptionHeight, TextAlignment.LEFT
				)
				textY += 20 * scale

				for (stat in baseStats) {
					val adder = hoverItem.getModifier(stat)
					if (adder != 0) {
						textBatch.drawShadowedString(
							"${stat.flashName}: $adder", textMinX.toFloat(), textY.toFloat(),
							descriptionHeight, basicFont, baseTextColor,
							strokeColor, strokeFactor * descriptionHeight,
							shadowColor, shadowFactor * descriptionHeight,
							shadowFactor * descriptionHeight, TextAlignment.LEFT
						)
						textY += 10 * scale
					}
				}
				textY += 12 * scale

				fun drawLine(currentLine: String) {
					val lineHeight = 5f * scale
					textBatch.drawShadowedString(
						currentLine, textMinX.toFloat(), textY.toFloat(), lineHeight,
						basicFont, baseTextColor, strokeColor, strokeFactor * lineHeight,
						shadowColor, shadowFactor * lineHeight,
						shadowFactor * lineHeight, TextAlignment.LEFT,
					)
					textY += 8 * scale
				}

				renderDescription(hoverItem.description, 35, ::drawLine)
			}

			if (interaction.descriptionIndex == 1 && equipment != null) {
				for ((row, skill) in equipment.skills.withIndex()) {
					val skillY = barY + 2 * scale + 28 * row * scale

					val nameColor = srgbToLinear(rgb(238, 203, 127))
					val nameHeight = 7f * scale
					textBatch.drawString(
						skill.name, minX + 20f * scale, skillY + 10f * scale,
						nameHeight, basicFont, nameColor,
					)

					val skillMastery = selectedCharacterState.skillMastery[skill] ?: 0
					if (skillMastery < skill.masteryPoints) {
						val masteryRenderer = ResourceBarRenderer(
							context, ResourceType.SkillMastery, Rectangle(
								23 * scale, skillY + 17 * scale, 57 * scale, 6 * scale
							), colorBatch, textBatch
						)
						masteryRenderer.renderBar(skillMastery, skill.masteryPoints)
						masteryRenderer.renderTextOverBar(skillMastery, skill.masteryPoints)
					}

					imageBatch.coloredScale(
						minX + 5f * scale, skillY.toFloat(),
						scale / 8f, skill.element.thickSprite.index,
						0, rgba(1f, 1f, 1f, 0.75f),
					)

					if (skillMastery >= skill.masteryPoints) {
						renderFancyMasteredText(
							context, textBatch,
							minX + 25f * scale,
							skillY + 24f * scale,
							8f * scale
						)
					}

					val skillSprite = when (skill) {
						is ReactionSkill -> when (skill.type) {
							ReactionSkillType.MeleeAttack -> context.content.ui.meleeAttackIcon
							ReactionSkillType.RangedAttack -> context.content.ui.rangedAttackIcon
							ReactionSkillType.MeleeDefense -> context.content.ui.meleeDefenseIcon
							ReactionSkillType.RangedDefense -> context.content.ui.rangedDefenseIcon
						}
						is PassiveSkill -> context.content.ui.passiveIcon
						else -> context.content.skills.classes.find { it.actions.contains(skill) }!!.icon
					}

					var x = maxX - 20 * scale
					if (skill !is ActiveSkill) x -= 2 * scale
					if (skillSprite is BcSprite) {
						imageBatch.simpleScale(
							x.toFloat(), skillY + 3f * scale,
							scale * 0.16f, skillSprite.index
						)
					} else {
						spriteBatch.simple(
							x, skillY + 3 * scale, scale.toFloat(),
							(skillSprite as KimSprite).index
						)
					}
				}
			}

			if (interaction.descriptionIndex == 2) {

				textY = barY + 10 * scale
				fun addLine(text: String, color: Int) {
					val lineHeight = 5f * scale
					textBatch.drawShadowedString(
						"• $text", textMinX.toFloat(), textY.toFloat(),
						lineHeight, basicFont, color,
						strokeColor, strokeFactor * lineHeight,
						shadowColor, shadowFactor * lineHeight,
						shadowFactor * lineHeight, TextAlignment.LEFT,
					)
					textY += 7 * scale
				}

				for (stat in baseStats) {
					val adder = hoverItem.getModifier(stat)
					if (adder != 0) addLine("${stat.flashName}: $adder", lineColorLight)
				}

				val weapon = equipment?.weapon
				if (weapon != null) addLine("Critical chance: ${weapon.critChance}%", lineColorLight)

				if (equipment?.onlyUser != null) {
					addLine("ONLY USABLE BY ${equipment.onlyUser!!.name.uppercase()}", lineColorLight)
				}

				if (element != null) addLine("${element.properName} Elemental", lineColorLight)

				if (weapon != null) {
					for (bonus in weapon.effectiveAgainstCreatureTypes) {
						if (bonus.modifier > 0f) addLine("Effective against ${bonus.type.flashName}", goodTextColor)
						if (bonus.modifier < 0f) addLine("Ineffective against ${bonus.type.flashName}", badTextColor)
					}
					for (bonus in weapon.effectiveAgainstElements) {
						addLine("Effective against ${bonus.element.properName}", goodTextColor)
					}
					if (weapon.hpDrain > 0f) addLine("Drains HP", lineColorLight)
					for (effect in weapon.addEffects) {
						addLine("Inflicts ${effect.effect.flashName} (${effect.chance}%)", lineColorLight)
					}
				}

				if (equipment != null) {
					for (statModifier in equipment.stats) {
						if (baseStats.contains(statModifier.stat)) continue
						if (statModifier.adder > 0) {
							addLine("${statModifier.stat.flashName} + ${statModifier.adder}", goodTextColor)
						}
						if (statModifier.adder < 0) {
							addLine("${statModifier.stat.flashName} - ${-statModifier.adder}", badTextColor)
						}
					}

					for (effect in equipment.autoEffects) {
						addLine("Auto-${effect.niceName}", lineColorLight)
					}

					fun percentageString(modifier: Float) = "${(100f * modifier).roundToInt()}%"

					for (bonus in equipment.elementalBonuses) {
						addLine("Empowers ${bonus.element.properName} (${percentageString(bonus.modifier)})", goodTextColor)
					}

					for (resistance in equipment.resistances.elements) {
						if (resistance.modifier > 1f) {
							addLine("Absorb ${resistance.element.properName} (${percentageString(resistance.modifier - 1f)})", goodTextColor)
						} else if (resistance.modifier > 0f) {
							addLine("Resist ${resistance.element.properName} (${percentageString(resistance.modifier)})", goodTextColor)
						}
						if (resistance.modifier < 0f) {
							addLine("Vulnerable to ${resistance.element.properName} (${percentageString(-resistance.modifier)})", badTextColor)
						}
					}

					for (resistance in equipment.resistances.effects) {
						addLine("Resist ${resistance.effect.niceName} (${resistance.percentage}%)", goodTextColor)
					}

					if (equipment.charismaticPerformanceChance != 0) {
						addLine("Charismatic Performer (${equipment.charismaticPerformanceChance}%)", lineColorLight)
					}
				}
			}
		}

		val tabWidth = width / 3 - 3 * scale
		if (tabWidth < 3 * scale) return
		val maxY = maxY - 2 * scale
		val tabX1 = 4 * scale
		val tabX2 = tabX1 + tabWidth + 2 * scale
		val tabX3 = tabX2 + tabWidth + 2 * scale

		fun drawTab(text: String, index: Int, x: Int) {
			var lineColor = lineColorDark
			var textColor = srgbToLinear(rgb(110, 101, 95))
			if (interaction.descriptionIndex == index) {
				fun mix(left: Float) = rgb(
					left * normalize(red(midColorLight)) + (1f - left) * normalize(red(midColorDark)),
					left * normalize(green(midColorLight)) + (1f - left) * normalize(green(midColorDark)),
					left * normalize(blue(midColorLight)) + (1f - left) * normalize(blue(midColorDark)),
				)
				val leftColor = mix(1f - (x - minX).toFloat() / (maxX - minX).toFloat())
				val rightColor = mix(1f - (x + tabWidth - minX).toFloat() / (maxX - minX).toFloat())
				colorBatch.gradient(
					x, tabsY, x + tabWidth, maxY,
					leftColor, rightColor, leftColor,
				)
				lineColor = lineColorLight
				textColor = srgbToLinear(rgb(238, 203, 127))
			}

			colorBatch.fill(x, tabsY, x, maxY, lineColor)
			colorBatch.fill(x, maxY, x + tabWidth, maxY, lineColor)
			colorBatch.fill(x + tabWidth, tabsY, x + tabWidth, maxY, lineColor)
			textBatch.drawString(
				text, x + tabWidth / 2, maxY - 4 * scale, 6 * scale,
				largeFont, textColor, TextAlignment.CENTERED
			)
		}

		drawTab("Description", 0, tabX1)
		drawTab("Skills", 1, tabX2)
		drawTab("Properties", 2, tabX3)

		colorBatch.fill(
			minX, tabsY,
			if (interaction.descriptionIndex == 0) tabX1 else tabX2, tabsY, lineColorLight
		)
		colorBatch.fill((
				if (interaction.descriptionIndex == 1) tabX2 else tabX1) + tabWidth, tabsY,
			tabX3, tabsY, lineColorLight
		)
		if (interaction.descriptionIndex != 2) colorBatch.fill(
			tabX2 + tabWidth, tabsY,
			tabX3 + tabWidth, tabsY, lineColorLight
		)
	}
}
