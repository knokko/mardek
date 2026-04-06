package mardek.renderer.menu

import com.github.knokko.bitser.ReferenceLazyBits
import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.CombatantAnimationContext
import mardek.renderer.animation.renderBattleBackgroundAnimation
import mardek.renderer.animation.renderCombatantAnimation
import mardek.renderer.animation.renderCutsceneAnimation
import mardek.renderer.animation.renderPortraitAnimation
import mardek.renderer.util.renderDescription
import mardek.state.ingame.battle.CombatantRenderInfo
import mardek.state.ingame.encyclopedia.EncyclopediaSnapshot
import mardek.state.ingame.menu.EncyclopediaTab
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderEncyclopediaTab(menuContext: MenuRenderContext, partialRegion: Rectangle, fullRegion: Rectangle) {
	val tab = menuContext.menu.currentTab as EncyclopediaTab
	if (tab.inside) {
		val (sectionName, renderDetailsFunction, renderListFunction) = when (tab.currentSection) {
			0 -> Triple("People", ::renderPersonDetails) {
				renderList(menuContext, tab.encyclopedia.people.map { person ->
					if (person.entry == null) {
						EncyclopediaSnapshot.OptionalEntry(null, person.lengthOfName!!, null)
					} else {
						EncyclopediaSnapshot.OptionalEntry(
							ListEntry(person.entry!!.element, person.entry!!.firstName),
							null, null,
						)
					}
				}, fullRegion)
			}
			1 -> Triple("Places", ::renderPlaceDetails) {
				renderList(menuContext, tab.encyclopedia.places.map { place ->
					if (place.entry == null) {
						EncyclopediaSnapshot.OptionalEntry(
							null, place.lengthOfName!!, null
						)
					} else {
						EncyclopediaSnapshot.OptionalEntry(
							ListEntry(null, place.entry!!.name),
							null, null,
						)
					}
				}, fullRegion)
			}
			2 -> Triple("Artefacts", ::renderArtefactDetails) {
				renderList(menuContext, tab.encyclopedia.artefacts.map { artefact ->
					if (artefact.entry == null) {
						EncyclopediaSnapshot.OptionalEntry(
							null, artefact.lengthOfName!!, null
						)
					} else {
						EncyclopediaSnapshot.OptionalEntry(
							ListEntry(artefact.entry!!.element, artefact.entry!!.name),
							null, null,
						)
					}
				}, fullRegion)
			}
			3 -> Triple("Bestiary", ::renderMonsterDetails) {
				renderList(menuContext, tab.encyclopedia.monsters.map { monsterEntry ->
					if (monsterEntry.entry != null) {
						EncyclopediaSnapshot.OptionalEntry(
							ListEntry(
								monsterEntry.entry!!.monsters[0].element,
								monsterEntry.entry!!.monsters[0].displayName,
							),
							null, monsterEntry.amount!!
						)
					} else {
						EncyclopediaSnapshot.OptionalEntry(
							null, monsterEntry.lengthOfName!!, monsterEntry.amount!!
						)
					}
				}, fullRegion)
			}
			else -> throw Error("Unexpected encyclopedia section ${tab.currentSection}")
		}

		menuContext.apply {
			val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
			textBatch.drawString(
				sectionName, fullRegion.maxX - 0.05f * fullRegion.height,
				fullRegion.minY + fullRegion.height / 16f, fullRegion.height / 24f, upperFont,
				srgbToLinear(rgb(238, 203, 127)),
				TextAlignment.RIGHT,
			)
		}

		if (tab.showDetailsOfCurrentEntry) {
			renderDetailsFunction(menuContext, fullRegion)
		} else {
			renderListFunction()
		}
	} else {
		renderOutside(tab, menuContext, partialRegion)
	}
}

private class ListEntry(
	val element: Element?,
	val name: String,
)

