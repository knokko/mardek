package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dTextStyle
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.PassiveSkill
import mardek.content.sprite.BcSprite
import mardek.content.stats.CombatStat
import mardek.renderer.MardekTextStyles
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.renderPortraitAnimation
import mardek.renderer.util.ResourceBarRenderer
import mardek.renderer.util.ResourceType
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.menu.PartyTab
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

internal fun renderPartyTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		if (region.width < 50 || region.height < 50) return
		val partyTab = menu.currentTab as PartyTab

		val width = min(14 * region.width / 15, 5 * region.height / 4)

		var descriptionX = region.minX + width
		run {
			val borderColor = MardekTextStyles.WEAK_TEXT_FILL.color
			val selectedColor = srgbToLinear(rgb(193, 169, 113))
			val unselectedColor = srgbToLinear(rgb(40, 10, 30))
			val borderWidth = max(1, width / 500)
			for (tabIndex in (0 until PartyTab.NUM_TABS).reversed()) {
				val middleColor = if (tabIndex == partyTab.currentTab) selectedColor else unselectedColor

				val minY = region.minY + width / 38
				val maxX = descriptionX - 1
				val minX = maxX - 5 * borderWidth
				val maxY = region.minY + width / 23
				colorBatch.fill(minX, minY, maxX, minY + borderWidth - 1, borderColor)
				colorBatch.fill(minX, 1 + maxY - borderWidth, maxX, maxY, borderColor)
				colorBatch.fill(minX, minY, minX + borderWidth - 1, maxY, borderColor)
				colorBatch.fill(1 + maxX - borderWidth, minY, maxX, maxY, borderColor)
				colorBatch.fill(minX + borderWidth, minY + borderWidth, maxX - borderWidth, maxY - borderWidth, middleColor)

				descriptionX -= 8 * borderWidth
			}
		}

		val arrowOffset = 0.003f * width * determinePointerOffset()
		val arrowSprite = context.content.ui.arrowHead
		val arrowScale = 0.04f * width / arrowSprite.height
		imageBatch.rotated(
			descriptionX + 0.02f * width + arrowOffset, region.minY - 0.04f * width,
			180f, arrowScale, arrowSprite.index, 0, -1,
		)
		imageBatch.rotated(
			descriptionX + 0.085f * width - arrowOffset, region.minY - 0.04f * width,
			0f, arrowScale, arrowSprite.index, 0, -1,
		)

		val tabDescription = when (partyTab.currentTab) {
			0 -> "Condition"
			1 -> "Vital Statistics"
			2 -> "Elemental Resistances"
			3 -> "Status Resistances"
			4 -> "Growth"
			else -> "Performance"
		}
		val descriptionFont = context.bundle.getFont(context.content.fonts.large1.index)
		simpleTextBatch.drawString(
			tabDescription, descriptionX - 0.01f * width, region.minY + 0.0425f * width,
			0.02f * width, descriptionFont,
			MardekTextStyles.WEAK_TEXT_FILL.only(), TextAlignment.RIGHT,
		)

		val areaState = state.state
		if (areaState is AreaState) {
			val area = areaState.area
			val areaFont = context.bundle.getFont(context.content.fonts.basic2.index)
			simpleTextBatch.drawString(
				area.properties.displayName, region.minX + 0.02f * region.height,
				region.maxY + 0.075f * region.height, 0.04f * region.height,
				areaFont, srgbToLinear(rgb(238, 203, 127)),
			)
		}

		for (partyMember in state.usedPartyMembers()) {
			val baseY = region.minY + width / 20 + 2 * width * partyMember.index / 11
			val blockRegion = Rectangle(region.minX, baseY, width, width / 8)
			renderPartyMember(menuContext, blockRegion, partyMember.character, partyMember.state, partyTab.currentTab)
		}
	}
}

