package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.gradientWithBorder
import mardek.state.UsedPartyMember
import mardek.state.ingame.menu.inventory.EquipmentRowRenderInfo
import mardek.state.ingame.menu.inventory.EquipmentSlotReference
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.util.Rectangle
import java.lang.Math.toIntExact
import kotlin.math.abs

internal const val CHARACTER_BAR_HEIGHT = 23
internal const val EQUIPMENT_SLOT_SIZE = 20

private val EQUAL_STAT_COLOR = srgbToLinear(rgb(0, 220, 255))
private val INCREASED_STAT_COLOR = srgbToLinear(rgb(152, 255, 0))
private val DECREASED_STAT_COLOR = srgbToLinear(rgb(255, 85, 85))

internal fun renderCharacterBars(
	inventoryContext: InventoryRenderContext,
	interaction: InventoryInteractionState,
	owners: List<UsedPartyMember>,
	startX: Int, startY: Int, maxX: Int, scale: Int
): Collection<EquipmentRowRenderInfo> {
	val renderInfo = mutableListOf<EquipmentRowRenderInfo>()
	for ((index, character, characterState) in owners) {
		renderInfo.add(renderCharacterBar(
			inventoryContext, startX, startY + index * scale * CHARACTER_BAR_HEIGHT,
			maxX, scale, interaction, index == interaction.partyIndex, character, characterState
		))
	}
	return renderInfo
}

