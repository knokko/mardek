package mardek.renderer.menu.inventory

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.gradientWithBorder
import mardek.state.ingame.menu.InventoryTab
import mardek.state.util.Rectangle
import java.lang.Math.toIntExact

internal const val CHARACTER_BAR_HEIGHT = 23
internal const val EQUIPMENT_SLOT_SIZE = 20

internal fun renderCharacterBars(menuContext: MenuRenderContext, startX: Int, startY: Int, maxX: Int, scale: Int) {
	for ((index, character, characterState) in menuContext.state.usedPartyMembers()) {
		renderCharacterBar(
			menuContext, startX, startY + index * scale * CHARACTER_BAR_HEIGHT,
			maxX, scale, index, character, characterState
		)
	}
}

private fun renderCharacterBar(
	menuContext: MenuRenderContext, startX: Int, startY: Int, maxX: Int, scale: Int,
	partyIndex: Int, character: PlayableCharacter, characterState: CharacterState,
) {
	menuContext.run {
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

		val tab = menu.currentTab as InventoryTab

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

		if (partyIndex == tab.partyIndex) {
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
			if (tab.partyIndex == partyIndex) {
				statsColor = selectedTextColor
				shadowColor = selectedShadowColor
			}
			val shadowOffset = 0.5f * scale
			val statsHeight = 5f * scale

			var attack = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.Attack
			)
			var defense = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.MeleeDefense
			)
			var rangedDefense = characterState.computeStatValue(
				character.baseStats, characterState.activeStatusEffects, CombatStat.RangedDefense
			)

			val pickedItem = tab.pickedUpItem
			if (pickedItem != null && pickedItem.getEquipmentType() != null && pickedItem.assetCharacter === character) {
				val equipment = pickedItem.get()?.item?.equipment
				if (equipment != null) {
					for (modifier in equipment.stats) {
						if (modifier.stat == CombatStat.Attack) attack -= modifier.adder
						if (modifier.stat == CombatStat.MeleeDefense) defense -= modifier.adder
						if (modifier.stat == CombatStat.RangedDefense) rangedDefense -= modifier.adder
					}
				}
			}

			textBatch.drawShadowedString(
				attack.toString(), x1 + 10f * scale, statsY,
				statsHeight, font, statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
			textBatch.drawShadowedString(
				defense.toString(), x2 + 10f * scale, statsY,
				statsHeight, font, statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
			textBatch.drawShadowedString(
				rangedDefense.toString(), x4 + 10f * scale, statsY,
				statsHeight, font, statsColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
			)
		}

		val textHeight = 5f * scale
		val shadowOffset = 0.5f * scale
		run {
			var textColor = baseTextColor
			var shadowColor = baseShadowColor
			if (tab.partyIndex == partyIndex) {
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

		renderEquipment(this, partyIndex, characterState, startY + margin - 1, maxX, scale)
	}
}

private fun renderEquipment(
	menuContext: MenuRenderContext, partyIndex: Int, characterState: CharacterState,
	startY: Int, maxX: Int, scale: Int,
) {
	menuContext.run {
		val tab = menu.currentTab as InventoryTab

		val startX = maxX - 6 * scale * EQUIPMENT_SLOT_SIZE
		val largeSlotSize = scale * EQUIPMENT_SLOT_SIZE
		val slotSize = scale * SIMPLE_SLOT_SIZE

		if (partyIndex == 0) {
			tab.renderEquipmentStartX = startX
			tab.renderEquipmentStartY = startY
			tab.renderEquipmentSlotSize = slotSize
			tab.renderEquipmentSlotSpacing = largeSlotSize
			tab.renderEquipmentCharacterSpacing = scale * CHARACTER_BAR_HEIGHT
		}

		val equipment = characterState.equipment

		val pickedItem = tab.pickedUpItem
		val lineColor = srgbToLinear(rgb(208, 193, 142))
		for ((column, item) in equipment.withIndex()) {
			val minX = startX + column * largeSlotSize
			val maxX = minX + slotSize - 1
			val maxY = startY + slotSize - 1
			colorBatch.fill(minX, startY, maxX, startY, lineColor)
			colorBatch.fill(minX, maxY, maxX, maxY, lineColor)
			colorBatch.fill(minX, startY, minX, maxY, lineColor)
			colorBatch.fill(maxX, startY, maxX, maxY, lineColor)
			if (pickedItem != null && (-pickedItem.slotIndex - 1) == column && pickedItem.characterState == characterState) continue
			if (item != null) spriteBatch.simple(
				minX + scale, startY + scale,
				scale.toFloat(), item.sprite.index
			)
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
			gradientWithBorder(
				colorBatch, minX, startY, maxX, maxY, 1, 1,
				hoverLineColor, hoverLightColor, hoverLightColor, hoverDarkColor
			)
		}
	}
}