private fun renderPersonDetails(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val tab = menu.currentTab as EncyclopediaTab
		val person = tab.encyclopedia.people[tab.currentEntry].entry!!
		val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val fatTextColor = srgbToLinear(rgb(255, 243, 159))
		textBatch.drawString(
			"${person.firstName} ${person.lastName}", region.minX + 0.15f * region.height,
			region.minY + 0.175f * region.height, 0.0375f * region.height,
			upperFont, baseTextColor,
		)

		imageBatch.simpleScale(
			region.minX + 0.03f * region.height, region.minY + 0.105f * region.height,
			0.1f * region.height / person.element.thinSprite.height, person.element.thinSprite.index,
		)

		val splitX = region.maxX - 0.3f * region.height
		val minDescriptionX = region.minX + 0.04f * region.height
		val maxDescriptionX = splitX - 0.275f * region.height

		var descriptionY = region.minY + 0.275f * region.height
		renderDescription(
			person.description, ((maxDescriptionX - minDescriptionX) * 72.5f / region.height).roundToInt()
		) { line ->
			val shadowColor = srgbToLinear(rgb(90, 52, 22))
			val shadowOffset = 0.003f * region.height
			textBatch.drawShadowedString(
				line, minDescriptionX, descriptionY, 0.02f * region.height, baseFont,
				baseTextColor, 0, 0f, shadowColor,
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			@Suppress("AssignedValueIsNeverRead")
			descriptionY += 0.035f * region.height
		}

		val propertyPairs = arrayOf(
			Pair("Race", person.creatureType.niceName),
			Pair("Gender", person.gender.name),
			// TODO CHAP2 Update age based on timeline variable
			Pair("Age", person.overrideAge ?: person.initialAge!!.toString()),
			Pair("Class", person.characterClass.displayName),
			Pair("Element", person.element.properName),
			Pair("Place of Origin", person.origin),
			Pair("Weapon", person.weaponType.niceName),
			null,
			Pair("Alignment", person.alignment.displayName),
		)

		for ((propertyIndex, rawPair) in propertyPairs.withIndex()) {
			if (rawPair == null) continue
			val (propertyName, propertyValue) = rawPair

			val textHeight = 0.025f * region.height
			val textY = region.minY + 0.5f * region.height + propertyIndex * 0.045f * region.height
			textBatch.drawString(
				"$propertyName:", splitX - 0.025f * region.height, textY,
				textHeight, baseFont, baseTextColor, TextAlignment.RIGHT,
			)
			textBatch.drawString(
				propertyValue, splitX, textY, textHeight, baseFont, fatTextColor
			)
		}

		val animationContext = AnimationContext(
			renderRegion = region,
			renderTime = System.nanoTime(),
			magicScale = context.content.portraits.magicScale,
			parentMatrix = Matrix3x2f().translate(
				splitX - 0.1f * region.height,
				region.minY + 0.125f * region.height,
			).scale(0.00425f * region.height),
			parentColorTransform = null,
			partBatch = animationPartBatch,
			noMask = context.content.battle.noMask,
			combat = null,
			portrait = person.portrait,
			portraitExpression = person.portraitExpression
		)
		renderPortraitAnimation(context.content.portraits.animations, animationContext)
	}
}

