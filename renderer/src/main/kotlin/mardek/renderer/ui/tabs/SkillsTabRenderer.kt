package mardek.renderer.ui.tabs

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.content.Content
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.SkillsTab
import mardek.state.title.AbsoluteRectangle
import kotlin.math.roundToInt

private val referenceTime = System.nanoTime()
private const val ANIMATION_PERIOD = 700_000_000L

class SkillsTabRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,

	private val tab: SkillsTab,
	private val region: AbsoluteRectangle,
	private val state: CampaignState,
	private val content: Content,
	private val resources: SharedResources,
) : TabRenderer() {

	private val characterScale = (region.width * 2 / 5) / (4 * 18)
	private var selectedCharacterX = 0
	private var selectedCharacterY = 0
	private val iconY = region.minY + 10 * characterScale
	private val headerY = iconY + 11 * characterScale
	private val headerHeight = region.height / 20
	private val headerMaxY = headerY + headerHeight - 1
	private val descriptionMaxX = region.minX + region.width / 4

	private val skillsMinX = descriptionMaxX + region.width / 100
	private val skillsMinY = headerMaxY + region.height / 50
	private val skillsSpacing = region.height / 20

	private val skillsEnablePointsX = descriptionMaxX + region.width / 2
	private val skillsMasteryPointsX = descriptionMaxX + region.width / 2 + region.width / 20

	private val visibleSkills = tab.determineSkillList()
	private val selectedSkill = if (visibleSkills.isEmpty()) null else visibleSkills[tab.skillIndex]

	private lateinit var kim1Batch: KimBatch
	private lateinit var kim2Batch: KimBatch

	private val assetCharacter = state.characterSelection.party[tab.partyIndex]!!

	private fun addKimRequest(request: KimRequest) {
		if (request.sprite.version == 1) kim1Batch.requests.add(request)
		else kim2Batch.requests.add(request)
	}

	override fun beforeRendering() {
		this.kim1Batch = resources.kim1Renderer.startBatch()
		this.kim2Batch = resources.kim2Renderer.startBatch()

		renderUpperIcons()
		renderSkillIcons()
	}

	private fun renderSkillIcons() {
		for ((row, skillEntry) in visibleSkills.withIndex()) {
			val baseY = skillsMinY + row * skillsSpacing
			val skill = skillEntry.skill

			if (skill is ActiveSkill) addKimRequest(KimRequest(
				x = skillsMinX, y = baseY, scale = region.height / 3000f, sprite = skill.element.sprite, opacity = 1f
			)) else {
				val icon = if (skillEntry.isToggled) content.ui.skillToggled else content.ui.skillNotToggled
				addKimRequest(KimRequest(
					x = skillsMinX, y = baseY, scale = region.height / 3000f, sprite = icon, opacity = 1f
				))
			}

			if (tab.inside && row == tab.skillIndex) addKimRequest(
				KimRequest(
				x = skillsMinX - region.height / 20, y = baseY, scale = region.height / 3000f,
				sprite = content.ui.horizontalPointer, opacity = 1f
			)
			)

			if (skillEntry.mastery >= skill.masteryPoints) addKimRequest(
				KimRequest(
				x = skillsMasteryPointsX, y = baseY, scale = region.height / 500f,
				sprite = content.ui.mastered, opacity = 1f
			)
			)
		}
	}

	private fun renderUpperIcons() {
		var spriteIndex = 0
		if ((System.nanoTime() - referenceTime) % ANIMATION_PERIOD >= ANIMATION_PERIOD / 2) spriteIndex += 1

		for ((column, character) in state.characterSelection.party.withIndex()) {
			if (character == null) continue

			val x = region.boundX + characterScale * 18 * (column - 4)
			val y = region.minY + 2 * characterScale
			addKimRequest(KimRequest(
				x = x, y = y, scale = characterScale.toFloat(),
				sprite = character.areaSprites.sprites[spriteIndex], opacity = 1f
			))

			if (column == tab.partyIndex) {
				selectedCharacterX = x - characterScale - 1
				selectedCharacterY = y - characterScale - 1
			}
		}

		addKimRequest(KimRequest(
			x = region.minX + 4 * characterScale,
			y = region.minY + 6 * characterScale, scale = characterScale / 2f,
			sprite = assetCharacter.characterClass.skillClass.icon, opacity = 1f
		))

		for ((column, icon) in arrayOf(
			content.ui.activeStarIcon, content.ui.meleeAttackIcon, content.ui.meleeDefenseIcon,
			content.ui.rangedAttackIcon, content.ui.rangedDefenseIcon, content.ui.passiveIcon
		).withIndex()) {
			val x = region.minX + region.width / 7 + column * 12 * characterScale
			addKimRequest(KimRequest(x = x, y = iconY, scale = characterScale / 32f, sprite = icon, opacity = 1f))

			if (column == tab.skillTypeIndex && tab.inside) addKimRequest(KimRequest(
				x = x - 4 * characterScale, y = iconY - 4 * characterScale,
				scale = characterScale / 16f, sprite = content.ui.diagonalPointer, opacity = 1f
			))
		}

		if (selectedSkill != null && tab.inside) addKimRequest(KimRequest(
			x = region.minX + region.width / 200,
			y = region.minY + region.height * 2 / 5,
			sprite = selectedSkill.skill.element.sprite,
			scale = region.width / 500f, opacity = 0.01f
		))
	}

	override fun render() {
		val uiRenderer = resources.uiRenderers[frameIndex]
		val titleTextColor = srgbToLinear(rgb(238, 203, 127))
		val selectionLowColor = srgbToLinear(rgb(25, 72, 119))
		uiRenderer.fillColor(
			selectedCharacterX, selectedCharacterY, selectedCharacterX + 18 * characterScale - 1,
			selectedCharacterY + 18 * characterScale - 1, srgbToLinear(rgb(165, 205, 254)), Gradient(
				1, 1, 18 * characterScale - 2, 18 * characterScale - 2, selectionLowColor, selectionLowColor, 0
			)
		)
		uiRenderer.fillColor(descriptionMaxX, headerY, region.maxX, headerMaxY, srgbToLinear(rgb(50, 37, 27)))

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
			uiRenderer.drawString(
				resources.font, skillTypeDescription, titleTextColor, intArrayOf(),
				region.minX, region.minY, region.minX + region.width / 7 + 75 * characterScale, headerY,
				selectedCharacterY + 7 * characterScale, 4 * characterScale, 1, TextAlignment.RIGHT
			)
		}

		val titleBarLowColor = srgbToLinear(rgb(125, 91, 49))
		val titleBarHighColor = srgbToLinear(rgb(80, 69, 61))
		uiRenderer.fillColor(
			region.minX, headerY, descriptionMaxX, headerMaxY + headerHeight, 0, Gradient(
				0, 0, region.width, 2 * headerHeight, titleBarLowColor, titleBarLowColor, titleBarHighColor
			)
		)

		val leftBarColor = srgbToLinear(rgba(93, 75, 43, 200))
		uiRenderer.fillColor(
			region.minX, headerMaxY + headerHeight, descriptionMaxX, region.maxY, 0, Gradient(
				0, 0, descriptionMaxX - region.minX, region.height, leftBarColor, 0, leftBarColor
			)
		)

		val selectedSkillText = if (tab.inside) selectedSkill?.skill?.name ?: "No skill"
		else assetCharacter.characterClass.skillClass.name
		uiRenderer.drawString(
			resources.font, selectedSkillText, titleTextColor, intArrayOf(),
			region.minX + region.width / 200, headerY, descriptionMaxX, headerY + 2 * headerHeight,
			headerY + 4 * headerHeight / 3, headerHeight * 2 / 3, 1, TextAlignment.LEFT
		)

		for ((minX, maxX, text) in arrayOf(
			Triple(skillsMinX, skillsEnablePointsX, "Ability"),
			Triple(skillsMinX, skillsEnablePointsX, if (tab.skillTypeIndex == 0) "MP" else "RP"),
			Triple(skillsMasteryPointsX, region.maxX, "AP")
		)) {
			uiRenderer.drawString(
				resources.font, text, titleTextColor, intArrayOf(),
				minX, headerY, maxX, headerMaxY,
				headerMaxY - headerHeight / 5, headerHeight * 3 / 5, 1,
				if (text.startsWith("A")) TextAlignment.LEFT else TextAlignment.RIGHT
			)
		}

		var lineY = headerMaxY + 2 * headerHeight
		fun drawDescriptionLine(line: String) {
			uiRenderer.drawString(
				resources.font, line, srgbToLinear(rgb(200, 185, 135)), intArrayOf(),
				region.minX + region.width / 100, headerMaxY, descriptionMaxX, region.maxY,
				lineY, region.width / 60, 1, TextAlignment.DEFAULT
			)
			lineY += region.width / 35
		}

		val description = if (tab.inside) selectedSkill?.skill?.description ?: ""
		else assetCharacter.characterClass.skillClass.description
		renderDescription(description, 25, ::drawDescriptionLine)

		uiRenderer.drawString(
			resources.font, assetCharacter.name, titleTextColor, intArrayOf(),
			region.minX + region.width / 100, headerMaxY, descriptionMaxX, region.maxY,
			region.maxY - region.height / 10, region.height / 35, 1, TextAlignment.DEFAULT
		)

		val resourceX = region.minX + region.width / 25
		val resourceMinY = region.maxY - region.height / 18
		val resourceMaxY = region.maxY - region.height / 30

		val resourceText = if (tab.skillTypeIndex == 0) "MP" else "RP"
		uiRenderer.drawString(
			resources.font, resourceText, titleTextColor, intArrayOf(),
			region.minX, headerMaxY, resourceX - 2, region.maxY,
			resourceMaxY, resourceMaxY - resourceMinY, 1, TextAlignment.RIGHT
		)
		val resourceSplitX = region.minX + (descriptionMaxX - region.minX) * 3 / 5

		val characterState = state.characterStates[assetCharacter]!!
		val maxResourceValue = if (tab.skillTypeIndex == 0) characterState.determineMaxMana(assetCharacter.baseStats)
		else characterState.determineSkillEnablePoints()
		val currentResourceValue = if (tab.skillTypeIndex == 0) characterState.currentMana
		else maxResourceValue - visibleSkills.sumOf { skillEntry ->
			if (skillEntry.isToggled) {
				if (skillEntry.skill is PassiveSkill) (skillEntry.skill as PassiveSkill).enablePoints
				else (skillEntry.skill as ReactionSkill).enablePoints
			} else 0
		}

		if (tab.skillTypeIndex == 0) {
			val leftManaColor = srgbToLinear(rgb(17, 127, 168))
			val rightManaColor = srgbToLinear(rgb(15, 204, 144))
			uiRenderer.fillColor(
				resourceX, resourceMinY, descriptionMaxX, resourceMaxY, 0,
				Gradient(
					0,
					0,
					descriptionMaxX - resourceX,
					resourceMaxY - resourceMinY,
					leftManaColor,
					rightManaColor,
					leftManaColor
				)
			)
		} else {
			val barColor = srgbToLinear(rgb(66, 48, 34))
			val barFilledColor = srgbToLinear(rgb(17, 151, 36))
			val filledX = resourceX + ((descriptionMaxX - resourceX) * (currentResourceValue.toFloat() / maxResourceValue)).roundToInt()
			uiRenderer.fillColor(resourceX, resourceMinY, filledX - 1, resourceMaxY, barFilledColor)
			uiRenderer.fillColor(filledX, resourceMinY, descriptionMaxX, resourceMaxY, barColor)
		}

		val resourceTextColor = srgbToLinear(
			if (tab.skillTypeIndex == 0) rgb(35, 246, 254) else rgb(19, 241, 70)
		)

		uiRenderer.drawString(
			resources.font, currentResourceValue.toString(), resourceTextColor, intArrayOf(),
			resourceX, headerMaxY, resourceSplitX - region.width / 100, region.maxY,
			resourceMaxY + region.height / 100, region.height / 20, 1, TextAlignment.RIGHT
		)
		uiRenderer.drawString(
			resources.font, maxResourceValue.toString(), resourceTextColor, intArrayOf(),
			resourceSplitX, headerMaxY, descriptionMaxX, region.maxY,
			resourceMaxY + region.height / 200, region.height / 28, 1, TextAlignment.LEFT
		)

		for ((row, skillEntry) in visibleSkills.withIndex()) {
			val baseY = skillsMinY + row * skillsSpacing
			val skill = skillEntry.skill
			uiRenderer.fillColor(
				skillsMinX, baseY, skillsEnablePointsX + region.height / 100,
				baseY + region.height / 33, rgba(0, 0, 0, 100)
			)
			uiRenderer.drawString(
				resources.font, skill.name, titleTextColor, intArrayOf(),
				skillsMinX + region.height / 20, headerMaxY, skillsEnablePointsX, region.maxY,
				baseY + region.height / 33, region.height / 35, 1, TextAlignment.LEFT
			)

			val rp = if (skill is ActiveSkill) skill.manaCost
			else if (skill is PassiveSkill) skill.enablePoints
			else (skill as ReactionSkill).enablePoints
			uiRenderer.drawString(
				resources.font, rp.toString(), titleTextColor, intArrayOf(),
				skillsMinX + region.height / 20, headerMaxY, skillsEnablePointsX - region.height / 100, region.maxY,
				baseY + region.height / 33, region.height / 35, 1, TextAlignment.RIGHT
			)

			val masteryBarColor = srgbToLinear(rgb(59, 38, 29))
			val masteryFilledBarColor = srgbToLinear(rgb(159, 31, 23))
			if (skillEntry.mastery < skill.masteryPoints) {
				val minMasteryX = skillsMasteryPointsX + region.height / 100
				val maxMasteryX = region.maxX - region.height / 50
				val filledX = minMasteryX + ((maxMasteryX - minMasteryX) * (skillEntry.mastery.toFloat() / skill.masteryPoints)).roundToInt()
				uiRenderer.fillColor(
					minMasteryX, baseY + region.height / 200, filledX - 1,
					baseY + region.height / 40, masteryFilledBarColor
				)
				uiRenderer.fillColor(
					filledX, baseY + region.height / 200, maxMasteryX,
					baseY + region.height / 40, masteryBarColor
				)
				val masteryTextColor = srgbToLinear(rgb(253, 94, 94))
				val masterySplitX = (maxMasteryX + minMasteryX) / 2
				uiRenderer.drawString(
					resources.font, skillEntry.mastery.toString(), masteryTextColor, intArrayOf(),
					minMasteryX, region.minY, masterySplitX, region.maxY,
					baseY + region.height / 30, region.height / 28, 1, TextAlignment.RIGHT
				)
				uiRenderer.drawString(
					resources.font, skill.masteryPoints.toString(), masteryTextColor, intArrayOf(),
					masterySplitX + region.height / 100, region.minY, maxMasteryX, region.maxY,
					baseY + region.height / 33, region.height / 35, 1, TextAlignment.LEFT
				)
			}
		}

		resources.kim1Renderer.submit(kim1Batch, recorder, targetImage)
		resources.kim2Renderer.submit(kim2Batch, recorder, targetImage)
	}
}
