package mardek.renderer.menu.skills

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.renderer.menu.MenuRenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.renderer.util.gradientWithBorder
import mardek.renderer.util.renderDescription
import mardek.state.ingame.menu.SkillsTab
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

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
		val assetCharacter = context.campaign.characterSelection.party[tab.partyIndex]!!

		// TODO Figure out the font
		val unknownFont = context.bundle.getFont(context.content.fonts.basic1.index)



		var spriteIndex = 0
		if ((System.nanoTime() - referenceTime) % ANIMATION_PERIOD >= ANIMATION_PERIOD / 2) spriteIndex += 1

		for ((column, character) in context.campaign.characterSelection.party.withIndex()) {
			if (character == null) continue

			val x = region.boundX + characterScale * 18 * (column - 4)
			val y = region.minY + 2 * characterScale
			spriteBatch.simple(
				x, y, characterScale,
				character.areaSprites.sprites[spriteIndex].index
			)

			if (column == tab.partyIndex) {
				selectedCharacterX = x - characterScale - 1
				selectedCharacterY = y - characterScale - 1
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
			val x = region.minX + region.width / 7 + column * 12 * characterScale
			val width = 8 * characterScale
			val height = width * icon.height / icon.width
			imageBatch.simple(x, iconY, x + width - 1, iconY + height - 1, icon.index)

			// TODO Diagonal pointer
			if (column == tab.skillTypeIndex && tab.inside) {
				val midX = x + 1f * characterScale
				val midY = iconY + 1f * characterScale
				imageBatch.rotated(
					midX, midY, -45f, characterScale / 16f,
					context.content.ui.pointer.index
				)
			}
//			if (column == tab.skillTypeIndex && tab.inside) addKimRequest(KimRequest(
//				x = x - 4 * characterScale, y = iconY - 4 * characterScale,
//				scale = characterScale / 16f, sprite = context.content.ui.diagonalPointer
//			))
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

		val titleBarLowColor = srgbToLinear(rgb(125, 91, 49))
		val titleBarHighColor = srgbToLinear(rgb(80, 69, 61))
		colorBatch.gradient(
			region.minX, headerY, descriptionMaxX, headerMaxY + headerHeight,
			titleBarLowColor, titleBarLowColor, titleBarHighColor
		)

		val leftBarColor = srgbToLinear(rgba(93, 75, 43, 200))
		colorBatch.gradient(
			region.minX, headerMaxY + headerHeight, descriptionMaxX, region.maxY,
			leftBarColor, 0, leftBarColor
		)

//		if (selectedSkill != null && tab.inside) addKimRequest(KimRequest(
//			x = region.minX + region.width / 200,
//			y = region.minY + region.height * 2 / 5,
//			sprite = selectedSkill.skill.element.sprite,
//			scale = region.width / 500f, opacity = 0.01f
//		))

		for ((row, skillEntry) in visibleSkills.withIndex()) {
			val baseY = skillsMinY + row * skillsSpacing
			val skill = skillEntry.skill

			// TODO Element sprites
//			if (skill is ActiveSkill) addKimRequest(KimRequest(
//				x = skillsMinX, y = baseY, scale = region.height / 3000f, sprite = skill.element.sprite
//			)) else {
//				val icon = if (skillEntry.isToggled) context.content.ui.skillToggled else context.content.ui.skillNotToggled
//				addKimRequest(KimRequest(
//					x = skillsMinX, y = baseY, scale = region.height / 3000f, sprite = icon
//				))
//			}

			if (tab.inside && row == tab.skillIndex) imageBatch.simpleScale(
				skillsMinX - region.height / 20, baseY,
				region.height / 3000f, context.content.ui.pointer.index
			)

			if (skillEntry.mastery >= skill.masteryPoints) {
				// TODO MASTERED
				textBatch.drawString(
					"Mastered", skillsMasteryPointsX.toFloat(), baseY + region.height / 50f, region.height / 20f,
					unknownFont, rgb(0, 0, 0)
				)
//				addKimRequest(KimRequest(
//					x = skillsMasteryPointsX, y = baseY, scale = region.height / 500f,
//					sprite = context.content.ui.mastered
//				))
			}
		}

		if (tab.inside) {
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
				selectedCharacterY + 7 * characterScale, 4 * characterScale, unknownFont,
				titleTextColor, TextAlignment.RIGHT
			)
//			context.uiRenderer.drawString(
//				context.resources.font, skillTypeDescription, titleTextColor, intArrayOf(),
//				region.minX, region.minY, , headerY,
//				selectedCharacterY + 7 * characterScale, 4 * characterScale, 1, TextAlignment.RIGHT
//			)
		}

		val selectedSkillText = if (tab.inside) selectedSkill?.skill?.name ?: "No skill"
		else assetCharacter.characterClass.skillClass.name
		textBatch.drawString(
			selectedSkillText, region.minX + region.width / 200, headerY + 4 * headerHeight / 3,
			headerHeight * 2 / 3, unknownFont, titleTextColor
		)

		for ((minX, maxX, text) in arrayOf(
			Triple(skillsMinX, skillsEnablePointsX, "Ability"),
			Triple(skillsMinX, skillsEnablePointsX, if (tab.skillTypeIndex == 0) "MP" else "RP"),
			Triple(skillsMasteryPointsX, region.maxX, "AP")
		)) {
			textBatch.drawString(
				text, if (text.startsWith("A")) minX else maxX, headerMaxY - headerHeight / 5,
				headerHeight * 3 / 5, unknownFont, titleTextColor,
				if (text.startsWith("A")) TextAlignment.LEFT else TextAlignment.RIGHT,
			)
		}

		var lineY = headerMaxY + 2 * headerHeight
		fun drawDescriptionLine(line: String) {
			textBatch.drawString(
				line, region.minX + region.width / 100, lineY, region.width / 60,
				unknownFont, srgbToLinear(rgb(200, 185, 135))
			)
			lineY += region.width / 35
		}

		val description = if (tab.inside) selectedSkill?.skill?.description ?: ""
		else assetCharacter.characterClass.skillClass.description
		renderDescription(description, 25, ::drawDescriptionLine)

		textBatch.drawString(
			assetCharacter.name, region.minX + region.width / 100, region.maxY - region.height / 10,
			region.height / 35, unknownFont, titleTextColor
		)

		val resourceX = region.minX + region.width / 25
		val resourceMinY = region.maxY - region.height / 18
		val resourceMaxY = region.maxY - region.height / 30

		val resourceText = if (tab.skillTypeIndex == 0) "MP" else "RP"
		textBatch.drawString(
			resourceText, resourceX - 2, resourceMaxY, resourceMaxY - resourceMinY,
			unknownFont, titleTextColor, TextAlignment.RIGHT
		)

		val characterState = context.campaign.characterStates[assetCharacter]!!
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

		for ((row, skillEntry) in visibleSkills.withIndex()) {
			val baseY = skillsMinY + row * skillsSpacing
			val skill = skillEntry.skill
			colorBatch.fill(
				skillsMinX, baseY, skillsEnablePointsX + region.height / 100,
				baseY + region.height / 33, rgba(0, 0, 0, 100)
			)
			textBatch.drawString(
				skill.name, skillsMinX + region.height / 20, baseY + region.height / 33, region.height / 35,
				unknownFont, titleTextColor
			)

			val rp = when (skill) {
				is ActiveSkill -> skill.manaCost
				is PassiveSkill -> skill.enablePoints
				else -> (skill as ReactionSkill).enablePoints
			}
			textBatch.drawString(
				rp.toString(), skillsEnablePointsX - region.height / 100, baseY + region.height / 33,
				region.height / 35, unknownFont, titleTextColor, TextAlignment.RIGHT,
			)

			val masteryBarColor = srgbToLinear(rgb(59, 38, 29))
			val masteryFilledBarColor = srgbToLinear(rgb(159, 31, 23))
			if (skillEntry.mastery < skill.masteryPoints) {
				val minMasteryX = skillsMasteryPointsX + region.height / 100
				val maxMasteryX = region.maxX - region.height / 50
				val filledX = minMasteryX + ((maxMasteryX - minMasteryX) * (skillEntry.mastery.toFloat() / skill.masteryPoints)).roundToInt()
				colorBatch.fill(
					minMasteryX, baseY + region.height / 200, filledX - 1,
					baseY + region.height / 40, masteryFilledBarColor
				)
				colorBatch.fill(
					filledX, baseY + region.height / 200, maxMasteryX,
					baseY + region.height / 40, masteryBarColor
				)
				val masteryTextColor = srgbToLinear(rgb(253, 94, 94))
				val masterySplitX = (maxMasteryX + minMasteryX) / 2
				textBatch.drawString(
					skillEntry.mastery.toString(), masterySplitX, baseY + region.height / 30,
					region.height / 28, unknownFont, masteryTextColor, TextAlignment.RIGHT
				)
				textBatch.drawString(
					skill.masteryPoints.toString(), masterySplitX + region.height / 33, baseY + region.height / 33,
					region.height / 35, unknownFont, masteryTextColor
				)
			}
		}
	}
}