private fun renderCharacterBar(
	inventoryContext: InventoryRenderContext, startX: Int, startY: Int, maxX: Int, scale: Int,
	interaction: InventoryInteractionState, isSelected: Boolean,
	character: PlayableCharacter, characterState: CharacterState,
): EquipmentRowRenderInfo {
	inventoryContext.run {
		val barHeight = scale * CHARACTER_BAR_HEIGHT

		run {
			val lineColor = srgbToLinear(rgb(165, 151, 110))
			val outerLightColor = srgbToLinear(rgb(89, 72, 42))
			val outerRightColor = srgbToLinear(rgb(104, 80, 47))
			val outerDarkColor = srgbToLinear(rgb(39, 26, 16))
			val maxY = startY + barHeight - 1
			gradientWithBorder(
				colorBatch, startX, startY, maxX, maxY, 2, 2,
				lineColor, outerLightColor, outerRightColor, outerDarkColor
			)
		}

		val margin = 3 * scale
		run {
			val innerDarkColor = srgbToLinear(rgb(113, 88, 58))
			val innerLightColor = srgbToLinear(rgb(119, 105, 91))
			colorBatch.gradient(
				startX + margin, startY + margin,
				maxX - margin, startY + barHeight * 2 / 3,
				innerDarkColor, innerDarkColor, innerLightColor
			)
		}

		val consumableRegion = Rectangle(
			startX + 2 * scale, startY + 2 * scale, 19 * scale, 19 * scale
		)
		val consumable = context.campaign.cursorItemStack?.item?.consumable
		if (consumable != null && consumable.isPositive()) {
			val blinkPeriod = 1_250_000_000L
			val relativeTime = (System.nanoTime() - referenceTime) % blinkPeriod
			val blinkIntensity = (abs(blinkPeriod / 2 - relativeTime) * 2.0 / blinkPeriod).toFloat()
			colorBatch.fill(
				consumableRegion.minX, consumableRegion.minY,
				consumableRegion.maxX, consumableRegion.maxY,
				srgbToLinear(rgba(0.65f, 0.55f, 0.2f, 0.1f + 0.4f * blinkIntensity))
			)
		}

		val characterX = startX + margin + margin / 2
		val characterY = startY + margin + scale
		run {
			var spriteIndex = 0
			val passedTime = System.nanoTime() - referenceTime
			val animationPeriod = 700_000_000L
			if (passedTime % animationPeriod >= animationPeriod / 2) spriteIndex = 1

			spriteBatch.simple(
				characterX, characterY, scale,
				character.areaSprites.sprites[spriteIndex].index
			)
		}

		if (isSelected) {
			val selectedColor = rgba(0, 30, 150, 100)
			colorBatch.fill(startX, startY, maxX, startY + barHeight - 1, selectedColor)
			imageBatch.simpleScale(
				startX - 9f * scale, startY + 8f * scale,
				scale * 0.15f, context.content.ui.pointer.index
			)
		}

		val x1 = characterX + 16 * scale + margin / 2

		val elementColor = srgbToLinear(character.element.color)
		val lowElementColor = changeAlpha(elementColor, 150)
		val highElementColor = changeAlpha(elementColor, 50)
		colorBatch.gradient(
			x1, startY + margin, characterX + 80 * scale,
			startY + margin + 6 * scale,
			lowElementColor, 0, highElementColor
		)

		val x2 = x1 + 25 * scale
		val x3 = x2 + 16 * scale
		val x4 = x3 + 8 * scale

		run {
			val iconY = startY + barHeight / 2 + margin / 2
			val iconScale = scale / 8f
			imageBatch.simpleScale(
				x1.toFloat(), iconY.toFloat(), iconScale,
				context.content.ui.attackIcon.index
			)
			imageBatch.simpleScale(
				x2.toFloat(), iconY.toFloat(), iconScale,
				context.content.ui.defIcon.index
			)
			imageBatch.simpleScale(
				x4.toFloat(), iconY.toFloat(), iconScale,
				context.content.ui.rangedDefIcon.index
			)
			val numEffects = characterState.activeStatusEffects.size
			if (numEffects > 0) {
				val passedTime = System.nanoTime() - referenceTime
				val period = 250_000_000L
				val index = (passedTime % (period * numEffects)) / period
				val sprite = characterState.activeStatusEffects.toList()[toIntExact(index)].icon
				imageBatch.simpleScale(
					x4 + 9f * scale, startY + 2f * scale,
					8f * scale / sprite.height, sprite.index
				)
			}
		}

		val barX = x4 + 20 * scale
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val baseShadowColor = srgbToLinear(rgb(61, 35, 18))
		val selectedTextColor = srgbToLinear(rgb(164, 204, 253))
		val selectedShadowColor = srgbToLinear(rgb(51, 0, 204))

		run {
			val statsY = startY + barHeight - margin * 1.33f
			var statsColor = baseTextColor
			var shadowColor = baseShadowColor
			if (isSelected) {
				statsColor = selectedTextColor
				shadowColor = selectedShadowColor
			}
			val shadowOffset = 0.5f * scale
			val statsHeight = 5f * scale

			var attack = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.Attack
			)
			var overrideAttackColor: Int? = null
			var defense = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.MeleeDefense
			)
			var overrideDefenseColor: Int? = null
			var rangedDefense = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.RangedDefense
			)
			var overrideRangedDefenceColor: Int? = null

			val hoveredItem = interaction.hoveredSlot?.get()?.item
			if (hoveredItem != null) {
				val equipment = hoveredItem.equipment
				if (equipment != null) {
					val firstSlot = character.characterClass.equipmentSlots.find {
						it.isAllowed(hoveredItem, character)
					}
					if (firstSlot != null) {
						val itemToReplace = characterState.equipment[firstSlot]
						var changedAttack = false
						var changedDefense = false
						var changedRangedDefense = false
						val oldAttack = attack
						val oldDefense = defense
						val oldRangedDefense = rangedDefense

						if (itemToReplace != null) {
							for (modifier in itemToReplace.equipment!!.stats) {
								if (modifier.stat == CombatStat.Attack) {
									attack -= modifier.adder
									changedAttack = true
								}
								if (modifier.stat == CombatStat.MeleeDefense) {
									defense -= modifier.adder
									changedDefense = true
								}
								if (modifier.stat == CombatStat.RangedDefense) {
									rangedDefense -= modifier.adder
									changedRangedDefense = true
								}
							}
						}
						for (modifier in equipment.stats) {
							if (modifier.stat == CombatStat.Attack) {
								attack += modifier.adder
								changedAttack = true
							}
							if (modifier.stat == CombatStat.MeleeDefense) {
								defense += modifier.adder
								changedDefense = true
							}
							if (modifier.stat == CombatStat.RangedDefense) {
								rangedDefense += modifier.adder
								changedRangedDefense = true
							}
						}

						fun chooseColor(oldValue: Int, newValue: Int) = if (newValue > oldValue) INCREASED_STAT_COLOR
						else if (newValue < oldValue) DECREASED_STAT_COLOR else EQUAL_STAT_COLOR

						if (changedAttack) overrideAttackColor = chooseColor(oldAttack, attack)
						if (changedDefense) overrideDefenseColor = chooseColor(oldDefense, defense)
						if (changedRangedDefense) {
							overrideRangedDefenceColor = chooseColor(oldRangedDefense, rangedDefense)
						}
					}
				}
			}

			textBatch.drawShadowedString(
				attack.toString(), x1 + 10f * scale, statsY, statsHeight, font,
				overrideAttackColor ?: statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
			textBatch.drawShadowedString(
				defense.toString(), x2 + 10f * scale, statsY, statsHeight, font,
				overrideDefenseColor ?: statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
			textBatch.drawShadowedString(
				rangedDefense.toString(), x4 + 10f * scale, statsY, statsHeight, font,
				overrideRangedDefenceColor ?: statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
		}

		val textHeight = 5f * scale
		val shadowOffset = 0.5f * scale
		run {
			var textColor = baseTextColor
			var shadowColor = baseShadowColor
			if (isSelected) {
				textColor = selectedTextColor
				shadowColor = selectedShadowColor
			}
			textBatch.drawShadowedString(
				character.name, x1 + margin * 0.5f, startY + margin + 5.5f * scale, textHeight,
				font, textColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
		}

		textBatch.drawShadowedString(
			"Lv${characterState.currentLevel}", x3.toFloat(), startY + margin + 5.5f * scale,
			textHeight, font, baseTextColor, 0, 0f,
			baseShadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
		)

		val baseBarWidth = 40 * scale
		val barsHeight = margin

		val healthRenderer = ResourceBarRenderer(
			context, ResourceType.Health, Rectangle(
				barX, startY + margin * 13 / 9, baseBarWidth, barsHeight
			), colorBatch, textBatch
		)
		val maxHealth = characterState.determineMaxHealth(character.baseStats, characterState.activeStatusEffects)
		healthRenderer.renderBar(characterState.currentHealth, maxHealth)
		healthRenderer.renderTextBelowBar(characterState.currentHealth, maxHealth)

		val manaRenderer = ResourceBarRenderer(context, ResourceType.Mana, Rectangle(
			barX, startY + margin * 37 / 8, baseBarWidth, barsHeight
		), colorBatch, textBatch)
		val maxMana = characterState.determineMaxMana(character.baseStats, characterState.activeStatusEffects)
		manaRenderer.renderBar(characterState.currentMana, maxMana)
		manaRenderer.renderTextBelowBar(characterState.currentMana, maxMana)

		return renderEquipment(
			this, interaction,
			character, characterState,
			startY + margin - 1, maxX, scale, consumableRegion,
		)
	}
}

private fun renderEquipment(
	inventoryContext: InventoryRenderContext,
	interaction: InventoryInteractionState,
	owner: PlayableCharacter,
	ownerState: CharacterState,
	startY: Int, maxX: Int, scale: Int,
	consumableRegion: Rectangle,
) : EquipmentRowRenderInfo {
	inventoryContext.run {
		val startX = maxX - 6 * scale * EQUIPMENT_SLOT_SIZE
		val largeSlotSize = scale * EQUIPMENT_SLOT_SIZE
		val slotSize = scale * SIMPLE_SLOT_SIZE

		val pickedItem = context.campaign.cursorItemStack
		val lineColor = srgbToLinear(rgb(208, 193, 142))
		for ((column, slot) in owner.characterClass.equipmentSlots.withIndex()) {
			val minX = startX + column * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			colorBatch.fill(minX, startY, maxX, startY, lineColor)
			colorBatch.fill(minX, maxY, maxX, maxY, lineColor)
			colorBatch.fill(minX, startY, minX, maxY, lineColor)
			colorBatch.fill(maxX, startY, maxX, maxY, lineColor)
			val item = ownerState.equipment[slot]
			if (item != null) spriteBatch.simple(
				minX + scale, startY + scale,
				scale.toFloat(), item.sprite.index
			)
		}

		var hoverLineColor = srgbToLinear(rgb(165, 205, 254))
		var hoverLightColor = srgbToLinear(rgb(25, 68, 118))
		var hoverDarkColor = srgbToLinear(rgb(64, 43, 36))
		val hoveredSlot = interaction.hoveredSlot
		if (hoveredSlot is EquipmentSlotReference && hoveredSlot.equipment === ownerState.equipment) {
			if (pickedItem != null && !hoveredSlot.slot.isAllowed(pickedItem.item, owner)) {
				hoverLineColor = srgbToLinear(rgb(255, 162, 162))
				hoverDarkColor = srgbToLinear(rgb(134, 107, 90))
				hoverLightColor = srgbToLinear(rgb(134, 30, 20))
			}
			val slotIndex = owner.characterClass.equipmentSlots.indexOf(hoveredSlot.slot)
			val minX = startX + slotIndex * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			gradientWithBorder(
				colorBatch, minX, startY, maxX, maxY, 1, 1,
				hoverLineColor, hoverLightColor, hoverLightColor, hoverDarkColor
			)
		}

		return EquipmentRowRenderInfo(
			startX = startX,
			startY = startY,
			slotSize = slotSize,
			slotSpacing = largeSlotSize,
			numSlots = owner.characterClass.equipmentSlots.size,
			consumableRegion = consumableRegion,
			owner = owner,
			ownerState = ownerState,
		)
	}
}
