package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderDescription
import mardek.renderer.util.renderFancyMasteredText
import mardek.state.ingame.menu.SkillsTab
import mardek.state.util.Rectangle
import kotlin.math.max

private const val ANIMATION_PERIOD = 700_000_000L

internal fun renderSkillsTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		if (region.width < 50) return
		val titleTextColor = srgbToLinear(rgb(238, 203, 127))
		val tab = menu.currentTab as SkillsTab

		val characterScale = (region.width * 2 / 5) / (4 * 18)
		var selectedCharacterX = 0
		var selectedCharacterY = 0
		val iconY = region.minY + 10 * characterScale
		val headerY = iconY + 11 * characterScale
		val headerHeight = region.height / 20
		val headerMaxY = headerY + headerHeight - 1
		val descriptionMaxX = region.minX + region.width / 4

		val skillsMinX = descriptionMaxX + region.width / 100
		val skillsMinY = headerMaxY + region.height / 50
		val skillsSpacing = region.height / 20

		val skillsEnablePointsX = descriptionMaxX + region.width / 2
		val skillsMasteryPointsX = descriptionMaxX + region.width / 2 + region.width / 20

		val visibleSkills = tab.determineSkillList(uiContext)
		val selectedSkill = if (visibleSkills.isEmpty()) null else visibleSkills[tab.skillIndex]
		val (assetCharacter, characterState) = context.campaign.allPartyMembers()[tab.partyIndex]!!

		val basicFont2 = context.bundle.getFont(context.content.fonts.basic2.index)

		var spriteIndex = 0
		if ((System.nanoTime() - referenceTime) % ANIMATION_PERIOD >= ANIMATION_PERIOD / 2) spriteIndex += 1

		for ((column, character) in context.campaign.usedPartyMembers()) {
			val x = region.boundX + characterScale * 18 * (column - 4)
			val y = region.minY + 2 * characterScale
			spriteBatch.simple(
				x, y, characterScale,
				character.areaSprites.sprites[spriteIndex].index
			)

			if (column == tab.partyIndex) {
				selectedCharacterX = x - characterScale - 1
				selectedCharacterY = y - characterScale - 1
				imageBatch.rotated(
					x + 8f * characterScale, y - 5f * characterScale, 270f, 0.1f * characterScale,
					context.content.ui.pointer.index, 0, -1
				)
			}
		}

		spriteBatch.simple(
			region.minX + 4 * characterScale, region.minY + 6 * characterScale, characterScale / 2f,
			assetCharacter.characterClass.skillClass.icon.index
		)

		for ((column, icon) in arrayOf(
			context.content.ui.activeStarIcon, context.content.ui.meleeAttackIcon, context.content.ui.meleeDefenseIcon,
			context.content.ui.rangedAttackIcon, context.content.ui.rangedDefenseIcon, context.content.ui.passiveIcon
		).withIndex()) {
			val x = region.minX + region.width / 7f + column * 12 * characterScale
			val width = 8f * characterScale
			val height = width * icon.height / icon.width
			imageBatch.simple(x, iconY.toFloat(), x + width, iconY + height, icon.index)

			if (column == tab.skillTypeIndex && tab.inside) {
				val midX = x + 1f * characterScale
				val midY = iconY + 1f * characterScale
				imageBatch.rotated(
					midX, midY, -45f, characterScale / 16f,
					context.content.ui.pointer.index, 0, -1
				)
			}
		}

		val selectionLowColor = srgbToLinear(rgb(25, 72, 119))
		val selectionBorder = srgbToLinear(rgb(165, 205, 254))
		val selectedCharacterMaxX = selectedCharacterX + 18 * characterScale - 1
		val selectedCharacterMaxY = selectedCharacterY + 18 * characterScale - 1
		gradientWithBorder(
			colorBatch, selectedCharacterX, selectedCharacterY, selectedCharacterMaxX, selectedCharacterMaxY,
			1, 1, selectionBorder,
			selectionLowColor, selectionLowColor, 0
		)

		colorBatch.fill(
			descriptionMaxX, headerY, region.maxX, headerMaxY,
			srgbToLinear(rgb(50, 37, 27))
		)

		run {
			val borderColor = srgbToLinear(rgb(68, 51, 34))
			val borderWidth = max(1, headerHeight / 10)
			colorBatch.fill(
				region.minX, headerY, descriptionMaxX,
				headerY + borderWidth - 1, borderColor,
			)
			colorBatch.fill(
				descriptionMaxX + 1 - borderWidth, headerY,
				descriptionMaxX, headerMaxY, borderColor,
			)

			val slopeSize = 4 * headerHeight / 5
			val slopeX = descriptionMaxX - slopeSize - borderWidth
			val slopeY = headerMaxY + slopeSize + borderWidth
			colorBatch.fillUnaligned(
				slopeX + 1, slopeY + 1,
				descriptionMaxX + 2, headerMaxY + 1,
				descriptionMaxX + 1 - borderWidth, headerMaxY + 1,
				slopeX + 1, 1 + slopeY - borderWidth, borderColor,
			)
			colorBatch.fill(
				region.minX, 1 + slopeY - borderWidth,
				slopeX, slopeY, borderColor,
			)

			val titleBarLowColor = srgbToLinear(rgb(125, 91, 49))
			val titleBarMidColor = srgbToLinear(rgb(85, 60, 34))
			val titleBarHighColor = srgbToLinear(rgb(80, 69, 61))
			val titleBarTopColor = srgbToLinear(rgb(34, 22, 14))
			colorBatch.gradientUnaligned(
				region.minX, 1 + slopeY - borderWidth, titleBarLowColor,
				1 + slopeX, 1 + slopeY - borderWidth, titleBarLowColor,
				1 + descriptionMaxX - borderWidth, 1 + headerMaxY, titleBarMidColor,
				region.minX, 1 + headerMaxY, titleBarMidColor,
			)
			colorBatch.fill(
				region.minX, headerY + borderWidth,
				descriptionMaxX - borderWidth, headerY + 3 * borderWidth - 1, titleBarTopColor,
			)
			colorBatch.gradient(
				region.minX, headerY + 3 * borderWidth,
				region.minX + 3 * borderWidth - 1, headerMaxY,
				titleBarMidColor, titleBarMidColor, titleBarTopColor,
			)
			colorBatch.gradient(
				1 + descriptionMaxX - 4 * borderWidth, headerY + 3 * borderWidth,
				descriptionMaxX - borderWidth, headerMaxY,
				titleBarMidColor, titleBarMidColor, titleBarTopColor,
			)
			colorBatch.gradient(
				region.minX + 3 * borderWidth, headerY + 3 * borderWidth,
				descriptionMaxX - 4 * borderWidth, headerMaxY,
				titleBarMidColor, titleBarMidColor, titleBarHighColor,
			)
		}
		val leftBarColor = srgbToLinear(rgb(93, 75, 43))
		colorBatch.gradient(
			region.minX, headerMaxY + headerHeight, descriptionMaxX, region.maxY,
			leftBarColor, 0, leftBarColor
		)

		if (tab.inside) {
			if (selectedSkill != null) {
				imageBatch.coloredScale(
					region.minX + region.width / 200f,
					region.minY + region.height * 0.4f,
					0.24f * region.width / selectedSkill.skill.element.thinSprite.width,
					selectedSkill.skill.element.thinSprite.index,
					0, rgba(1f, 1f, 1f, 0.075f),
				)
			}
			for ((row, skillEntry) in visibleSkills.withIndex()) {
				val baseY = skillsMinY + row * skillsSpacing
				val skill = skillEntry.skill

				val icon = if (skill is ActiveSkill) skill.element.thickSprite
				else if (skillEntry.isToggled) context.content.ui.skillToggled
				else context.content.ui.skillNotToggled

				var iconScale = 0.03f * region.height / icon.height
				var iconY = baseY.toFloat()
				if (skill is ActiveSkill) {
					iconY -= 0.25f * iconScale * icon.height
					iconScale *= 1.5f
				}
				imageBatch.coloredScale(
					skillsMinX.toFloat(), iconY, iconScale, icon.index,
					0, rgba(1f, 1f, 1f, 0.7f),
				)

				if (tab.inside && row == tab.skillIndex) imageBatch.simpleScale(
					skillsMinX - region.height / 20f, baseY.toFloat(),
					region.height / 2000f, context.content.ui.pointer.index
				)

				if (skillEntry.mastery >= skill.masteryPoints) {
					renderFancyMasteredText(
						context, textBatch,
						skillsMasteryPointsX.toFloat() + region.height / 100f,
						baseY + region.height * 0.03f, region.height / 30f
					)
				}
			}
			val skillTypeDescription = when (tab.skillTypeIndex) {
				0 -> "Action Skills"
				1 -> "Physical Attack Reactions"
				2 -> "Physical Defence Reactions"
				3 -> "Magical Attack Reactions"
				4 -> "Magical Defence Reactions"
				5 -> "Passive Skills"
				else -> throw IllegalStateException("Unexpected skill type index ${tab.skillTypeIndex}")
			}
			textBatch.drawString(
				skillTypeDescription, region.minX + region.width / 7 + 75 * characterScale,
				selectedCharacterY + 7 * characterScale, 4 * characterScale, basicFont2,
				titleTextColor, TextAlignment.RIGHT
			)
		}

		val selectedSkillText = if (tab.inside) selectedSkill?.skill?.name ?: "No skill"
		else assetCharacter.characterClass.skillClass.name
		textBatch.drawShadowedString(
			selectedSkillText, region.minX + region.height * 0.015f, headerY + 1.33f * headerHeight,
			headerHeight * 0.66f, basicFont2, titleTextColor, 0, 0f,
			srgbToLinear(rgb(37, 21, 10)), headerHeight * 0.05f,
			headerHeight * 0.05f, TextAlignment.LEFT,
		)

		for ((minX, maxX, text) in arrayOf(
			Triple(skillsMinX, skillsEnablePointsX, "Ability"),
			Triple(skillsMinX, skillsEnablePointsX, if (tab.skillTypeIndex == 0) "MP" else "RP"),
			Triple(skillsMasteryPointsX, region.maxX, "AP")
		)) {
			textBatch.drawString(
				text, if (text.startsWith("A")) minX else maxX, headerMaxY - headerHeight / 5,
				headerHeight * 3 / 5, basicFont2, titleTextColor,
				if (text.startsWith("A")) TextAlignment.LEFT else TextAlignment.RIGHT,
			)
		}

		var lineY = headerMaxY + 2f * headerHeight

		fun drawDescriptionLine(line: String) {
			val shadowOffset = region.width * 0.001f
			textBatch.drawShadowedString(
				line, region.minX + region.width * 0.01f, lineY, region.width * 0.015f,
				basicFont2, srgbToLinear(rgb(200, 185, 135)), 0, 0f,
				srgbToLinear(rgb(53, 42, 27)),
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			@Suppress("AssignedValueIsNeverRead")
			lineY += region.width * 0.025f
		}

		val description = if (tab.inside) selectedSkill?.skill?.description ?: ""
		else assetCharacter.characterClass.skillClass.description
		renderDescription(description, 22, ::drawDescriptionLine)

		textBatch.drawString(
			assetCharacter.name, region.minX + region.width * 0.01f,
			region.maxY - region.height * 0.1f, region.height * 0.03f, basicFont2,
			titleTextColor, TextAlignment.LEFT,
		)

		val resourceX = region.minX + region.height / 18
		val resourceMinY = region.maxY - region.height / 18
		val resourceMaxY = region.maxY - region.height / 30

		val resourceText = if (tab.skillTypeIndex == 0) "MP" else "RP"
		textBatch.drawString(
			resourceText, resourceX - 2, resourceMaxY, resourceMaxY - resourceMinY,
			basicFont2, titleTextColor, TextAlignment.RIGHT
		)

		val maxResourceValue = if (tab.skillTypeIndex == 0) {
			characterState.determineMaxMana(assetCharacter.baseStats, characterState.activeStatusEffects)
		} else characterState.determineSkillEnablePoints()
		val currentResourceValue = if (tab.skillTypeIndex == 0) characterState.currentMana
		else maxResourceValue - visibleSkills.sumOf { skillEntry ->
			if (skillEntry.isToggled) {
				if (skillEntry.skill is PassiveSkill) (skillEntry.skill as PassiveSkill).enablePoints
				else (skillEntry.skill as ReactionSkill).enablePoints
			} else 0
		}

		val resourceType = if (tab.skillTypeIndex == 0) ResourceType.Mana else ResourceType.SkillEnable
		val resourceRenderer = ResourceBarRenderer(
			context, resourceType, Rectangle(
				resourceX, resourceMinY, descriptionMaxX - resourceX, resourceMaxY - resourceMinY
			), colorBatch, textBatch
		)

		resourceRenderer.renderBar(currentResourceValue, maxResourceValue)
		resourceRenderer.renderTextOverBar(currentResourceValue, maxResourceValue)

		if (tab.inside) {
			for ((row, skillEntry) in visibleSkills.withIndex()) {
				val baseY = skillsMinY + row * skillsSpacing
				val skill = skillEntry.skill
				val ovalX1 = skillsMinX + region.height / 50
				val ovalX2 = skillsEnablePointsX + region.height / 300
				val rowMaxY = baseY + region.height / 33
				val ovalRadius = 0.5f * (1f + rowMaxY - baseY)
				val leftBackgroundColor = srgbToLinear(rgba(50, 35, 25, 200))
				val rightBackgroundColor = srgbToLinear(rgba(75, 50, 30, 200))
				ovalBatch.aliased(
					(ovalX1 - ovalRadius - 1f).toInt(), baseY, ovalX2, rowMaxY,
					ovalX1.toFloat(), baseY + ovalRadius, ovalRadius, ovalRadius,
					leftBackgroundColor
				)
				colorBatch.gradient(
					ovalX1 + 1, baseY, ovalX2 - 1, rowMaxY,
					leftBackgroundColor, rightBackgroundColor, leftBackgroundColor
				)
				ovalBatch.aliased(
					ovalX2, baseY, (ovalX2 + ovalRadius + 1f).toInt(), rowMaxY,
					ovalX2.toFloat(), baseY + ovalRadius, ovalRadius, ovalRadius,
					rightBackgroundColor
				)

				var rowTextColor = titleTextColor
				if (selectedSkill != null && selectedSkill.skill === skill) {
					rowTextColor = srgbToLinear(rgb(240, 224, 185))
				}
				textBatch.drawString(
					skill.name, skillsMinX + region.height / 20, baseY + region.height / 33,
					region.height / 35, basicFont2, rowTextColor
				)

				val rp = when (skill) {
					is ActiveSkill -> skill.manaCost
					is PassiveSkill -> skill.enablePoints
					else -> (skill as ReactionSkill).enablePoints
				}

				val shadowColor = srgbToLinear(rgb(61, 35, 18))
				val shadowOffset = region.height * 0.002f
				textBatch.drawShadowedString(
					rp.toString(), skillsEnablePointsX - region.height * 0.01f,
					baseY + region.height * 0.028f, region.height * 0.027f,
					basicFont2, rowTextColor, 0, 0f,
					shadowColor, shadowOffset, shadowOffset, TextAlignment.RIGHT,
				)

				if (skillEntry.mastery < skill.masteryPoints) {
					val minMasteryX = skillsMasteryPointsX - region.height / 200
					val maxMasteryX = skillsMasteryPointsX + region.height / 4 + region.height / 50
					val masteryRegion = Rectangle(
						minMasteryX, baseY + region.height / 200,
						maxMasteryX - minMasteryX, region.height / 40
					)
					val masteryBar = ResourceBarRenderer(
						context, ResourceType.SkillMastery, masteryRegion,
						colorBatch, textBatch
					)
					masteryBar.renderBar(skillEntry.mastery, skill.masteryPoints)
					masteryBar.renderTextOverBar(skillEntry.mastery, skill.masteryPoints)
				}
			}
		}
	}
}