private fun renderPartyMember(
	menuContext: MenuRenderContext, region: Rectangle,
	character: PlayableCharacter, state: CharacterState, currentTab: Int,
) {
	menuContext.apply {
		val lineWidth = max(1, region.width / 350)
		val lineColor = MardekTextStyles.WEAK_TEXT_FILL.color
		colorBatch.fill(region.minX, region.minY, region.maxX, region.minY + lineWidth - 1, lineColor)
		colorBatch.fill(region.minX, region.boundY - lineWidth, region.maxX, region.maxY, lineColor)
		colorBatch.fill(region.boundX - lineWidth, region.minY, region.maxX, region.maxY, lineColor)

		colorBatch.gradient(
			region.minX, region.minY + lineWidth,
			region.maxX - lineWidth, region.maxY - lineWidth,
			srgbToLinear(rgb(47, 33, 23)),
			srgbToLinear(rgb(103, 82, 49)),
			srgbToLinear(rgb(44, 31, 19)),
		)

		val margin = region.width / 100
		colorBatch.gradient(
			region.minX + margin, region.minY + margin,
			region.maxX - margin, region.maxY - region.height / 2 + margin,
			rgba(255, 255, 230, 5),
			rgba(255, 255, 120, 5),
			rgba(255, 255, 230, 5)
		)
	}

	menuContext.apply {
		val magicScale = region.height / 50f

		val renderWidth = 60f * magicScale

		val renderX = region.minX + renderWidth - region.height * 0.1f
		val renderY = region.minY - region.height * 0.25f

		val animationContext = AnimationContext(
			renderRegion = Rectangle(region.minX, region.minY - region.height / 2, region.width, 3 * region.height / 2),
			renderTime = System.nanoTime(),
			magicScale = context.content.portraits.magicScale,
			parentMatrix = Matrix3x2f().translate(renderX, renderY).scale(-magicScale, magicScale),
			parentColorTransform = null,
			partBatch = animationPartBatch,
			noMask = context.content.battle.noMask,
			combat = null,
			portrait = character.portraitInfo,
			currentChapter = context.campaign.story.evaluate(context.content.story.fixedVariables.chapter) ?: 0,
			portraitExpression = "norm",
			animationDuration = Duration.ZERO,
		)
		renderPortraitAnimation(context.content.portraits.animations, animationContext)
	}

	when (currentTab) {
		0 -> renderCondition(menuContext, region, character, state)
		1 -> renderVitalStatistics(menuContext, region, character, state)
		2 -> renderElementalResistances(menuContext, region, character, state)
		3 -> renderStatusResistances(menuContext, region, state)
		4 -> renderGrowth(menuContext, region, state)
		5 -> renderPerformance1(menuContext, region, state)
		6 -> renderPerformance2(menuContext, region, state)
	}
}

private fun renderCondition(
	menuContext: MenuRenderContext, region: Rectangle,
	character: PlayableCharacter, state: CharacterState,
) {
	menuContext.apply {
		val y1 = region.minY + 0.39f * region.height
		val height1 = 0.24f * region.height
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		val style1 = MardekTextStyles.TitleScreen.GAME_NAME_LABEL
		simpleTextBatch.drawShadowedString(
			character.name, region.minX + 0.175f * region.width, y1,
			height1, font, style1, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			"Lv ${state.currentLevel} ${character.characterClass.displayName}",
			region.minX + 0.46f * region.width, y1,
			height1, font, style1, TextAlignment.LEFT,
		)

		val y2 = region.minY + 0.1f * region.width
		val height2 = 0.16f * region.height
		val style2 = MardekTextStyles.STRONG_TEXT_FILL.only()
		simpleTextBatch.drawString(
			"HP", region.minX + 0.175f * region.width, y2,
			height2, font, style2, TextAlignment.LEFT,
		)
		simpleTextBatch.drawString(
			"MP", region.minX + 0.46f * region.width, y2,
			height2, font, style2, TextAlignment.LEFT,
		)
		simpleTextBatch.drawString(
			"XP", region.minX + 0.7f * region.width, y2,
			height2, font, style2, TextAlignment.LEFT,
		)

		val healthBar = ResourceBarRenderer(context, ResourceType.HealthPartyTab, Rectangle(
			region.minX + 2 * region.width / 9, region.minY + 2 * region.width / 25,
			23 * region.width / 100, region.width / 42
		), colorBatch, simpleTextBatch)
		val maxHealth = state.determineMaxHealth(character.baseStats, state.activeStatusEffects)
		healthBar.renderBar(state.currentHealth, maxHealth)
		healthBar.renderTextOverBarWithShadow(state.currentHealth, maxHealth)

		val manaBar = ResourceBarRenderer(context, ResourceType.ManaPartyTab, Rectangle(
			region.minX + 51 * region.width / 100, region.minY + 2 * region.width / 25,
			9 * region.width / 50, region.width / 42
		), colorBatch, simpleTextBatch)
		val maxMana = state.determineMaxMana(character.baseStats, state.activeStatusEffects)
		manaBar.renderBar(state.currentMana, maxMana)
		manaBar.renderTextOverBarWithShadow(state.currentMana, maxMana)

		val xpBar = ResourceBarRenderer(context, ResourceType.Experience, Rectangle(
			region.minX + 74 * region.width / 100, region.minY + 2 * region.width / 25,
			region.width / 8, region.width / 42
		), colorBatch, simpleTextBatch)
		val maxXp = state.experienceForNextLevel()
		xpBar.renderBar(state.experienceToNextLevel, maxXp)
		xpBar.renderPercentage(state.experienceToNextLevel, maxXp)

		imageBatch.colored(
			region.maxX - 0.125f * region.width, region.minY + 0.001f * region.width,
			region.maxX - 0.002f * region.width, region.maxY - 0.001f * region.width,
			character.element.thinSprite.index, 0,
			rgba(255, 255, 255, 200)
		)
	}
}