private fun renderPlaceDetails(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.apply {
		val tab = menu.currentTab as EncyclopediaTab
		val place = tab.encyclopedia.places[tab.currentEntry].entry!!

		val imageWidth = min(region.width - region.height / 10, 3 * region.height / 2)
		val imageRegion = Rectangle(
			region.minX + (region.width - imageWidth) / 2,
			region.minY + (0.175f * region.height).roundToInt(),
			imageWidth, (0.3f * imageWidth).roundToInt(),
		)

		val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		textBatch.drawString(
			place.name, imageRegion.minX - 0.01f * region.height,
			region.minY + 0.15f * region.height, 0.041f * region.height,
			upperFont, baseTextColor,
		)

		val minDescriptionX = imageRegion.minX + 0.01f * region.height
		val maxDescriptionX = imageRegion.maxX - 0.01f * region.height

		var descriptionY = imageRegion.maxY + 0.045f * region.height
		renderDescription(
			place.description, ((maxDescriptionX - minDescriptionX) * 75f / region.height).roundToInt()
		) { line ->
			val shadowColor = srgbToLinear(rgb(90, 52, 22))
			val shadowOffset = 0.003f * region.height
			textBatch.drawShadowedString(
				line, minDescriptionX, descriptionY, 0.02f * region.height, baseFont,
				baseTextColor, 0, 0f, shadowColor,
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			@Suppress("AssignedValueIsNeverRead")
			descriptionY += 0.035f * region.height
		}

		val borderColor = srgbToLinear(rgb(208, 193, 142))
		colorBatch.fill(
			imageRegion.minX, imageRegion.minY - 1,
			imageRegion.maxX, imageRegion.minY - 1, borderColor
		)
		colorBatch.fill(
			imageRegion.minX, imageRegion.maxY + 1,
			imageRegion.maxX, imageRegion.maxY + 1, borderColor
		)
		colorBatch.fill(
			imageRegion.minX - 1, imageRegion.minY,
			imageRegion.minX - 1, imageRegion.maxY, borderColor
		)
		colorBatch.fill(
			imageRegion.maxX + 1, imageRegion.minY,
			imageRegion.maxX + 1, imageRegion.maxY, borderColor
		)
		val animationContext = AnimationContext(
			renderRegion = imageRegion,
			renderTime = System.nanoTime(),
			magicScale = place.background.magicScale,
			parentMatrix = Matrix3x2f().translate(
				imageRegion.minX - 0.0075f * imageWidth,
				imageRegion.minY - 0.0125f * imageWidth,
			).scale(0.00258f * imageWidth),
			parentColorTransform = null,
			partBatch = animationPartBatch,
			noMask = context.content.battle.noMask,
			combat = null,
			portrait = null,
		)
		renderBattleBackgroundAnimation(place.background.nodes, animationContext)
	}
}

private fun renderArtefactDetails(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val tab = menu.currentTab as EncyclopediaTab
		val artefact = tab.encyclopedia.artefacts[tab.currentEntry].entry!!
		val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		textBatch.drawString(
			artefact.name, region.minX + 0.045f * region.height,
			region.minY + 0.15f * region.height, 0.04f * region.height,
			upperFont, baseTextColor,
		)

		val minDescriptionX = region.minX + 0.6f * region.height
		val maxDescriptionX = region.maxX - 0.05f * region.height

		var descriptionY = region.minY + 0.21f * region.height
		renderDescription(
			artefact.description, ((maxDescriptionX - minDescriptionX) * 75f / region.height).roundToInt()
		) { line ->
			val shadowColor = srgbToLinear(rgb(90, 52, 22))
			val shadowOffset = 0.003f * region.height
			textBatch.drawShadowedString(
				line, minDescriptionX, descriptionY, 0.02f * region.height, baseFont,
				baseTextColor, 0, 0f, shadowColor,
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			@Suppress("AssignedValueIsNeverRead")
			descriptionY += 0.035f * region.height
		}

		val animationContext = AnimationContext(
			renderRegion = region,
			renderTime = System.nanoTime(),
			magicScale = artefact.magicScale,
			parentMatrix = Matrix3x2f().translate(
				region.minX + 0.32f * region.height,
				region.minY + 0.525f * region.height,
			).scale(0.0019f * region.height),
			parentColorTransform = null,
			partBatch = animationPartBatch,
			noMask = context.content.battle.noMask,
			combat = null,
			portrait = null,
		)
		renderCutsceneAnimation(ReferenceLazyBits(artefact.animation.frames), animationContext)
	}
}

private fun renderMonsterDetails(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val tab = menu.currentTab as EncyclopediaTab
		val monsterEntry = tab.encyclopedia.monsters[tab.currentEntry].entry!!
		val monster = monsterEntry.monsters[0]
		val upperFont = context.bundle.getFont(context.content.fonts.large2.index)
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val fatTextColor = srgbToLinear(rgb(255, 243, 159))
		textBatch.drawString(
			monster.displayName, region.minX + 0.15f * region.height,
			region.minY + 0.175f * region.height, 0.0375f * region.height,
			upperFont, baseTextColor,
		)

		imageBatch.simpleScale(
			region.minX + 0.03f * region.height, region.minY + 0.105f * region.height,
			0.1f * region.height / monster.element.thinSprite.height,
			monster.element.thinSprite.index,
		)

		val splitX = region.maxX - 0.37f * region.height
		val minDescriptionX = region.minX + 0.04f * region.height
		val maxDescriptionX = splitX - 0.015f * region.height

		val shadowColor = srgbToLinear(rgb(90, 52, 22))
		val shadowOffset = 0.003f * region.height

		var descriptionY = region.minY + 0.7f * region.height
		renderDescription(
			monsterEntry.description, ((maxDescriptionX - minDescriptionX) * 72.5f / region.height).roundToInt()
		) { line ->

			textBatch.drawShadowedString(
				line, minDescriptionX, descriptionY, 0.02f * region.height, baseFont,
				baseTextColor, 0, 0f, shadowColor,
				shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			@Suppress("AssignedValueIsNeverRead")
			descriptionY += 0.035f * region.height
		}

		spriteBatch.simple(
			region.maxX - (0.085f * region.height).roundToInt(),
			region.minY + (0.105f * region.height).roundToInt(),
			max(1f, 0.004f * region.height),
			monster.type.icon.index,
		)

		fun describeAmountPerLevel(base: Int?, perLevelNumerator: Int, perLevelDenominator: Int): String {
			val result = StringBuilder()
			if (base != null && base != 0) result.append("$base")
			if (perLevelNumerator != 0 && perLevelDenominator != 0) {
				if (base != null && base != 0) result.append(" + ")
				if (perLevelNumerator != 1) result.append("$perLevelNumerator x ")
				result.append("Lv")
				if (perLevelDenominator != 1) result.append("/ $perLevelDenominator")
			}
			return result.toString()
		}

		val propertyPairs1 = arrayOf(
			Pair("Class", monster.className),
			Pair("Type", monster.type.flashName),
			null,
			Pair("HP", describeAmountPerLevel(
				monster.baseStats[CombatStat.MaxHealth], monster.hpPerLevel, 1
			)),
			Pair("ATK", describeAmountPerLevel(
				monster.baseStats[CombatStat.Attack],
				monster.attackPerLevelNumerator,
				monster.attackPerLevelDenominator,
			)),
			Pair("STR", monster.baseStats[CombatStat.Strength]?.toString() ?: "0"),
			Pair("VIT", monster.baseStats[CombatStat.Vitality]?.toString() ?: "0"),
			Pair("SPR", monster.baseStats[CombatStat.Spirit]?.toString() ?: "0"),
			Pair("AGL", monster.baseStats[CombatStat.Agility]?.toString() ?: "0"),
			null,
			// Dividing by 2 is needed to match the vanilla EXP.
			// - In vanilla, the EXP gained is 2 * monster.EXP * ...
			// - In this engine, the EXP gained is monster.EXP * ..., where the EXP is multiplied by 2 beforehand
			Pair("EXP", (monster.experience / 2).toString()),
		)

		val propertyPairs2 = arrayOf(
			null, null, null, null, null,
			Pair("MP", monster.baseStats[CombatStat.MaxMana]?.toString() ?: "0"),
			Pair("DEF", monster.baseStats[CombatStat.MeleeDefense]?.toString() ?: "0"),
			Pair("MDEF", monster.baseStats[CombatStat.RangedDefense]?.toString() ?: "0"),
			Pair("EVA", monster.baseStats[CombatStat.Evasion]?.let { "$it%" } ?: "0%"),
		)

		fun renderPropertyPairs(propertyPairs: Array<Pair<String, String>?>, valueX: Float) {
			for ((propertyIndex, rawPair) in propertyPairs.withIndex()) {
				if (rawPair == null) continue
				val (propertyName, propertyValue) = rawPair

				val textHeight = 0.025f * region.height
				val textY = region.minY + 0.13f * region.height + propertyIndex * 0.044f * region.height
				textBatch.drawString(
					"$propertyName:", valueX - 0.025f * region.height, textY,
					textHeight, baseFont, baseTextColor, TextAlignment.RIGHT,
				)
				textBatch.drawString(
					propertyValue, valueX, textY, textHeight, baseFont, fatTextColor
				)
			}
		}

		renderPropertyPairs(propertyPairs1, splitX)
		renderPropertyPairs(propertyPairs2, splitX + 0.255f * region.height)

		val animationContext = AnimationContext(
			renderRegion = region,
			renderTime = System.nanoTime(),
			magicScale = monster.animations.skeleton.magicScale,
			parentMatrix = Matrix3x2f().translate(
				region.minX + 0.44f * region.height,
				region.minY + 0.49f * region.height,
			).scale(0.0025f * region.height),
			parentColorTransform = null,
			partBatch = animationPartBatch,
			noMask = context.content.battle.noMask,
			combat = CombatantAnimationContext(
				isSelectedTarget = false,
				isSelectingMove = false,
				meleeElement = null,
				magicElement = null,
				isMoving = false,
				rootSkin = monster.animations.skin,
				weaponName = null,
				shieldName = null,
				renderInfo = CombatantRenderInfo()
			),
			portrait = null,
		)

		val animation = monster.animations["idle"]
		val relativeTime = System.nanoTime() % animation.duration.inWholeNanoseconds
		renderCombatantAnimation(
			animation,
			monster.animations.skeleton.earlyFlatNodes,
			monster.animations.skeleton.lateFlatNodes,
			relativeTime, animationContext
		)

		for (lootIndex in 0 until 4) {
			val baseX = region.maxX - (0.455f * region.height).roundToInt()
			val baseY = region.minY + ((0.6f + lootIndex * 0.076f) * region.height).roundToInt()
			val backgroundHeight = (0.046f * region.height).roundToInt()
			val radius = 0.5f * backgroundHeight
			val leftBackgroundColor = srgbToLinear(rgb(74, 46, 26))
			val rightBackgroundColor = srgbToLinear(rgb(90, 55, 35))
			colorBatch.gradient(
				baseX, baseY, region.maxX, baseY + backgroundHeight - 1,
				leftBackgroundColor, rightBackgroundColor, leftBackgroundColor,
			)
			ovalBatch.antiAliased(
				baseX - radius.toInt() - 2, baseY, baseX - 1, baseY + backgroundHeight - 1,
				baseX.toFloat(), baseY + radius, radius, radius,
				0.1f, leftBackgroundColor,
			)

			val loot = if (lootIndex < monster.loot.size) monster.loot[lootIndex] else continue
			if (loot.item == null) continue

			spriteBatch.simple(
				baseX - (0.005f * region.height).roundToInt(),
				baseY - (0.008f * region.height).roundToInt(),
				0.0039f * region.height, loot.item!!.sprite.index,
			)
			textBatch.drawShadowedString(
				loot.item!!.displayName, baseX + 0.08f * region.height, baseY + 0.033f * region.height,
				0.022f * region.height, baseFont, baseTextColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			textBatch.drawShadowedString(
				"${loot.chance}%", baseX + 0.375f * region.height, baseY + 0.033f * region.height,
				0.022f * region.height, baseFont, baseTextColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
		}

		fun renderResistance(icon: BcSprite, resistance: Int, index: Int, baseMinY: Int) {
			if (index >= 10) return

			val baseMinX = region.minX + ((0.0306f + 0.14f * index) * region.height).roundToInt()
			val baseMaxX = baseMinX + (0.102f * region.height).roundToInt()
			val baseMaxY = baseMinY + (0.03f * region.height).roundToInt()
			val radius = (1 + baseMaxY - baseMinY) * 0.5f

			val (backgroundColor, textColor, displayResistance) = if (resistance > 100) {
				Triple(
					srgbToLinear(rgb(67, 78, 26)),
					srgbToLinear(rgb(152, 255, 102)),
					resistance
				)
			} else if (resistance > 0) {
				Triple(
					srgbToLinear(rgb(62, 60, 75)),
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
					srgbToLinear(rgb(80, 37, 24)),
					srgbToLinear(rgb(255, 102, 102)),
					-resistance
				)
			}

			colorBatch.fill(baseMinX, baseMinY, baseMaxX, baseMaxY, backgroundColor)
			ovalBatch.antiAliased(
				baseMinX - radius.roundToInt() - 2, baseMinY, baseMinX - 1, baseMaxY,
				baseMinX.toFloat(), baseMinY + radius, radius, radius,
				0.1f, backgroundColor,
			)
			ovalBatch.antiAliased(
				baseMaxX + 1, baseMinY, baseMaxX + radius.roundToInt() + 2, baseMaxY,
				baseMaxX.toFloat(), baseMinY + radius, radius, radius,
				0.1f, backgroundColor,
			)

			imageBatch.coloredScale(
				baseMinX - 1.1f * radius, baseMinY - radius * 0.5f,
				radius * 3f / icon.height, icon.index, 0,
				if (resistance == 0) rgba(1f, 1f, 1f, 0.05f) else -1,
			)

			if (textColor != 0) {
				textBatch.drawShadowedString(
					"$displayResistance%", baseMaxX + 0.007f * region.height,
					baseMinY + 0.0235f * region.height,  0.019f * region.height, baseFont,
					textColor, 0, 0f, shadowColor,
					shadowOffset, shadowOffset, TextAlignment.RIGHT,
				)
			}
		}

		for ((index, element) in context.content.stats.elements.withIndex()) {
			if (index >= 10) break

			val baseMinY = region.maxY - (0.106f * region.height).roundToInt()
			renderResistance(
				element.thickSprite,
				(monster.resistances.get(element) * 100f).roundToInt(),
				index, baseMinY,
			)
		}

		for ((index, effect) in context.content.stats.statusEffects.withIndex()) {
			if (index >= 10) break

			val baseMinY = region.maxY - (0.049f * region.height).roundToInt()
			renderResistance(effect.icon, monster.resistances.get(effect), index, baseMinY)
		}
	}
}

private fun renderList(
	menuContext: MenuRenderContext,
	entries: List<EncyclopediaSnapshot.OptionalEntry<ListEntry>>,
	region: Rectangle
) {
	menuContext.run {
		val tab = menu.currentTab as EncyclopediaTab
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)

		val sectionHeight = (0.035f * region.height).roundToInt()
		val sectionWidth = (0.43f * region.height).roundToInt()
		val startX = region.minX + 0.05f * region.height
		val startY = region.minY + 0.12f * region.height
		val verticalSpacing = 0.06f * region.height
		val horizontalSpacing = 0.5f * region.height // TODO CHAP2 Fine-tune horizontal spacing

		val numRows = 14
		tab.numRenderedRows = numRows

		var currentRow = 0
		var currentColumn = 0
		for ((index, entry) in entries.withIndex()) {
			val baseX = (startX + currentColumn * horizontalSpacing).roundToInt()
			val baseY = (startY + currentRow * verticalSpacing).roundToInt()
			val leftBackgroundColor = if (index == tab.currentEntry) {
				srgbToLinear(rgb(37 ,27, 50))
			} else {
				srgbToLinear(rgb(37, 27, 16))
			}
			val rightBackgroundColor = changeAlpha(leftBackgroundColor, 50)

			colorBatch.gradient(
				baseX, baseY, baseX + sectionWidth - 1, baseY + sectionHeight - 1,
				leftBackgroundColor, rightBackgroundColor, leftBackgroundColor,
			)

			val radius = sectionHeight * 0.5f
			ovalBatch.antiAliased(
				(baseX - radius - 1).toInt(), baseY, baseX - 1, baseY + sectionHeight - 1,
				baseX.toFloat(), baseY + radius, radius, radius,
				0.1f, leftBackgroundColor,
			)
			ovalBatch.antiAliased(
				baseX + sectionWidth, baseY,
				baseX + sectionWidth + radius.roundToInt() + 1, baseY + sectionHeight - 1,
				baseX + sectionWidth.toFloat(), baseY + radius, radius, radius,
				0.1f, rightBackgroundColor
			)

			val textColor = if (tab.currentEntry == index) {
				srgbToLinear(rgb(164, 204, 253))
			} else if (entry.entry == null) {
				srgbToLinear(rgb(102, 96, 79))
			} else {
				srgbToLinear(rgb(238, 203, 127))
			}

			val textX = baseX + 1f * sectionHeight
			val textY = baseY + 0.85f * sectionHeight
			val textHeight = 0.75f * sectionHeight
			val shadowColor = changeAlpha(0, 50)
			val shadowSize = 0f * textHeight // TODO CHAP1 Revisit after text render refactor

			val nameString = "${index + 1}) ${entry.entry?.name ?: "-".repeat(entry.lengthOfName!!)}"
			textBatch.drawString(
				nameString, textX, textY,
				textHeight, baseFont, textColor,
				shadowColor, shadowSize, TextAlignment.LEFT,
			)

			if (entry.entry != null) {
				if (entry.entry!!.element != null) {
					val icon = entry.entry!!.element!!.thickSprite
					imageBatch.simpleScale(
						baseX - 1.2f * radius, baseY - 0.2f * radius,
						2.5f * radius / icon.height, icon.index,
					)
				}

				if (entry.amount != null) {
					textBatch.drawString(
						entry.amount.toString(), baseX + 0.9f * sectionWidth, textY,
						textHeight, baseFont, textColor,
						shadowColor, shadowSize, TextAlignment.RIGHT,
					)
				}
			}

			if (tab.currentEntry == index) {
				val pointer = context.content.ui.pointer
				imageBatch.simpleScale(
					baseX - 4f * radius + 0.2f * sectionHeight * determinePointerOffset(),
					baseY - 0.2f * radius,
					0.9f * sectionHeight / pointer.height, pointer.index,
				)
			}

			currentRow += 1
			if (currentRow >= numRows) {
				currentRow = 0
				currentColumn += 1
			}
		}
	}
}

private fun renderOutside(tab: EncyclopediaTab, menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.apply {
		val baseFont = context.bundle.getFont(context.content.fonts.basic2.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val shadowColor = srgbToLinear(rgb(90, 52, 22))
		val shadowOffset = 0.003f * region.height
		var lineY = region.minY + 0.075f * region.height
		val description = "This section contains optional reading about the world that this game takes place in. " +
				"It doesn't really serve any purpose except for providing background information. " +
				"Every time you hear about a new person or place, or see them, their information is added here.\\n" +
				"\\nThere are several sections:"

		fun drawLine(line: String) {
			textBatch.drawShadowedString(
				line, region.minX + 0.05f * region.height, lineY,
				0.025f * region.height, baseFont, baseTextColor, 0, 0f,
				shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT,
			)
			lineY += 0.045f * region.height
		}

		renderDescription(description, 55 * region.width / region.height, ::drawLine)

		class Section(
			val title: String,
			val description: String,
			val sprite: KimSprite,
		)

		var sections = arrayOf(
			Section(
				"People",
				"A list of all the player characters, and important NPCs you've talked to.",
				context.content.ui.encyclopediaPeople,
			),
			Section(
				"Places",
				"All the places you've visisted in your travels are listed here.",
				context.content.ui.encyclopediaPlaces,
			),
			Section(
				"Artefacts",
				"Some rare MacGuffins that people talk about will be added to this section; " +
						"usually plot-related items.",
				context.content.ui.encyclopediaArtefacts,
			),
			Section(
				"Bestiary",
				"Whenever you defeat a monster, its information is added here." +
						"Contains stats and weaknesses/resistances, as well as descriptions " +
						"for each monster and a number showing how many of that type you've defeated.",
				context.content.ui.encyclopediaBestiary,
			),
			Section(
				"Dreamstones",
				"There are some special stones that exist in the world which can contain memories," +
						"thoughts or dreams... Viewing them gives you a glimpse into someone's mind in the past.",
				context.content.ui.encyclopediaDreamstones,
			)
		)

		// TODO CHAP3 Don't do this in chapter 3
		sections = sections.slice(0 until 4).toTypedArray()

		val arrowIcon = context.content.ui.arrowHead
		val arrowOffset = determinePointerOffset() * region.height

		val spriteBatch = context.addAreaSpriteBatch(10, region)
		val arrowY = lineY + 0.15f * region.height
		val arrowScale = 0.045f * region.height / arrowIcon.height
		imageBatch.rotated(
			region.minX + 0.1f * region.height - arrowOffset * 0.008f, arrowY, 180f, arrowScale,
			arrowIcon.index, 0, -1,
		)
		imageBatch.rotated(
			region.minX + (0.2f + 0.16f * sections.size) * region.height + arrowOffset * 0.008f,
			arrowY, 0f, arrowScale, arrowIcon.index, 0, -1,
		)

		for ((index, section) in sections.withIndex()) {
			val iconX = region.minX + ((0.18f + 0.16f * index) * region.height).roundToInt()
			val opacity = if (index == tab.currentSection) 1f else 0.2f
			spriteBatch.draw(
				section.sprite, iconX, (lineY + 0.1f * region.height).roundToInt(),
				0.1f * region.height / section.sprite.height, opacity = opacity,
			)

			if (index == tab.currentSection) {
				val pointer = context.content.ui.pointer
				imageBatch.rotated(
					iconX + 0.05f * region.height, lineY + 0.08f * region.height - arrowOffset * 0.007f,
					270f, 0.033f * region.height / pointer.height,
					pointer.index, 0, -1,
				)
			}
		}

		val section = sections[tab.currentSection]
		val titleFont = context.bundle.getFont(context.content.fonts.large1.index)
		val titleShadowOffset = 0.004f * region.height
		textBatch.drawShadowedString(
			section.title, region.minX + 0.05f * region.height, lineY + 0.3f * region.height,
			0.04f * region.height, titleFont,
			srgbToLinear(rgb(222, 166, 83)), 0, 0f,
			srgbToLinear(rgb(124, 77, 35)),
			titleShadowOffset, titleShadowOffset, TextAlignment.LEFT,
		)

		lineY += 0.37f * region.height
		renderDescription(section.description, 55 * region.width / region.height, ::drawLine)
	}
}
