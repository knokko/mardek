package mardek.renderer.ui.tabs

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.assets.Campaign
import mardek.assets.combat.CombatStat
import mardek.assets.inventory.EquipmentSlotType
import mardek.assets.skill.ActiveSkill
import mardek.assets.skill.PassiveSkill
import mardek.assets.skill.ReactionSkill
import mardek.assets.skill.ReactionSkillType
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.Kim1Renderer
import mardek.renderer.batch.KimRequest
import mardek.renderer.SharedResources
import mardek.state.ingame.CampaignState
import mardek.assets.inventory.ItemStack
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.menu.InventoryTab
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CHARACTER_BAR_HEIGHT = 23
private const val CHARACTER_BAR_WIDTH = 135
private const val SIMPLE_SLOT_SIZE = 18
private const val EQUIPMENT_SLOT_SIZE = 20

private const val BASE_WIDTH = 3 + 6 * EQUIPMENT_SLOT_SIZE + CHARACTER_BAR_WIDTH
private const val BASE_HEIGHT = 3 + 4 * CHARACTER_BAR_HEIGHT + 8 * SIMPLE_SLOT_SIZE

private val LINE_COLOR = srgbToLinear(rgb(179, 162, 116))
private val LIGHT_SLOT_COLOR = srgbToLinear(rgb(100, 80, 48))
private val DARK_SLOT_COLOR = srgbToLinear(rgb(74, 48, 30))

private val referenceTime = System.nanoTime()

class InventoryTabRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,

	private val tab: InventoryTab,
	private val region: AbsoluteRectangle,
	private val state: CampaignState,
	private val campaign: Campaign,
	private val resources: SharedResources,
) : TabRenderer() {

	private val scale = max(1, min(region.width / BASE_WIDTH, region.height / BASE_HEIGHT))

	private lateinit var kim1Batch: KimBatch
	private lateinit var kim2Batch: KimBatch

	private fun addKimRequest(request: KimRequest) {
		if (request.sprite.version == 1) kim1Batch.requests.add(request)
		else kim2Batch.requests.add(request)
	}

	override fun beforeRendering() {
		this.kim1Batch = resources.kim1Renderer.startBatch()
		this.kim2Batch = resources.kim2Renderer.startBatch()
		renderHoverItemProperties(null, resources.kim1Renderer)
		renderItemGrid(null, resources.kim1Renderer, null)
		renderCharacterBars(null, resources.kim1Renderer)

		kim1Batch.requests.add(KimRequest(
			x = region.minX + region.width / 2,
			y = 4 * scale, scale = scale.toFloat(),
			sprite = campaign.ui.goldIcon, opacity = 1f
		))
	}

	override fun render() {
		val uiRenderer = resources.uiRenderers[frameIndex]
		renderHoverItemProperties(uiRenderer, null)
		renderItemGrid(uiRenderer, null, null)
		renderCharacterBars(uiRenderer, null)

		val goldColor = srgbToLinear(rgb(254, 225, 123))
		uiRenderer.drawString(
			resources.font, state.gold.toString(), goldColor, intArrayOf(),
			region.minX + region.width / 2 + 20 * scale, 0, region.maxX, region.minY,
			18 * scale, 10 * scale, 1, TextAlignment.LEFT
		)
	}

	override fun postUiRendering() {
		resources.kim1Renderer.submit(kim1Batch, recorder, targetImage)
		resources.kim2Renderer.submit(kim2Batch, recorder, targetImage)

		val uiRenderer = resources.uiRenderers[frameIndex]
		uiRenderer.beginBatch()
		renderItemGrid(null, null, uiRenderer)
		uiRenderer.endBatch()
	}

	private fun getItemGridSize() = 8 * scale * SIMPLE_SLOT_SIZE + 2

	private fun getItemGridStartX() = region.boundX - getItemGridSize()

	private fun getItemGridStartY() = region.maxY - getItemGridSize() - 2 * scale

	private fun renderHoverItemProperties(uiRenderer: UiRenderer?, kim1Renderer: Kim1Renderer?) {
		val maxX = min(200 * scale, getItemGridStartX() - 2 * scale)
		val width = 1 + maxX - region.minX
		val startY = getItemGridStartY()
		val barY = startY + scale * SIMPLE_SLOT_SIZE
		val tabsY = region.maxY - 15 * scale

		val barColorLight = srgbToLinear(rgb(127, 93, 50))
		val barColorDark = srgbToLinear(rgb(66, 56, 48))
		uiRenderer?.fillColor(region.minX, startY, maxX, barY, barColorLight, Gradient(
			0, 0, width, barY - startY, barColorLight, barColorLight, barColorDark
		))

		val midColorLight = srgbToLinear(rgb(91, 74, 43))
		val midColorDark = srgbToLinear(rgb(60, 40, 28))
		val midGradient = Gradient(
			0, 0, width, tabsY - barY, midColorLight, midColorDark, midColorLight
		)
		uiRenderer?.fillColor(region.minX, barY, maxX, tabsY, midColorLight, midGradient)

		val hoverItem = tab.hoveringItem?.get()
		if (hoverItem != null) {
			val equipment = hoverItem.item.equipment
			val element = hoverItem.item.element
			if (element != null) {
				val elementColor = srgbToLinear(element.color)
				val lowElementColor = rgba(red(elementColor), green(elementColor), blue(elementColor), 100.toByte())
				val highElementColor = rgba(red(elementColor), green(elementColor), blue(elementColor), 30.toByte())
				uiRenderer?.fillColor(
					region.minX, startY + 5 * scale, maxX - 5 * scale,
					startY + 13 * scale, highElementColor, Gradient(
						0, 0, 2 * width, barY - startY, lowElementColor, 0, highElementColor
					)
				)

				if (kim1Renderer != null) kim2Batch.requests.add(KimRequest(
					x = region.minX + (maxX - region.minX - scale * element.sprite.width) / 2,
					y = barY + (tabsY - barY - scale * element.sprite.height) / 2,
					scale = scale.toFloat(), sprite = element.sprite, opacity = 0.02f
				))
			}

			val titleColor = srgbToLinear(rgb(238, 203, 127))
			uiRenderer?.drawString(
				resources.font, hoverItem.toString(), titleColor, intArrayOf(),
				region.minX + 5 * scale, startY, maxX - 5 * scale, barY,
				startY + 11 * scale, 8 * scale, 1, TextAlignment.DEFAULT
			)

			val baseTextColor = srgbToLinear(rgb(207, 192, 141))
			val goodTextColor = srgbToLinear(rgb(152, 254, 0))
			val badTextColor = srgbToLinear(rgb(254, 84, 84))

			val baseStats = mutableListOf<CombatStat>()
			if (equipment != null) {
				if (equipment.weapon == null) {
					baseStats.add(campaign.combat.stats.find { it.flashName == "DEF" }!!)
					baseStats.add(campaign.combat.stats.find { it.flashName == "MDEF" }!!)
				} else baseStats.add(campaign.combat.stats.find { it.flashName == "ATK" }!!)
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
					resources.font, highText, baseTextColor, intArrayOf(),
					textMinX, barY, textMaxX, tabsY, textY, 6 * scale, 1, TextAlignment.DEFAULT
				)
				textY += 20 * scale

				for (stat in baseStats) {
					val adder = hoverItem.item.getModifier(stat)
					if (adder != 0) {
						uiRenderer?.drawString(
							resources.font, "${stat.flashName}: $adder", baseTextColor, intArrayOf(),
							textMinX, barY, textMaxX, tabsY, textY, 6 * scale, 1, TextAlignment.DEFAULT
						)
						textY += 10 * scale
					}
				}
				textY += 12 * scale

				fun drawLine(currentLine: String) {
					uiRenderer?.drawString(
						resources.font, currentLine, baseTextColor, intArrayOf(),
						textMinX, barY, textMaxX, tabsY, textY, 5 * scale, 1, TextAlignment.DEFAULT
					)
					textY += 8 * scale
				}

				renderDescription(hoverItem.item.description, 35, ::drawLine)
			}

			if (tab.descriptionIndex == 1 && equipment != null) {
				val assetCharacter = state.characterSelection.party[tab.partyIndex]
				val characterState = state.characterStates[assetCharacter]!!

				for ((row, skill) in equipment.skills.withIndex()) {
					val skillY = barY + 2 * scale + 28 * row * scale

					val nameColor = srgbToLinear(rgb(238, 203, 127))
					uiRenderer?.drawString(
						resources.font, skill.name, nameColor, intArrayOf(),
						20 * scale, barY, maxX, tabsY,
						skillY + 10 * scale, 7 * scale, 1, TextAlignment.LEFT
					)

					val skillMastery = characterState.skillMastery[skill] ?: 0

					if (skillMastery < skill.masteryPoints && uiRenderer != null) {
						val darkColor = srgbToLinear(rgb(67, 51, 36))
						uiRenderer.fillColor(
							23 * scale, skillY + 17 * scale, 80 * scale, skillY + 23 * scale, darkColor
						)

						val textColor = srgbToLinear(rgb(253, 94, 94))
						uiRenderer.drawString(
							resources.font, skillMastery.toString(), textColor, intArrayOf(),
							43 * scale, barY, 55 * scale, tabsY,
							skillY + 25 * scale, 10 * scale, 1, TextAlignment.RIGHT
						)
						uiRenderer.drawString(
							resources.font, skill.masteryPoints.toString(), textColor, intArrayOf(),
							57 * scale, barY, maxX, tabsY,
							skillY + 24 * scale, 8 * scale, 1, TextAlignment.LEFT
						)
					}

					if (kim1Renderer != null) {
						kim2Batch.requests.add(KimRequest(
							x = 5 * scale, y = skillY, scale = scale / 8f,
							sprite = skill.element.sprite, opacity = 1f
						))

						if (skillMastery >= skill.masteryPoints) {
							kim1Batch.requests.add(KimRequest(
								x = 25 * scale, y = skillY + 16 * scale, scale = scale / 2f,
								sprite = campaign.ui.mastered, opacity = 1f
							))
						}

						val skillSprite = if (skill is ReactionSkill) {
							when (skill.type) {
								ReactionSkillType.MeleeAttack -> campaign.ui.meleeAttackIcon
								ReactionSkillType.RangedAttack -> campaign.ui.rangedAttackIcon
								ReactionSkillType.MeleeDefense -> campaign.ui.meleeDefenseIcon
								ReactionSkillType.RangedDefense -> campaign.ui.rangedDefenseIcon
							}
						} else if (skill is PassiveSkill) campaign.ui.passiveIcon else {
							val skillClass = campaign.skills.classes.find { it.actions.contains(skill) }!!
							skillClass.icon
						}

						var x = maxX - 20 * scale
						if (skill !is ActiveSkill) x -= 2 * scale
						addKimRequest(KimRequest(
							x = x, y = skillY + 3 * scale,
							scale = if (skill is ActiveSkill) scale.toFloat() else scale / 12f,
							sprite = skillSprite, opacity = 1f
						))
					}
				}
			}

			if (tab.descriptionIndex == 2 && uiRenderer != null) {

				textY = barY + 10 * scale

				val basePropertiesColor = rgb(220, 220, 220)
				fun addLine(text: String, color: Int) {
					uiRenderer.drawString(
						resources.font, "${11089.toChar()} $text", srgbToLinear(color), intArrayOf(),
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
						if (bonus.bonusFraction > 0f) addLine("Effective against ${bonus.type.flashName}", goodTextColor)
						if (bonus.bonusFraction < 0f) addLine("Ineffective against ${bonus.type.flashName}", badTextColor)
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

					for (resistance in equipment.elementalResistances) {
						if (resistance.modifier < -1f) {
							addLine("Absorb ${resistance.element.properName} (${percentageString(-1f - resistance.modifier)})", goodTextColor)
						} else if (resistance.modifier < 0f) {
							addLine("Resist ${resistance.element.properName} (${percentageString(-resistance.modifier)})", goodTextColor)
						}
						if (resistance.modifier > 0f) {
							addLine("Vulnerable to ${resistance.element.properName} (${percentageString(-resistance.modifier)})", badTextColor)
						}
					}

					for (resistance in equipment.statusResistances) {
						addLine("Resist ${resistance.effect.niceName} (${resistance.chance}%)", goodTextColor)
					}

					if (equipment.charismaticPerformanceChance != 0) {
						addLine("Charismatic Performer (${equipment.charismaticPerformanceChance}%)", basePropertiesColor)
					}
				}
			}
		}

		fun shift(amount: Int) = Gradient(
			midGradient.minX - amount, midGradient.minY, midGradient.width, midGradient.height,
			midGradient.baseColor, midGradient.rightColor, midGradient.upColor
		)

		val tabWidth = width / 3 - 3 * scale
		val maxY = region.maxY - 2 * scale
		val tabX1 = 4 * scale
		val tabX2 = tabX1 + tabWidth + 2 * scale
		val tabX3 = tabX2 + tabWidth + 2 * scale

		val lineColorLight = srgbToLinear(rgb(208, 193, 142))
		val lineColorDark = srgbToLinear(rgb(140, 94, 47))

		fun drawTab(text: String, index: Int, x: Int) {
			var lineColor = lineColorDark
			var textColor = srgbToLinear(rgb(110, 101, 95))
			if (tab.descriptionIndex == index) {
				uiRenderer?.fillColor(x, tabsY, x + tabWidth, maxY, midColorDark, shift(x))
				lineColor = lineColorLight
				textColor = srgbToLinear(rgb(238, 203, 127))
			}

			uiRenderer?.fillColor(x, tabsY, x, maxY, lineColor)
			uiRenderer?.fillColor(x, maxY, x + tabWidth, maxY, lineColor)
			uiRenderer?.fillColor(x + tabWidth, tabsY, x + tabWidth, maxY, lineColor)
			uiRenderer?.drawString(
				resources.font, text, textColor, intArrayOf(),
				x + scale, tabsY, x + tabWidth - scale, maxY,
				maxY - 4 * scale, 4 * scale, 1, TextAlignment.CENTER
			)
		}

		drawTab("DESCRIPTION", 0, tabX1)
		drawTab("SKILLS", 1, tabX2)
		drawTab("PROPERTIES", 2, tabX3)

		uiRenderer?.fillColor(region.minX, tabsY, if (tab.descriptionIndex == 0) tabX1 else tabX2, tabsY, lineColorLight)
		uiRenderer?.fillColor((if (tab.descriptionIndex == 1) tabX2 else tabX1) + tabWidth, tabsY, tabX3, tabsY, lineColorLight)
		if (tab.descriptionIndex != 2) uiRenderer?.fillColor(tabX2 + tabWidth, tabsY, tabX3 + tabWidth, tabsY, lineColorLight)
	}

	private fun renderItemGrid(uiRenderer: UiRenderer?, kim1Renderer: Kim1Renderer?, amountRenderer: UiRenderer?) {
		val fullSlotSize = scale * SIMPLE_SLOT_SIZE
		val size = getItemGridSize()
		val startX = getItemGridStartX()
		val startY = getItemGridStartY()

		uiRenderer?.fillColor(startX, startY, region.maxX, startY, LINE_COLOR)
		for (row in 0 until 8) {
			val minY = 1 + startY + row * fullSlotSize
			val maxY = minY + fullSlotSize - 1
			uiRenderer?.fillColor(startX, minY, region.maxX, maxY, LINE_COLOR, Gradient(
				2, 1, size - 4, fullSlotSize - 2,
				LIGHT_SLOT_COLOR, LIGHT_SLOT_COLOR, DARK_SLOT_COLOR
			))
		}
		uiRenderer?.fillColor(startX, startY + size - 1, region.maxX, startY + size - 1, LINE_COLOR)

		for (column in 1 until 8) {
			val x = startX + column * fullSlotSize
			uiRenderer?.fillColor(x, startY + 2, x + 1, startY + 8 * fullSlotSize - 1, LINE_COLOR)
		}

		val hoveredItem = tab.hoveringItem
		if (hoveredItem != null) {
			val hoverLineColor = srgbToLinear(rgb(165, 205, 254))
			val hoverLightColor = srgbToLinear(rgb(25, 68, 118))
			val hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
			if (hoveredItem.slotIndex >= 0) {
				val slotX = hoveredItem.slotIndex % 8
				val slotY = hoveredItem.slotIndex / 8

				val x = startX + 1 + slotX * fullSlotSize
				val y = startY + 1 + slotY * fullSlotSize
				uiRenderer?.fillColor(x, y, x + fullSlotSize - 1, y + fullSlotSize - 1, hoverLineColor, Gradient(
					1, 1, fullSlotSize - 2, fullSlotSize - 2,
					hoverLightColor, hoverLightColor, hoverDarkColor
				))
			}
		}

		fun renderAmount(stack: ItemStack, itemX: Int, itemY: Int) {
			if (amountRenderer == null || stack.amount == 1) return

			val textColor = srgbToLinear(rgb(238, 203, 127))
			val shadowColor = rgba(0, 0, 0, 200)
			for (offsetX in arrayOf(-scale, 0, scale)) {
				for (offsetY in arrayOf(-scale, 0, scale)) {
					amountRenderer.drawString(
						resources.font, stack.amount.toString(), shadowColor, intArrayOf(),
						itemX + 5 * scale + offsetX, itemY, itemX + 19 * scale + offsetX, itemY + 20 * scale,
						itemY + 15 * scale + offsetY, 6 * scale, 1, TextAlignment.CENTER
					)
				}
			}
			amountRenderer.drawString(
				resources.font, stack.amount.toString(), textColor, intArrayOf(),
				itemX + 5 * scale, itemY, itemX + 19 * scale, itemY + 20 * scale,
				itemY + 15 * scale, 6 * scale, 1, TextAlignment.CENTER
			)
		}

		val pickedItem = tab.pickedUpItem

		val party = state.characterSelection.party
		val selectedCharacter = party[tab.partyIndex]
		if (selectedCharacter != null) {
			val inventory = state.characterStates[selectedCharacter]!!.inventory
			for (y in 0 until 8) {
				for (x in 0 until 8) {
					val itemStack = inventory[x + 8 * y] ?: continue
					if (pickedItem != null && itemStack === pickedItem.get()) continue
					val itemX = startX + 1 + fullSlotSize * x + scale
					val itemY = startY + 1 + fullSlotSize * y + scale
					if (kim1Renderer != null) {
						kim1Batch.requests.add(KimRequest(
							x = itemX, y = itemY, scale = scale.toFloat(), sprite = itemStack.item.sprite, opacity = 1f
						))
					}
					renderAmount(itemStack, itemX, itemY)
				}
			}
		}

		if (pickedItem != null && tab.mouseX >= 0 && tab.mouseY >= 0) {
			if (kim1Renderer != null) {
				kim1Batch.requests.add(KimRequest(
					x = tab.mouseX, y = tab.mouseY, scale = scale.toFloat(),
					sprite = pickedItem.get()!!.item.sprite, opacity = 1f
				))
			}
			renderAmount(pickedItem.get()!!, tab.mouseX, tab.mouseY)
		}

		tab.renderItemsStartX = startX + 1
		tab.renderItemsStartY = startY + 1
		tab.renderItemSlotSize = fullSlotSize
	}

	private fun renderCharacterBars(uiRenderer: UiRenderer?, kim1Renderer: Kim1Renderer?) {
		for (index in state.characterSelection.party.indices) {
			renderCharacterBar(index, uiRenderer, kim1Renderer)
		}
	}

	private fun renderCharacterBar(partyIndex: Int, uiRenderer: UiRenderer?, kim1Renderer: Kim1Renderer?) {
		val assetCharacter = state.characterSelection.party[partyIndex] ?: return
		val characterState = state.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		val barHeight = scale * CHARACTER_BAR_HEIGHT
		val startX = region.minX + 5 * scale
		val startY = region.minY + 3 * scale + partyIndex * barHeight
		val width = region.boundX - startX

		val lineColor = srgbToLinear(rgb(165, 151, 110))
		val outerLightColor = srgbToLinear(rgb(89, 72, 42))
		val outerRightColor = srgbToLinear(rgb(104, 80, 47))
		val outerDarkColor = srgbToLinear(rgb(39, 26, 16))
		uiRenderer?.fillColor(startX, startY, region.maxX, startY + barHeight - 1, lineColor, Gradient(
			2, 2, width - 4, barHeight - 3,
			outerLightColor, outerRightColor, outerDarkColor
		))

		val innerDarkColor = srgbToLinear(rgb(113, 88, 58))
		val innerLightColor = srgbToLinear(rgb(119, 105, 91))
		val margin = 3 * scale
		uiRenderer?.fillColor(
			startX + margin, startY + margin, region.maxX - margin,
			startY + barHeight * 2 / 3, innerDarkColor, Gradient(
				0, 0, region.width, barHeight, innerDarkColor, innerDarkColor, innerLightColor
			)
		)

		if (partyIndex == tab.partyIndex) {
			val selectedColor = rgba(0, 30, 150, 100)
			uiRenderer?.fillColor(startX, startY, region.maxX, startY + barHeight - 1, selectedColor)
		}

		val characterX = startX + margin + margin / 2
		val characterY = startY + margin + scale
		if (kim1Renderer != null) {
			var spriteIndex = 0
			val passedTime = System.nanoTime() - referenceTime
			val animationPeriod = 700_000_000L
			if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1
			kim1Batch.requests.add(KimRequest(
				x = characterX, y = characterY, scale = scale.toFloat(),
				sprite = assetCharacter.areaSprites.sprites[spriteIndex], opacity = 1f
			))
		}

		val x1 = characterX + 16 * scale + margin / 2

		val elementColor = srgbToLinear(assetCharacter.element.color)
		val lowElementColor = rgba(red(elementColor), green(elementColor), blue(elementColor), 150.toByte())
		val highElementColor = rgba(red(elementColor), green(elementColor), blue(elementColor), 50.toByte())
		uiRenderer?.fillColor(
			x1, startY + margin, characterX + 80 * scale,
			startY + margin + 6 * scale, highElementColor, Gradient(
				0, 0, region.width, region.height, lowElementColor, 0, highElementColor
			)
		)

		val x2 = x1 + 25 * scale
		val x3 = x2 + 16 * scale
		val x4 = x3 + 8 * scale

		if (kim1Renderer != null) {
			val iconY = startY + barHeight / 2 + margin / 2
			val iconScale = scale / 8f
			addKimRequest(KimRequest(
				x = x1, y = iconY, scale = iconScale, sprite = campaign.ui.attackIcon, opacity = 1f
			))
			addKimRequest(KimRequest(
				x = x2, y = iconY, scale = iconScale, sprite = campaign.ui.defIcon, opacity = 1f
			))
			addKimRequest(KimRequest(
				x = x4, y = iconY, scale = iconScale, sprite = campaign.ui.rangedDefIcon, opacity = 1f
			))
		}

		val barX = x4 + 20 * scale

		if (uiRenderer != null) {
			val statsY = startY + barHeight - margin * 4 / 3
			val statsColor = srgbToLinear(rgb(238, 203, 127))
			val statsHeight = 5 * scale

			val attack = characterState.determineValue(
				assetCharacter.baseStats, campaign.combat.stats.find { it.flashName == "ATK" }!!
			)
			val defense = characterState.determineValue(
				assetCharacter.baseStats, campaign.combat.stats.find { it.flashName == "DEF" }!!
			)
			val rangedDefense = characterState.determineValue(
				assetCharacter.baseStats, campaign.combat.stats.find { it.flashName == "MDEF" }!!
			)

			uiRenderer.drawString(
				resources.font, attack.toString(), statsColor, intArrayOf(),
				x1 + 10 * scale, region.minY, x2, region.maxY, statsY, statsHeight,
				1, TextAlignment.LEFT
			)
			uiRenderer.drawString(
				resources.font, defense.toString(), statsColor, intArrayOf(),
				x2 + 10 * scale, region.minY, x4, region.maxY, statsY, statsHeight,
				1, TextAlignment.LEFT
			)
			uiRenderer.drawString(
				resources.font, rangedDefense.toString(), statsColor, intArrayOf(),
				x4 + 10 * scale, region.minY, barX, region.maxY, statsY, statsHeight,
				1, TextAlignment.LEFT
			)
		}

		val textColor = srgbToLinear(rgb(238, 204, 127))
		val textHeight = 5 * scale
		uiRenderer?.drawString(
			resources.font, assetCharacter.name, textColor, intArrayOf(),
			x1 + margin / 2, region.minY, x3, region.maxY,
			startY + margin + 5 * scale, textHeight, 1, TextAlignment.LEFT
		)
		uiRenderer?.drawString(
			resources.font, "Lv${characterState.currentLevel}", textColor, intArrayOf(),
			x3, region.minY, barX, region.maxY,
			startY + margin + 5 * scale, textHeight, 1, TextAlignment.LEFT
		)

		// TODO proper health & mana bar
		val baseBarWidth = 40 * scale
		val barsHeight = margin
		val bottomHealthColor = srgbToLinear(rgb(75, 179, 42))
		val topHealthColor = srgbToLinear(rgb(182, 229, 163))
		val rightHealthColor = srgbToLinear(rgb(26, 89, 45))
		uiRenderer?.fillColor(
			barX, startY + margin * 13 / 8, barX + baseBarWidth,
			startY + margin * 13 / 8 + barsHeight, bottomHealthColor, Gradient(
				0, 0, baseBarWidth, barsHeight, bottomHealthColor, rightHealthColor, topHealthColor
			)
		)

		val healthTextColor = srgbToLinear(rgb(122, 217, 62))
		val maxHealth = characterState.determineMaxHealth(assetCharacter.baseStats, campaign.combat.stats)
		uiRenderer?.drawString(
			resources.font, "${characterState.currentHealth}/$maxHealth", healthTextColor, intArrayOf(),
			barX, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 34 / 8, margin * 3 / 2, 1, TextAlignment.CENTER
		)

		val bottomManaColor = srgbToLinear(rgb(8, 122, 178))
		val topManaColor = srgbToLinear(rgb(152, 204, 230))
		val rightManaColor = srgbToLinear(rgb(12, 207, 159))
		uiRenderer?.fillColor(
			barX, startY + margin * 37 / 8, barX + baseBarWidth,
			startY + margin * 37 / 8 + barsHeight, bottomManaColor, Gradient(
				0, 0, baseBarWidth, barsHeight, bottomManaColor, rightManaColor, topManaColor
			)
		)

		val manaTextColor = srgbToLinear(rgb(35, 227, 240))
		val maxMana = characterState.determineMaxMana(assetCharacter.baseStats, campaign.combat.stats)
		uiRenderer?.drawString(
			resources.font, "${characterState.currentMana}/$maxMana", manaTextColor, intArrayOf(),
			barX, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 59 / 8, margin * 3 / 2, 1, TextAlignment.CENTER
		)

		renderEquipment(partyIndex, startY + margin - 1, uiRenderer, kim1Renderer)
	}

	private fun renderEquipment(partyIndex: Int, startY: Int, uiRenderer: UiRenderer?, kim1Renderer: Kim1Renderer?) {
		val startX = region.maxX - 6 * scale * EQUIPMENT_SLOT_SIZE
		val largeSlotSize = scale * EQUIPMENT_SLOT_SIZE
		val slotSize = scale * SIMPLE_SLOT_SIZE

		if (partyIndex == 0) {
			tab.renderEquipmentStartX = startX
			tab.renderEquipmentStartY = startY
			tab.renderEquipmentSlotSize = slotSize
			tab.renderEquipmentSlotSpacing = largeSlotSize
			tab.renderEquipmentCharacterSpacing = scale * CHARACTER_BAR_HEIGHT
		}

		val assetCharacter = state.characterSelection.party[partyIndex] ?: return
		val characterState = state.characterStates[assetCharacter] ?: throw IllegalStateException("Missing state for $assetCharacter")
		val equipment = characterState.equipment

		val pickedItem = tab.pickedUpItem
		val lineColor = srgbToLinear(rgb(208, 193, 142))
		for ((column, item) in equipment.withIndex()) {
			val minX = startX + column * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			uiRenderer?.fillColor(minX, startY, maxX, startY, lineColor)
			uiRenderer?.fillColor(minX, maxY, maxX, maxY, lineColor)
			uiRenderer?.fillColor(minX, startY, minX, maxY, lineColor)
			uiRenderer?.fillColor(maxX, startY, maxX, maxY, lineColor)
			if (pickedItem != null && (-pickedItem.slotIndex - 1) == column && pickedItem.characterState == characterState) continue
			if (kim1Renderer != null && item != null) kim1Batch.requests.add(KimRequest(
				x = minX + scale, y = startY + scale, scale = scale.toFloat(), sprite = item.sprite, opacity = 1f
			))
		}

		var hoverLineColor = srgbToLinear(rgb(165, 205, 254))
		var hoverLightColor = srgbToLinear(rgb(25, 68, 118))
		var hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
		val hoveredItem = tab.hoveringItem
		if (hoveredItem != null && hoveredItem.slotIndex < 0 && hoveredItem.characterState == characterState) {
			if (pickedItem != null && pickedItem != hoveredItem && !hoveredItem.canInsert(pickedItem.get()!!.item)) {
				hoverLineColor = srgbToLinear(rgb(255, 162, 162))
				hoverDarkColor = srgbToLinear(rgb(134, 107, 90))
				hoverLightColor = srgbToLinear(rgb(134, 30, 20))
			}
			val minX = startX + (-hoveredItem.slotIndex - 1) * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			uiRenderer?.fillColor(minX, startY, maxX, maxY, hoverLineColor, Gradient(
				1, 1, slotSize - 2, slotSize - 2,
				hoverLightColor, hoverLightColor, hoverDarkColor
			))
		}
	}
}