private fun renderVitalStatistics(
	menuContext: MenuRenderContext, region: Rectangle,
	character: PlayableCharacter, state: CharacterState,
) {
	menuContext.apply {
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		val upperStats = arrayOf(CombatStat.Strength, CombatStat.Vitality, CombatStat.Spirit, CombatStat.Agility)
		for ((index, stat) in upperStats.withIndex()) {
			val baseY = region.minY + 0.35f * region.height
			val splitX = region.minX + 0.24f * region.width + index * 0.2f * region.width
			simpleTextBatch.drawString(
				"${stat.flashName}:", splitX, baseY, 0.18f * region.height, font,
				MardekTextStyles.STRONG_TEXT_FILL.only(), TextAlignment.RIGHT,
			)

			val baseValue = character.baseStats.find { it.stat == stat }!!.adder
			val currentValue = state.computeStatValue(character.baseStats, state.activeStatusEffects, stat)
			val currentValueStyle = if (currentValue == baseValue) MardekTextStyles.PartyTab.NEUTRAL_STAT
			else if (currentValue > baseValue) MardekTextStyles.PartyTab.POSITIVE_STAT
			else MardekTextStyles.PartyTab.NEGATIVE_STAT

			simpleTextBatch.drawShadowedString(
				currentValue.toString(), splitX + 0.01f * region.width,
				baseY + 0.02f * region.height, 0.255f * region.height, font,
				Vk2dTextStyle.Shadowed(
					currentValueStyle,
					MardekTextStyles.SHADOW_FILL.only(),
					0.1f,
				), TextAlignment.LEFT,
			)

			if (baseValue != currentValue) {
				val difference = if (baseValue < currentValue) "+${currentValue - baseValue}"
				else "-${baseValue - currentValue}"
				simpleTextBatch.drawShadowedString(
					difference, splitX + 0.075f * region.width, baseY,
					0.17f * region.height, font,
					MardekTextStyles.PartyTab.STAT_DIFFERENCE, TextAlignment.LEFT,
				)
			}
		}

		val lowerStats = arrayOf(CombatStat.Attack, CombatStat.MeleeDefense, CombatStat.RangedDefense)
		for ((index, stat) in lowerStats.withIndex()) {
			val baseY = region.minY + 0.8f * region.height
			val baseX = region.minX + 0.25f * region.width + index * 0.2f * region.width
			val statValue = state.computeStatValue(character.baseStats, state.activeStatusEffects, stat)
			simpleTextBatch.drawShadowedString(
				statValue.toString(), baseX, baseY, 0.22f * region.height, font,
				MardekTextStyles.PartyTab.LOWER_STATS, TextAlignment.LEFT,
			)

			val icon = when (stat) {
				CombatStat.Attack -> context.content.ui.attackIcon
				CombatStat.MeleeDefense -> context.content.ui.defIcon
				CombatStat.RangedDefense -> context.content.ui.rangedDefIcon
				else -> throw Error("Unexpected stat $stat")
			}
			imageBatch.simpleScale(
				baseX - 0.06f * region.width, baseY - 0.035f * region.width,
				0.05f * region.width / icon.height, icon.index
			)
		}
	}
}

