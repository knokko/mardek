package mardek.renderer.menu

import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.util.Rectangle

internal fun renderStatusTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.apply {
		val labelStyle = MardekTextStyles.STRONG_TEXT_FILL.only()
		val valueStyle = MardekTextStyles.StatusTab.VALUE_STYLE
		val labelX = region.minX + 0.38f * region.width
		val valueX = region.minX + 0.43f * region.width

		val stats = state.statistics
		val playerStats = state.characterStates.values.map { it.performance }
		val values = arrayOf(
			Pair("Steps:", stats.totalSteps),
			Pair("Gold Earned:", stats.goldEarned),
			Pair("Damage Inflicted:", playerStats.sumOf { it.damageDealt }),
			Pair("Damage Received:", playerStats.sumOf { it.damageReceived }),
			Pair("Monsters Killed:", stats.numKills),
			Pair("Allies KO'd:", playerStats.sumOf { it.numFaints }),
			Pair("Physical Skills Used:", playerStats.sumOf { it.numMeleeAttacks }),
			Pair("Magical Skills Used:", playerStats.sumOf { it.numMagicSkills }),
			Pair("Items Used:", stats.itemsConsumed),
			Pair("Battles:", stats.battlesWon),
			Pair("Fled:", stats.battlesFled),
			Pair("Chests Opened:", state.openedChests.size),
		)

		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		for ((index, valuePair) in values.withIndex()) {
			val (label, value) = valuePair
			val y = region.minY + (0.12f + index * 0.064f) * region.height
			simpleTextBatch.drawString(
				label, labelX, y, 0.025f * region.height,
				font, labelStyle, TextAlignment.RIGHT,
			)
			simpleTextBatch.drawString(
				value.toString(), valueX, y - 0.001f * region.height,
				0.027f * region.height, font, valueStyle, TextAlignment.LEFT,
			)
		}
	}
}
