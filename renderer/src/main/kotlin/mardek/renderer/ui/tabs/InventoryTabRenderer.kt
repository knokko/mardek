package mardek.renderer.ui.tabs

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.assets.Campaign
import mardek.assets.combat.CombatStat
import mardek.assets.inventory.EquipmentSlotType
import mardek.assets.skill.ActiveSkill
import mardek.assets.skill.PassiveSkill
import mardek.assets.skill.ReactionSkill
import mardek.assets.skill.ReactionSkillType
import mardek.renderer.KimBatch
import mardek.renderer.KimRenderer
import mardek.renderer.KimRequest
import mardek.renderer.SharedResources
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.title.AbsoluteRectangle
import java.lang.StringBuilder
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

	private lateinit var kimBatch: KimBatch

	override fun beforeRendering() {
		this.kimBatch = resources.kimRenderer.startBatch()
		renderHoverItemProperties(null, resources.kimRenderer)
		renderItemGrid(null, resources.kimRenderer)
		renderCharacterBars(null, resources.kimRenderer)

		kimBatch.requests.add(KimRequest(
			x = region.minX + region.width / 2,
			y = 4 * scale, scale = scale.toFloat(),
			sprite = campaign.ui.goldIcon, opacity = 1f
		))
	}

	override fun render() {
		val uiRenderer = resources.uiRenderers[frameIndex]
		renderHoverItemProperties(uiRenderer, null)
		renderItemGrid(uiRenderer, null)
		renderCharacterBars(uiRenderer, null)

		val goldColor = srgbToLinear(rgb(254, 225, 123))
		uiRenderer.drawString(
			resources.font, state.gold.toString(), goldColor, intArrayOf(),
			region.minX + region.width / 2 + 20 * scale, 0, region.maxX, region.minY,
			18 * scale, 10 * scale, 1
		)
	}

	override fun postUiRendering() {
		resources.kimRenderer.submit(kimBatch, recorder, targetImage, frameIndex)
	}

	private fun getItemGridSize() = 8 * scale * SIMPLE_SLOT_SIZE + 2

	private fun getItemGridStartX() = region.boundX - getItemGridSize()

	private fun getItemGridStartY() = region.maxY - getItemGridSize() - 2 * scale

	private fun renderHoverItemProperties(uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
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

				val availableSize = min(maxX - region.minX, tabsY - barY)
				val spriteScale = 0.8f * availableSize.toFloat() / element.thickSprite.width
				val spriteSize = (element.thickSprite.width * spriteScale).roundToInt()

				if (kimRenderer != null) kimBatch.requests.add(
					KimRequest(
					x = region.minX + (maxX - region.minX - spriteSize) / 2,
					y = barY + (tabsY - barY - spriteSize) / 2,
					scale = spriteScale, sprite = element.thickSprite, opacity = 0.02f
				)
				)
			}

			val titleColor = srgbToLinear(rgb(238, 203, 127))
			uiRenderer?.drawString(
				resources.font, hoverItem.toString(), titleColor, intArrayOf(),
				region.minX + 5 * scale, startY, maxX, barY, startY + 11 * scale, 8 * scale, 1
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

			val textX = 5 * scale
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
					textX, barY, maxX, tabsY, textY, 6 * scale, 1
				)
				textY += 20 * scale

				for (stat in baseStats) {
					val adder = hoverItem.item.getModifier(stat)
					if (adder != 0) {
						uiRenderer?.drawString(
							resources.font, "${stat.flashName}: $adder", baseTextColor, intArrayOf(),
							textX, barY, maxX, tabsY, textY, 6 * scale, 1
						)
						textY += 10 * scale
					}
				}
				textY += 12 * scale

				val splitDescription = hoverItem.item.description.split(" ")
				val lineWidth = 35
				val currentLine = StringBuilder(lineWidth)

				fun drawLine() {
					uiRenderer?.drawString(
						resources.font, currentLine.toString(), baseTextColor, intArrayOf(),
						textX, barY, maxX, tabsY, textY, 6 * scale, 1
					)
					textY += 8 * scale
					currentLine.clear()
				}
				for (word in splitDescription) {
					if (currentLine.isNotEmpty() && currentLine.length + word.length >= lineWidth) drawLine()
					currentLine.append(word).append(' ')
				}
				if (currentLine.isNotEmpty()) drawLine()
			}

			if (tab.descriptionIndex == 1 && equipment != null) {
				for ((row, skill) in equipment.skills.withIndex()) {
					val skillY = barY + 5 * scale + 40 * row * scale

					if (kimRenderer != null) {
						kimBatch.requests.add(KimRequest(
							x = 5 * scale, y = skillY, scale = scale / 3f,
							sprite = skill.element.thickSprite, opacity = 1f
						))

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
						kimBatch.requests.add(KimRequest(
							x = maxX - 20 * scale, y = skillY, scale = scale / 4f,
							sprite = skillSprite, opacity = 1f
						))
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
			// TODO Center?
			uiRenderer?.drawString(
				resources.font, text, textColor, intArrayOf(),
				x + scale, tabsY, x + tabWidth, maxY, maxY - 4 * scale, 5 * scale, 1
			)
		}

		drawTab("DESCRIPTION", 0, tabX1)
		drawTab("SKILLS", 1, tabX2)
		drawTab("PROPERTIES", 2, tabX3)

		uiRenderer?.fillColor(region.minX, tabsY, if (tab.descriptionIndex == 0) tabX1 else tabX2, tabsY, lineColorLight)
		uiRenderer?.fillColor((if (tab.descriptionIndex == 1) tabX2 else tabX1) + tabWidth, tabsY, tabX3, tabsY, lineColorLight)
		if (tab.descriptionIndex != 2) uiRenderer?.fillColor(tabX2 + tabWidth, tabsY, tabX3 + tabWidth, tabsY, lineColorLight)
	}

	private fun renderItemGrid(uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
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

		val pickedItem = tab.pickedUpItem

		if (kimRenderer != null) {
			val party = state.characterSelection.party
			val selectedCharacter = party[tab.partyIndex]
			if (selectedCharacter != null) {
				val inventory = state.characterStates[selectedCharacter]!!.inventory
				for (y in 0 until 8) {
					for (x in 0 until 8) {
						val itemStack = inventory[x + 8 * y] ?: continue
						if (pickedItem != null && itemStack === pickedItem.get()) continue
						kimBatch.requests.add(KimRequest(
							x = startX + 1 + fullSlotSize * x + scale,
							y = startY + 1 + fullSlotSize * y + scale,
							scale = scale.toFloat(), sprite = itemStack.item.sprite, opacity = 1f
						))
					}
				}
			}

			if (pickedItem != null && tab.mouseX >= 0 && tab.mouseY >= 0) {
				kimBatch.requests.add(KimRequest(
					x = tab.mouseX, y = tab.mouseY, scale = scale.toFloat(),
					sprite = pickedItem.get()!!.item.sprite, opacity = 1f
				))
			}
		}

		tab.renderItemsStartX = startX + 1
		tab.renderItemsStartY = startY + 1
		tab.renderItemSlotSize = fullSlotSize
	}

	private fun renderCharacterBars(uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
		for (index in state.characterSelection.party.indices) {
			renderCharacterBar(index, uiRenderer, kimRenderer)
		}
	}

	private fun renderCharacterBar(partyIndex: Int, uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
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
		if (kimRenderer != null) {
			var spriteIndex = 0
			val passedTime = System.nanoTime() - referenceTime
			val animationPeriod = 700_000_000L
			if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1
			kimBatch.requests.add(KimRequest(
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

		if (kimRenderer != null) {
			val iconY = startY + barHeight / 2 + margin / 2
			val iconScale = scale / 8f
			kimBatch.requests.add(KimRequest(
				x = x1, y = iconY, scale = iconScale, sprite = campaign.ui.attackIcon, opacity = 1f
			))
			kimBatch.requests.add(KimRequest(
				x = x2, y = iconY, scale = iconScale, sprite = campaign.ui.defIcon, opacity = 1f
			))
			kimBatch.requests.add(KimRequest(
				x = x4, y = iconY, scale = iconScale, sprite = campaign.ui.rangedDefIcon, opacity = 1f
			))
			// TODO Create 12x12 icon of each element?
			// TODO Create 16x16 icon of each reaction/passive skill type
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
				x1 + 10 * scale, region.minY, x2, region.maxY, statsY, statsHeight, 1
			)
			uiRenderer.drawString(
				resources.font, defense.toString(), statsColor, intArrayOf(),
				x2 + 10 * scale, region.minY, x4, region.maxY, statsY, statsHeight, 1
			)
			uiRenderer.drawString(
				resources.font, rangedDefense.toString(), statsColor, intArrayOf(),
				x4 + 10 * scale, region.minY, barX, region.maxY, statsY, statsHeight, 1
			)
		}

		val textColor = srgbToLinear(rgb(238, 204, 127))
		val textHeight = 5 * scale
		uiRenderer?.drawString(
			resources.font, assetCharacter.name, textColor, intArrayOf(),
			x1 + margin / 2, region.minY, x3, region.maxY, startY + margin + 5 * scale, textHeight, 1
		)
		uiRenderer?.drawString(
			resources.font, "Lv${characterState.currentLevel}", textColor, intArrayOf(),
			x3, region.minY, barX, region.maxY, startY + margin + 5 * scale, textHeight, 1
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
			barX + 2 * margin, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 34 / 8, margin * 3 / 2, 1
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
			barX + 2 * margin, region.minY, barX + baseBarWidth, region.maxY,
			startY + margin * 59 / 8, margin * 3 / 2, 1
		)

		renderEquipment(partyIndex, startY + margin - 1, uiRenderer, kimRenderer)
	}

	private fun renderEquipment(partyIndex: Int, startY: Int, uiRenderer: UiRenderer?, kimRenderer: KimRenderer?) {
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
			if (kimRenderer != null && item != null) kimBatch.requests.add(KimRequest(
				x = minX + scale, y = startY + scale, scale = scale.toFloat(), sprite = item.sprite, opacity = 1f
			))
		}

		var hoverLineColor = srgbToLinear(rgb(165, 205, 254))
		var hoverLightColor = srgbToLinear(rgb(25, 68, 118))
		var hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
		val hoveredItem = tab.hoveringItem
		if (hoveredItem != null && hoveredItem.slotIndex < 0 && hoveredItem.characterState == characterState) {
			if (pickedItem != null && !hoveredItem.canInsert(pickedItem.get()!!.item)) {
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
