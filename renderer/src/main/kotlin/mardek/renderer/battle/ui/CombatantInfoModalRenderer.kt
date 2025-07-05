package mardek.renderer.battle.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import mardek.content.sprite.KimSprite
import mardek.content.stats.CombatStat
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
import mardek.renderer.battle.BattleRenderContext
import mardek.renderer.battle.creature.SingleCreatureRenderer
import mardek.renderer.ui.ResourceBarRenderer
import mardek.renderer.ui.ResourceType
import mardek.state.title.AbsoluteRectangle
import java.util.Locale
import kotlin.math.roundToInt

class CombatantInfoModalRenderer(private val context: BattleRenderContext) {

	private val combatant = context.battle.openCombatantInfo
	private val width = context.viewportWidth
	private val height = context.viewportHeight

	private val x1 = width / 30
	private val x2 = width / 4
	private val x3 = width / 2
	private val x4 = x3 + height / 9
	private val y1 = height / 10
	private val y2 = y1 + height / 9
	private val maxY = height - height / 4
	private val y3 = maxY - height / 9

	private lateinit var batch1: KimBatch
	private lateinit var batch2: KimBatch

	fun beforeRendering() {
		if (combatant == null) return

		batch1 = context.resources.kim1Renderer.startBatch()
		batch2 = context.resources.kim2Renderer.startBatch()

		// Render element icon
		run {
			val radius = height / 20
			val sprite = combatant!!.element.sprite
			batch2.requests.add(KimRequest(
				x = x2 - radius, y = y1 + radius / 3,
				scale = 2f * radius / sprite.width, sprite = sprite
			))
		}

		// Render equipment
		run {
			val baseY = y1 + 15 * height / 100
			val baseX = x2 + 54 * height / 100
			val offsetY = height / 15
			val spriteHeight = height / 16f
			for ((index, item) in combatant!!.getEquipment(context.updateContext).withIndex()) {
				if (item != null) batch1.requests.add(KimRequest(
					x = baseX, y = baseY + index * offsetY,
					scale = spriteHeight / item.sprite.height, sprite = item.sprite
				))
			}
		}

		fun renderResistanceIcon(batch: KimBatch, sprite: KimSprite, percentage: Int, minY: Int, column: Int) {
			batch.requests.add(KimRequest(
				x = x2 + height / 25 + column * height / 10, y = minY,
				scale = height / (30f * sprite.height), sprite = sprite,
				opacity = if (percentage == 0) 0.1f else 1f
			))
		}

		for ((column, name) in arrayOf(
			"FIRE", "WATER", "AIR", "EARTH", "LIGHT", "DARK", "AETHER", "FIG", "PHYSICAL", "THAUMA"
		).withIndex()) {
			val element = context.content.stats.elements.find { it.properName == name }!!
			val percentage = (100f * combatant.getResistance(element, context.updateContext)).roundToInt()
			renderResistanceIcon(batch2, element.sprite, percentage, y3 + height / 55, column)
		}

		for ((column, name) in arrayOf(
			"PSN", "PAR", "NUM", "SIL", "CRS", "DRK", "CNF", "SLP", "ZOM", "BLD"
		).withIndex()) {
			val effect = context.content.stats.statusEffects.find { it.flashName == name }!!
			val percentage = combatant.getResistance(effect, context.updateContext)
			renderResistanceIcon(batch1, effect.icon, percentage, y3 + 2 * height / 31, column)
		}
	}