private fun renderResistance(
	menuContext: MenuRenderContext, region: Rectangle,
	icon: BcSprite, resistance: Int, index: Int, rowMajor: Boolean
) {
	if (index >= 10) return
	val (indexX, indexY) = if (rowMajor) Pair(index % 5, index / 5)
	else if (index == 1) Pair(1, 0)
	else if (index == 2) Pair(0, 1)
	else Pair(index / 2, index % 2)

	val baseMinX = region.minX + ((0.19f + 0.16f * indexX) * region.width).roundToInt()
	val baseMinY = region.minY + ((if (indexY == 0) 0.11f else 0.56f) * region.height).roundToInt()
	val baseMaxX = baseMinX + (0.11f * region.width).roundToInt()
	val baseMaxY = baseMinY + (0.32f * region.height).roundToInt()
	val radius = (1 + baseMaxY - baseMinY) * 0.5f

	val (backgroundColor, textColor, displayResistance) = if (resistance > 100) {
		Triple(
			srgbToLinear(rgb(67, 78, 26)),
			srgbToLinear(rgb(152, 255, 102)),
			resistance
		)
	} else if (resistance > 0) {
		Triple(
			srgbToLinear(rgb(80, 75, 90)),
			srgbToLinear(rgb(85, 220, 255)),
			resistance,
		)
	} else if (resistance == 0) {
		Triple(
			srgbToLinear(rgba(72, 50, 32, 30)),
			0, 0
		)
	} else {
		Triple(
			srgbToLinear(rgb(110, 60, 40)),
			srgbToLinear(rgb(255, 102, 102)),
			-resistance
		)
	}

	menuContext.colorBatch.fill(baseMinX, baseMinY, baseMaxX, baseMaxY, backgroundColor)
	menuContext.ovalBatch.antiAliased(
		baseMinX - radius.roundToInt() - 2, baseMinY, baseMinX - 1, baseMaxY,
		baseMinX.toFloat(), baseMinY + radius, radius, radius,
		0.1f, backgroundColor,
	)
	menuContext.ovalBatch.antiAliased(
		baseMaxX + 1, baseMinY, baseMaxX + radius.roundToInt() + 2, baseMaxY,
		baseMaxX.toFloat(), baseMinY + radius, radius, radius,
		0.1f, backgroundColor,
	)

	menuContext.imageBatch.coloredScale(
		baseMinX - 1.1f * radius, baseMinY - radius * 0.1f,
		radius * 2.2f / icon.height, icon.index, 0,
		if (resistance == 0) rgba(1f, 1f, 1f, 0.05f) else -1,
	)

	if (textColor != 0) {
		val baseFont = menuContext.context.bundle.getFont(menuContext.context.content.fonts.basic2.index)
		menuContext.simpleTextBatch.drawShadowedString(
			"$displayResistance%", baseMaxX - 0.05f * region.height,
			baseMinY + 0.225f * region.height,  0.15f * region.height, baseFont,
			MardekTextStyles.Encyclopedia.elementalResistance(textColor), TextAlignment.RIGHT,
		)
	}
}

private fun renderElementalResistances(
	menuContext: MenuRenderContext, region: Rectangle,
	character: PlayableCharacter, state: CharacterState,
) {
	for ((index, element) in menuContext.context.content.stats.elements.withIndex()) {
		if (index >= 10) break

		var resistance = 0f
		for (equipment in state.equipment.values) {
			val properties = equipment.equipment ?: continue
			resistance += properties.resistances.get(element)
		}

		for (effect in state.activeStatusEffects) {
			resistance += effect.resistances.get(element)
		}

		for (skill in state.toggledSkills) {
			if (skill is PassiveSkill) resistance += skill.resistances.get(element)
		}

		if (element === character.element) resistance += 0.2f
		if (element === character.element.weakAgainst) resistance -= 0.2f

		val percentageResistance = (100f * resistance).roundToInt()
		renderResistance(menuContext, region, element.thickSprite, percentageResistance, index, false)
	}
}

private fun renderStatusResistances(menuContext: MenuRenderContext, region: Rectangle, state: CharacterState) {
	for ((index, effect) in menuContext.context.content.stats.statusEffects.withIndex()) {
		if (index >= 10) break

		var resistance = 0f
		for (equipment in state.equipment.values) {
			val properties = equipment.equipment ?: continue
			resistance += properties.resistances.get(effect)
		}

		for (effect in state.activeStatusEffects) {
			resistance += effect.resistances.get(effect)
		}

		for (skill in state.toggledSkills) {
			if (skill is PassiveSkill) resistance += skill.resistances.get(effect)
		}

		val percentageResistance = (100f * resistance).roundToInt()
		renderResistance(menuContext, region, effect.icon, percentageResistance, index, true)
	}
}

