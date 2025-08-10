package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.menu.MenuRenderContext
import mardek.state.ingame.menu.InventoryTab

internal fun renderHoverItemProperties(
	menuContext: MenuRenderContext, minX: Int, startY: Int, maxX: Int, maxY: Int, scale: Int,
) {
	menuContext.run {
		val width = 1 + maxX - minX
		val barY = startY + scale * SIMPLE_SLOT_SIZE
		val tabsY = maxY - 15 * scale

		val barColorLight = srgbToLinear(rgb(127, 93, 50))
		val barColorDark = srgbToLinear(rgb(66, 56, 48))
		colorBatch1.gradient(
			minX, startY, maxX, barY,
		barColorLight, barColorLight, barColorDark
		)

		val midColorLight = srgbToLinear(rgb(91, 74, 43))
		val midColorDark = srgbToLinear(rgb(60, 40, 28))
		colorBatch1.gradient(
			minX, barY, maxX, tabsY,
			midColorLight, midColorDark, midColorLight
		)

		val tab = menu.currentTab as InventoryTab
		val hoverItem = tab.hoveringItem?.get()
		if (hoverItem != null) {
			val equipment = hoverItem.item.equipment
			val element = hoverItem.item.element
			if (element != null) {
				val elementColor = srgbToLinear(element.color)
				val lowElementColor = changeAlpha(elementColor, 100)
				val highElementColor = changeAlpha(elementColor, 30)
				colorBatch1.gradient(
					minX, startY + 5 * scale,
					maxX - 5 * scale, startY + 13 * scale,
					lowElementColor, 0, highElementColor
				)


				if (kim1Renderer != null) kim2Batch.requests.add(KimRequest(
					x = minX + (maxX - minX - scale * element.sprite.width) / 2,
					y = barY + (tabsY - barY - scale * element.sprite.height) / 2,
					scale = scale.toFloat(), sprite = element.sprite, opacity = 0.02f
				))
			}

			val titleColor = srgbToLinear(rgb(238, 203, 127))
			uiRenderer?.drawString(
				context.resources.font, hoverItem.toString(), titleColor, intArrayOf(),
				minX + 5 * scale, startY, maxX - 5 * scale, barY,
				startY + 11 * scale, 8 * scale, 1, TextAlignment.DEFAULT
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
			val textMaxX = maxX - 5 * scale
			var textY = barY + 13 * scale
			if (tab.descriptionIndex == 0) {
				var highText = ""
				if (equipment != null) {
					if (equipment.weapon != null) highText = "WEAPON: ${equipment.weapon!!.type.flashName}"
					if (equipment.armorType != null) {
						highText = equipment.armorType!!.name
						if (tab.hoveringItem!!.getEquipmentType() == EquipmentSlotType.Body) highText = "ARMOUR: $highText"
						if (tab.hoveringItem!!.getEquipmentType() == EquipmentSlotType.Head) highText = "HELMET: $highText"
					}
					if (equipment.gem != null) highText = "GEMSTONE"
					if (highText.isEmpty()) highText = "ACCESSORY"
				}
				if (hoverItem.item.consumable != null) highText = "EXPENDABLE ITEM"
				if (highText.isEmpty()) highText = "MISCELLANEOUS ITEM"

				uiRenderer?.drawString(
					context.resources.font, highText, baseTextColor, intArrayOf(),
					textMinX, barY, textMaxX, tabsY, textY, 6 * scale, 1, TextAlignment.DEFAULT
				)
				textY += 20 * scale

				for (stat in baseStats) {
					val adder = hoverItem.item.getModifier(stat)
					if (adder != 0) {
						uiRenderer?.drawString(
							context.resources.font, "${stat.flashName}: $adder", baseTextColor, intArrayOf(),
							textMinX, barY, textMaxX, tabsY, textY, 6 * scale, 1, TextAlignment.DEFAULT
						)
						textY += 10 * scale
					}
				}
				textY += 12 * scale

				fun drawLine(currentLine: String) {
					uiRenderer?.drawString(
						context.resources.font, currentLine, baseTextColor, intArrayOf(),
						textMinX, barY, textMaxX, tabsY, textY, 5 * scale, 1, TextAlignment.DEFAULT
					)
					textY += 8 * scale
				}

				renderDescription(hoverItem.item.description, 35, ::drawLine)
			}

			if (tab.descriptionIndex == 1 && equipment != null) {
				val assetCharacter = context.campaign.characterSelection.party[tab.partyIndex]
				val characterState = context.campaign.characterStates[assetCharacter]!!

				for ((row, skill) in equipment.skills.withIndex()) {
					val skillY = barY + 2 * scale + 28 * row * scale

					val nameColor = srgbToLinear(rgb(238, 203, 127))
					uiRenderer?.drawString(
						context.resources.font, skill.name, nameColor, intArrayOf(),
						20 * scale, barY, maxX, tabsY,
						skillY + 10 * scale, 7 * scale, 1, TextAlignment.LEFT
					)

					val skillMastery = characterState.skillMastery[skill] ?: 0

					if (skillMastery < skill.masteryPoints && uiRenderer != null) {
						val masteryRenderer = ResourceBarRenderer(
							context, ResourceType.SkillMastery, AbsoluteRectangle(
								23 * scale, skillY + 17 * scale, 57 * scale, 6 * scale
							)
						)
						masteryRenderer.renderBar(skillMastery, skill.masteryPoints)
						masteryRenderer.renderTextOverBar(skillMastery, skill.masteryPoints)
					}

					if (kim1Renderer != null) {
						kim2Batch.requests.add(KimRequest(
							x = 5 * scale, y = skillY, scale = scale / 8f,
							sprite = skill.element.sprite
						))

						if (skillMastery >= skill.masteryPoints) {
							kim1Batch.requests.add(KimRequest(
								x = 25 * scale, y = skillY + 16 * scale, scale = scale / 2f,
								sprite = context.content.ui.mastered
							))
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
						addKimRequest(KimRequest(
							x = x, y = skillY + 3 * scale,
							scale = if (skill is ActiveSkill) scale.toFloat() else scale / 12f,
							sprite = skillSprite
						))
					}
				}
			}

			if (tab.descriptionIndex == 2 && uiRenderer != null) {

				textY = barY + 10 * scale

				val basePropertiesColor = rgb(220, 220, 220)
				fun addLine(text: String, color: Int) {
					uiRenderer.drawString(
						context.resources.font, "${11089.toChar()} $text", srgbToLinear(color), intArrayOf(),
						textMinX, barY, textMaxX, tabsY, textY, 4 * scale, 1, TextAlignment.DEFAULT
					)
					textY += 7 * scale
				}

				for (stat in baseStats) {
					val adder = hoverItem.item.getModifier(stat)
					if (adder != 0) addLine("${stat.flashName}: $adder", basePropertiesColor)
				}

				val weapon = equipment?.weapon
				if (weapon != null) addLine("Critical chance: ${weapon.critChance}%", basePropertiesColor)

				if (equipment?.onlyUser != null) {
					addLine("ONLY USABLE BY ${equipment.onlyUser!!.uppercase()}", basePropertiesColor)
				}

				if (element != null) addLine("${element.properName} Elemental", basePropertiesColor)

				if (weapon != null) {
					for (bonus in weapon.effectiveAgainstCreatureTypes) {
						if (bonus.modifier > 0f) addLine("Effective against ${bonus.type.flashName}", goodTextColor)
						if (bonus.modifier < 0f) addLine("Ineffective against ${bonus.type.flashName}", badTextColor)
					}
					for (bonus in weapon.effectiveAgainstElements) {
						addLine("Effective against ${bonus.element.properName}", goodTextColor)
					}
					if (weapon.hpDrain > 0f) addLine("Drains HP", basePropertiesColor)
					for (effect in weapon.addEffects) {
						addLine("Inflicts ${effect.effect.flashName} (${effect.chance}%)", basePropertiesColor)
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
						addLine("Auto-${effect.niceName}", basePropertiesColor)
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
						addLine("Charismatic Performer (${equipment.charismaticPerformanceChance}%)", basePropertiesColor)
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

		val lineColorLight = srgbToLinear(rgb(208, 193, 142))
		val lineColorDark = srgbToLinear(rgb(140, 94, 47))

		fun drawTab(text: String, index: Int, x: Int) {
			var lineColor = lineColorDark
			var textColor = srgbToLinear(rgb(110, 101, 95))
			if (tab.descriptionIndex == index) {
				val midGradient = Gradient(
					0, 0, width, tabsY - barY,
					midColorLight, midColorDark, midColorLight
				)
				fun shift(amount: Int) = Gradient(
					midGradient.minX - amount, midGradient.minY, midGradient.width, midGradient.height,
					midGradient.baseColor, midGradient.rightColor, midGradient.upColor
				)
				uiRenderer?.fillColor(x, tabsY, x + tabWidth, maxY, midColorDark, shift(x))
				lineColor = lineColorLight
				textColor = srgbToLinear(rgb(238, 203, 127))
			}

			rectangleRenderer?.fill(x, tabsY, x, maxY, lineColor)
			rectangleRenderer?.fill(x, maxY, x + tabWidth, maxY, lineColor)
			rectangleRenderer?.fill(x + tabWidth, tabsY, x + tabWidth, maxY, lineColor)
			uiRenderer?.drawString(
				context.resources.font, text, textColor, intArrayOf(),
				x + scale, tabsY, x + tabWidth - scale, maxY,
				maxY - 4 * scale, 4 * scale, 1, TextAlignment.CENTER
			)
		}

		drawTab("DESCRIPTION", 0, tabX1)
		drawTab("SKILLS", 1, tabX2)
		drawTab("PROPERTIES", 2, tabX3)

		rectangleRenderer?.fill(
			minX, tabsY,
			if (tab.descriptionIndex == 0) tabX1 else tabX2, tabsY, lineColorLight
		)
		rectangleRenderer?.fill((
				if (tab.descriptionIndex == 1) tabX2 else tabX1) + tabWidth, tabsY,
			tabX3, tabsY, lineColorLight
		)
		if (tab.descriptionIndex != 2) rectangleRenderer?.fill(
			tabX2 + tabWidth, tabsY,
			tabX3 + tabWidth, tabsY, lineColorLight
		)
	}

}