	fun render() {
		if (combatant == null) return

		val rectangles = context.resources.rectangleRenderer
		rectangles.beginBatch(context, 5)

		// Render thin left bar
		run {
			val upColor = srgbToLinear(rgba(40, 25, 10, 250))
			val downColor = srgbToLinear(rgba(50, 30, 15, 250))
			rectangles.gradient(
				0, y1, x1, maxY,
				downColor, downColor, upColor
			)
		}

		// Render thick dark left bar
		rectangles.fill(
			x1, y1, x2, maxY,
			srgbToLinear(rgb(24, 14, 10))
		)

		// Render main background
		run {
			val rightColor = srgbToLinear(rgba(99, 81, 49, 250))
			val leftColor = srgbToLinear(rgba(50, 40, 25, 230))
			rectangles.gradient(
				x2, y1, width - 1, y3,
				leftColor, rightColor, leftColor
			)
		}

		// Render background of upper bar & lower bar
		run {
			val leftColor = srgbToLinear(rgba(55, 35, 15, 240))
			val rightColor = srgbToLinear(rgba(90, 60, 20, 240))
			rectangles.gradientUnaligned(
				x3, y1, leftColor,
				x4, y2, leftColor,
				width, y2, rightColor,
				width, y1, rightColor,
			)
			rectangles.gradient(
				x2, y3, width - 1, maxY,
				leftColor, rightColor, leftColor
			)
		}

		rectangles.endBatch(context.recorder)
		context.uiRenderer.beginBatch()

		// Render health
		val darkTextColor = srgbToLinear(rgb(149, 107, 62))
		val greenTextColor = srgbToLinear(rgb(102, 255, 0))
		run {
			val baseY = y1 + height / 20
			context.uiRenderer.drawString(
				context.resources.font, "HP:", darkTextColor, IntArray(0),
				x4 + height / 50, y1, width, y2, baseY,
				height / 45, 1, TextAlignment.LEFT
			)
			ResourceBarRenderer(
				context, ResourceType.Health, AbsoluteRectangle(
					x4 + height / 11, baseY - height / 50, 2 * height / 9, height / 50
				)
			).renderBar(combatant!!.currentHealth, combatant.maxHealth)
			context.uiRenderer.drawString(
				context.resources.font, "${combatant.currentHealth}/${combatant.maxHealth}",
				greenTextColor, IntArray(0), x4 + height / 3, y1, width, y2,
				baseY, height / 50, 1, TextAlignment.LEFT
			)
		}

		// Render mana
		val blueTextColor = srgbToLinear(rgb(85, 237, 255))
		run {
			val baseY = y1 + height / 10
			context.uiRenderer.drawString(
				context.resources.font, "MP:", darkTextColor, IntArray(0),
				x4 + height / 50, y1, width, y2, baseY,
				height / 45, 1, TextAlignment.LEFT
			)
			ResourceBarRenderer(
				context, ResourceType.Mana, AbsoluteRectangle(
					x4 + height / 11, baseY - height / 50, 2 * height / 9, height / 50
				)
			).renderBar(combatant!!.currentMana, combatant.maxMana)
			context.uiRenderer.drawString(
				context.resources.font, "${combatant.currentMana}/${combatant.maxMana}", blueTextColor,
				IntArray(0), x4 + height / 3, y1, width, y2,
				baseY, height / 50, 1, TextAlignment.LEFT
			)
		}

		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val strongTextColor = srgbToLinear(rgb(255, 203, 51))
		val redTextColor = srgbToLinear(rgb(255, 169, 169))

		fun renderStatRow(stat: CombatStat, baseY: Int) {
			val heightA = height / 45
			val textX1 = x2 + 2 * height / 9
			context.uiRenderer.drawString(
				context.resources.font, stat.flashName.uppercase(Locale.ROOT), darkTextColor,
				IntArray(0), x2, y1, textX1, maxY,
				baseY, heightA, 1, TextAlignment.RIGHT
			)

			val textX2 = x2 + height / 3
			var extra = combatant.getStat(stat, context.updateContext) - combatant.getNatural(stat)
			run {
				val equipment = combatant!!.getEquipment(context.updateContext)
				val weapon = equipment[0]?.equipment
				if (stat == CombatStat.Attack && weapon != null) extra -= weapon.getStat(stat)
				if (stat == CombatStat.MeleeDefense || stat == CombatStat.RangedDefense) {
					for (potentialArmor in equipment) {
						val armor = potentialArmor?.equipment
						if (armor != null) extra -= armor.getStat(stat)
					}
				}
			}

			context.uiRenderer.drawString(
				context.resources.font,
				"${combatant.getStat(stat, context.updateContext)}${if (stat == CombatStat.Evasion) "%" else ""}",
				if (extra == 0) baseTextColor else strongTextColor, IntArray(0),
				textX1, y1, textX2, maxY,
				baseY, heightA, 1, TextAlignment.CENTER
			)

			if (extra != 0) {
				context.uiRenderer.drawString(
					context.resources.font, "${if (extra > 0) "+" else ""}$extra",
					if (extra > 0) greenTextColor else redTextColor, IntArray(0),
					textX2, y1, x4, maxY,
					baseY, heightA, 1, TextAlignment.LEFT
				)
			}
		}

		// Render stats
		run {
			val statY1 = y1 + 2 * height / 9
			val statOffsetY = height / 25
			renderStatRow(CombatStat.Strength, statY1)
			renderStatRow(CombatStat.Vitality, statY1 + statOffsetY)
			renderStatRow(CombatStat.Spirit, statY1 + 2 * statOffsetY)
			renderStatRow(CombatStat.Agility, statY1 + 3 * statOffsetY)

			val statY2 = y1 + 4 * height / 10
			renderStatRow(CombatStat.Attack, statY2)
			renderStatRow(CombatStat.MeleeDefense, statY2 + statOffsetY)
			renderStatRow(CombatStat.RangedDefense, statY2 + 2 * statOffsetY)
			renderStatRow(CombatStat.Evasion, statY2 + 3 * statOffsetY)
		}

		// Render equipment names
		run {
			val baseY = y1 + 19 * height / 100
			val baseX = x2 + 63 * height / 100
			val textHeight = height / 45
			val offsetY = height / 15
			for ((index, item) in combatant!!.getEquipment(context.updateContext).withIndex()) {
				context.uiRenderer.drawString(
					context.resources.font, item?.flashName ?: "-", baseTextColor,
					IntArray(0), baseX, y1, width, maxY,
					baseY + index * offsetY, textHeight, 1, TextAlignment.LEFT
				)
			}
		}

		// Render name, level, and class
		run {
			val baseX = x2 + height / 20
			val textHeight = height / 45
			val baseY = y1 + height / 15
			val lineGap = height / 20
			context.uiRenderer.drawString(
				context.resources.font, combatant!!.getName(), baseTextColor, IntArray(0),
				baseX, y1, width, maxY, baseY,
				textHeight, 1, TextAlignment.LEFT
			)
			context.uiRenderer.drawString(
				context.resources.font,
				"Level ${combatant.getLevel(context.updateContext)} ${combatant.getClassName()}",
				baseTextColor, IntArray(0), baseX, y1, width, maxY,
				baseY + lineGap, textHeight, 1, TextAlignment.LEFT
			)
			context.uiRenderer.drawString(
				context.resources.font, combatant.getCreatureType().flashName, baseTextColor,
				IntArray(0), baseX, y1, width, maxY,
				baseY + 2 * lineGap, textHeight, 1, TextAlignment.LEFT
			)
		}

		fun renderResistance(percentage: Int, baseY: Int, column: Int) {
			if (percentage == 0) return
			val (color, value) = if (percentage < 0) Pair(redTextColor, -percentage)
			else if (percentage > 100) Pair(greenTextColor, percentage - 100)
			else Pair(blueTextColor, percentage)
			context.uiRenderer.drawString(
				context.resources.font, value.toString(), color, IntArray(0),
				x2 + height / 13 + column * height / 10, y3, width, maxY,
				baseY, height / 60, 1, TextAlignment.LEFT
			)
		}

		fun renderElementalResistance(name: String, column: Int) {
			val element = context.content.stats.elements.find { it.properName == name }!!
			renderResistance(
				(100f * combatant.getResistance(element, context.updateContext)).roundToInt(),
				y3 + height / 22, column
			)
		}

		for ((column, element) in arrayOf(
			"FIRE", "WATER", "AIR", "EARTH", "LIGHT", "DARK", "AETHER", "FIG", "PHYSICAL", "THAUMA"
		).withIndex()) renderElementalResistance(element, column)

		fun renderStatusResistance(name: String, column: Int) {
			val effect = context.content.stats.statusEffects.find { it.flashName == name }!!
			renderResistance(
				combatant.getResistance(effect, context.updateContext),
				y3 + height / 11, column
			)
		}

		for ((column, effect) in arrayOf(
			"PSN", "PAR", "NUM", "SIL", "CRS", "DRK", "CNF", "SLP", "ZOM", "BLD"
		).withIndex()) renderStatusResistance(effect, column)

		context.uiRenderer.drawString(
			context.resources.font, "RESISTANCES", darkTextColor, IntArray(0),
			x2, y2, width - height / 35, maxY,
			y3 + height / 130, height / 60, 1, TextAlignment.RIGHT
		)

		context.uiRenderer.endBatch()

		context.resources.kim1Renderer.submit(batch1, context)
		context.resources.kim2Renderer.submit(batch2, context)

		context.resources.partRenderer.startBatch(context.recorder)
		SingleCreatureRenderer(context, combatant, true).render()
		context.resources.partRenderer.endBatch()
	}
}