private fun renderGrowth(
	menuContext: MenuRenderContext, region: Rectangle,
	state: CharacterState,
) {
	menuContext.apply {
		val simpleFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val numberFont = context.bundle.getFont(context.content.fonts.large1.index)
		simpleTextBatch.drawString(
			"EXP:", region.minX + 0.17f * region.width, region.minY + 0.33f * region.height,
			0.17f * region.height, simpleFont,
			MardekTextStyles.STRONG_TEXT_FILL.only(), TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			"Level ${state.currentLevel}", region.minX + 0.74f * region.width,
			region.minY + 0.4f * region.height, 0.25f * region.height, simpleFont,
			MardekTextStyles.TitleScreen.GAME_NAME_LABEL, TextAlignment.LEFT,
		)

		fun format(amount: Int) = String.format(Locale.ROOT, "%,d", amount)

		simpleTextBatch.drawShadowedString(
			"${format(state.experienceToNextLevel)} /  ${format(state.experienceForNextLevel())}",
			region.minX + 0.36f * region.width, region.minY + 0.4f * region.height,
			0.25f * region.height, numberFont,
			MardekTextStyles.PartyTab.GROWTH_EXPERIENCE, TextAlignment.LEFT,
		)

		val xpBar = ResourceBarRenderer(context, ResourceType.Experience, Rectangle(
			region.minX + 17 * region.width / 100, region.minY + 6 * region.height / 10,
			8 * region.width / 10, region.height / 5
		), colorBatch, simpleTextBatch)
		xpBar.renderBar(state.experienceToNextLevel, state.experienceForNextLevel())
	}
}

private fun renderPerformance1(menuContext: MenuRenderContext, region: Rectangle, state: CharacterState) {
	menuContext.apply {
		val labelStyle = MardekTextStyles.STRONG_TEXT_FILL.only()
		val valueStyle = MardekTextStyles.PartyTab.LOWER_STATS

		val labelY1 = region.minY + 0.33f * region.height
		val valueY1 = region.minY + 0.36f * region.height
		val valueY2 = region.minY + 0.82f * region.height

		val labelHeight = 0.18f * region.height
		val valueHeight = 0.24f * region.height
		val font = context.bundle.getFont(context.content.fonts.basic2.index)

		simpleTextBatch.drawString(
			"Battles:", region.minX + 0.18f * region.width, labelY1,
			labelHeight, font, labelStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numBattles.toString(), region.minX + 0.295f * region.width, valueY1,
			valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawString(
			"Kills:", region.minX + 0.455f * region.width, labelY1,
			labelHeight, font, labelStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numKills.toString(), region.minX + 0.54f * region.width, valueY1,
			valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawString(
			"KO'd:", region.minX + 0.72f * region.width, labelY1,
			labelHeight, font, labelStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numFaints.toString(), region.minX + 0.815f * region.width, valueY1,
			valueHeight, font, valueStyle, TextAlignment.LEFT,
		)

		simpleTextBatch.drawString(
			"USED:", region.minX + 0.18f * region.width, region.minY + 0.76f * region.height,
			labelHeight, font, labelStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numMeleeAttacks.toString(), region.minX + 0.33f * region.width,
			valueY2, valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numMagicSkills.toString(), region.minX + 0.54f * region.width,
			valueY2, valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.numItems.toString(), region.minX + 0.76f * region.width,
			valueY2, valueHeight, font, valueStyle, TextAlignment.LEFT,
		)

		val iconHeight = 0.465f * region.height
		val iconY = region.minY + 0.47f * region.height
		val sword = context.content.ui.performanceMeleeAttacks
		val magic = context.content.ui.performanceMagicSkills
		val items = context.content.ui.performanceUsedItems
		imageBatch.simpleScale(
			region.minX + 0.28f * region.width, iconY,
			iconHeight / sword.height, sword.index,
		)
		imageBatch.simpleScale(
			region.minX + 0.475f * region.width, iconY,
			iconHeight / magic.height, magic.index,
		)
		imageBatch.simpleScale(
			region.minX + 0.71f * region.width, iconY,
			iconHeight / items.height, items.index,
		)
	}
}

private fun renderPerformance2(menuContext: MenuRenderContext, region: Rectangle, state: CharacterState) {
	menuContext.apply {
		val labelStyle = MardekTextStyles.STRONG_TEXT_FILL.only()
		val valueStyle = MardekTextStyles.PartyTab.LOWER_STATS

		val labelX = region.minX + 0.41f * region.width
		val valueX = region.minX + 0.45f * region.width

		val labelHeight = 0.18f * region.height
		val valueHeight = 0.24f * region.height
		val font = context.bundle.getFont(context.content.fonts.basic2.index)

		simpleTextBatch.drawString(
			"Damage dealt:", labelX, region.minY + 0.33f * region.height,
			labelHeight, font, labelStyle, TextAlignment.RIGHT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.damageDealt.toString(), valueX, region.minY + 0.34f * region.height,
			valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
		simpleTextBatch.drawString(
			"Damage received:", labelX, region.minY + 0.79f * region.height,
			labelHeight, font, labelStyle, TextAlignment.RIGHT,
		)
		simpleTextBatch.drawShadowedString(
			state.performance.damageReceived.toString(), valueX, region.minY + 0.80f * region.height,
			valueHeight, font, valueStyle, TextAlignment.LEFT,
		)
	}
}
