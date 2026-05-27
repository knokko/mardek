package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.text.Vk2dFancyTextStyle
import com.github.knokko.vk2d.text.Vk2dTextStyle
import mardek.renderer.util.ResourceColorEntry

object MardekTextStyles {

	val WEAK_TEXT_FILL = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(207, 192, 141))
	)
	val STRONG_TEXT_FILL = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(238, 203, 127))
	)
	val BRIGHT_TEXT_FILL = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(255, 225, 127))
	)
	val SHADOW_FILL = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(61, 35, 18))
	)
	val SHADOW_FILL2 = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(48, 35, 23))
	)
	val SHADOW_FILL3 = Vk2dTextStyle.FillStyle(
		srgbToLinear(rgb(90, 52, 22))
	)
	val BLACK_FILL = Vk2dTextStyle.FillStyle(rgb(0, 0, 0))

	val CHEST_TITLE_BACK = run {
		val strokeColor = srgbToLinear(rgb(132, 81, 37))
		Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient.plain(0), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient(
				strokeColor, strokeColor, 0, 0, 0,
				0.125f, 0.14f, 12345f, 12345f,
			), true,
		)
	}

	val CHEST_TITLE_FRONT = run {
		val lowColor = srgbToLinear(rgb(204, 153, 0))
		val highColor = srgbToLinear(rgb(255, 204, 102))
		Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				lowColor, lowColor, highColor, highColor, highColor,
				0.5f, 0.5f, 0.5f, 0.5f
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient.plain(0), true,
		)
	}

	fun masteredBack1(alpha: Int): Vk2dFancyTextStyle {
		val middleColor = srgbToLinear(rgba(255, 147, 26, alpha))

		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				0, 0, middleColor, middleColor, 0,
				0.34f, 0.34f, 0.58f, 0.58f,
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient.plain(0), true,
		)
	}

	fun masteredBack2(alpha: Int): Vk2dFancyTextStyle {
		val quarterColor = srgbToLinear(rgba(255, 81, 26, alpha))
		val outerColor = srgbToLinear(rgba(213, 0, 0, alpha))

		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				0, 0, quarterColor, quarterColor, outerColor,
				0.58f, 0.58f, 0.78f, 0.78f,
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient.plain(0), true,
		)
	}

	fun masteredFront(alpha: Int): Vk2dFancyTextStyle {
		val outerColor = srgbToLinear(rgba(213, 0, 0, alpha))
		val quarterColor = srgbToLinear(rgba(255, 81, 26, alpha))
		val strokeColor = srgbToLinear(rgba(255, 255, 153, alpha))

		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				outerColor, outerColor, quarterColor, quarterColor, 0,
				0.17f, 0.17f, 0.34f, 0.34f,
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient(
				strokeColor, 0, 0, 0, 0,
				0.07f, 12345f, 12345f, 12345f
			), true,
		)
	}

	fun button(lowerTextColor: Int, upperTextColor: Int, showOutline: Boolean): Vk2dFancyTextStyle {
		val stroke = if (showOutline) {
			val outlineColor = rgba(0, 0, 0, 200)
			Vk2dFancyTextStyle.Gradient(
				outlineColor, 0, 0, 0, 0,
				0.075f, 12345f, 12345f, 12345f,
			)
		} else Vk2dFancyTextStyle.Gradient.plain(0)

		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				lowerTextColor, lowerTextColor,
				upperTextColor, upperTextColor, upperTextColor,
				0.5f, 0.5f, 0.5f, 0.5f
			), 1f, 0f,
			stroke, stroke, true,
		)
	}

	object TitleScreen {
		val TITLE_BACK = run {
			val outerBorderColor = srgbToLinear(rgb(190, 144, 95))
			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient.plain(0), 1f, 0.01f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient(
					0, 0, outerBorderColor, outerBorderColor, 0,
					0.06f, 0.065f, 0.1f, 0.105f
				), false
			)
		}
		val TITLE_FRONT = run {
			val outerColor = srgbToLinear(rgb(107, 53, 4))
			val quarterColor = srgbToLinear(rgb(185, 93, 68))
			val middleColor = srgbToLinear(rgb(230, 187, 178))
			val innerBorderColor = srgbToLinear(rgb(68, 51, 34))
			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					outerColor, quarterColor, middleColor, quarterColor, outerColor,
					0.3f, 0.4f, 0.5f, 1f
				), 1f, 0.01f,
				Vk2dFancyTextStyle.Gradient(
					innerBorderColor, 0, 0, 0, 0,
					0.01f, 12345f, 12345f, 12345f,
				),
				Vk2dFancyTextStyle.Gradient(
					0, innerBorderColor, innerBorderColor, 0, 0,
					0f, 0.06f, 0.065f, 12345f,
				),
				false
			)
		}

		val SUB_TITLE = run {
			val shadowColor = srgbToLinear(rgb(91, 63, 30))
			val lowerColor = srgbToLinear(rgb(184, 130, 60))
			val upperColor = srgbToLinear(rgb(241, 182, 113))
			Vk2dFancyTextStyle.Shadowed(
				Vk2dFancyTextStyle(
					Vk2dFancyTextStyle.Gradient(
						lowerColor, lowerColor, upperColor, upperColor, upperColor,
						0.3f, 0.3f, 12345f, 12345f,
					), 1f, 0f,
					Vk2dFancyTextStyle.Gradient.plain(0),
					Vk2dFancyTextStyle.Gradient.plain(0), false,
				),
				Vk2dFancyTextStyle(
					Vk2dFancyTextStyle.Gradient.plain(shadowColor), 1f, 0f,
					Vk2dFancyTextStyle.Gradient.plain(0),
					Vk2dFancyTextStyle.Gradient.plain(0), false,
				),
				0.075f,
			)
		}

		val GAME_NAME_LABEL = Vk2dTextStyle.Shadowed(
			STRONG_TEXT_FILL.only(), SHADOW_FILL.only(), 0.1f
		)

		val GAME_NAME = Vk2dTextStyle(
			Vk2dTextStyle.FillStyle(srgbToLinear(rgb(255, 203, 152))),
			Vk2dTextStyle.StrokeStyle.NONE,
		)

		val GAME_OVER_BACK1 = run {
			val middleColor = srgbToLinear(rgb(178, 37, 37))

			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					0, 0, middleColor, middleColor, 0,
					0.34f, 0.34f, 0.58f, 0.58f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient.plain(0), true,
			)
		}

		val GAME_OVER_BACK2 = run {
			val quarterColor = srgbToLinear(rgb(154, 1, 1))
			val outerColor = srgbToLinear(rgb(107, 0, 0))

			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					0, 0, quarterColor, quarterColor, outerColor,
					0.58f, 0.58f, 0.78f, 0.78f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient.plain(0), true,
			)
		}

		val GAME_OVER_FRONT = run {
			val outerColor = srgbToLinear(rgb(107, 0, 0))
			val quarterColor = srgbToLinear(rgb(154, 1, 1))
			val strokeColor = srgbToLinear(rgb(36, 0, 0))

			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					outerColor, outerColor, quarterColor, quarterColor, 0,
					0.17f, 0.17f, 0.34f, 0.34f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient(
					strokeColor, strokeColor, 0, 0, 0,
					0.05f, 0.08f, 12345f, 12345f
				), true,
			)
		}
	}

	fun resourceBarWithoutShadow(entry: ResourceColorEntry, opacity: Float) = Vk2dTextStyle(
		Vk2dTextStyle.FillStyle(multiplyAlpha(entry.textColor, opacity)),
		Vk2dTextStyle.StrokeStyle(
			rgb(0, 0, 0), 0.1f, true, 0.75f
		)
	)

	private fun resourceBarWithShadow(entry: ResourceColorEntry, opacity: Float) = Vk2dTextStyle(
		Vk2dTextStyle.FillStyle(multiplyAlpha(entry.textColor, opacity)),
		Vk2dTextStyle.StrokeStyle.NONE,
	)

	private fun resourceBarShadow(entry: ResourceColorEntry, opacity: Float) = Vk2dTextStyle(
		Vk2dTextStyle.FillStyle(multiplyAlpha(entry.shadowColor, opacity)),
		Vk2dTextStyle.StrokeStyle.NONE,
	)

	fun resourceBarInventory(entry: ResourceColorEntry) = Vk2dTextStyle.Shadowed(
		resourceBarWithShadow(entry, 1f),
		resourceBarShadow(entry, 1f),
		0.2f
	)

	fun resourceBarWithShadowed(entry: ResourceColorEntry, opacity: Float) = Vk2dTextStyle.Shadowed(
		resourceBarWithShadow(entry, opacity),
		resourceBarShadow(entry, opacity),
		0.1f,
	)

	fun menuSection(selected: Boolean): Vk2dFancyTextStyle {
		val lowTextColor: Int
		val highTextColor: Int
		if (selected) {
			lowTextColor = srgbToLinear(rgb(104, 179, 252))
			highTextColor = srgbToLinear(rgb(230, 255, 255))
		} else {
			lowTextColor = srgbToLinear(rgb(214, 170, 98))
			highTextColor = srgbToLinear(rgb(249, 237, 210))
		}

		val strokeColor = rgba(0, 0, 0, 175)
		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				lowTextColor, lowTextColor, highTextColor, highTextColor, highTextColor,
				0.5f, 0.5f, 0.5f, 0.5f,
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient(
				strokeColor, 0, 0, 0, 0,
				0.15f, 12345f, 12345f, 12345f,
			), true,
		)
	}

	object Inventory {
		fun characterBarStats(mainColor: Int, shadowColor: Int) = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle.FillStyle(mainColor).only(),
			Vk2dTextStyle.FillStyle(shadowColor).only(),
			0.17f
		)

		val CHARACTER_BAR_LEVEL = characterBarStats(srgbToLinear(
			rgb(255, 203, 102)
		), SHADOW_FILL.color)
	}

	object Dialogue {
		val BOLD_COLOR = srgbToLinear(rgb(253, 218, 116))
		val BASE_STROKE = Vk2dTextStyle.StrokeStyle(
			rgba(0, 0, 0, 250),
			0.15f, true, 1f,
		)
		val BASE = Vk2dTextStyle(WEAK_TEXT_FILL, BASE_STROKE)
		val BOLD = Vk2dTextStyle(Vk2dTextStyle.FillStyle(BOLD_COLOR), BASE_STROKE)
		val UNSELECTED = Vk2dTextStyle(Vk2dTextStyle.FillStyle(
			srgbToLinear(rgb(68, 68, 68))
		), BASE_STROKE)
		val SELECTED = Vk2dTextStyle(Vk2dTextStyle.FillStyle(
			srgbToLinear(rgb(164, 204, 253))
		), BASE_STROKE)
		val SHADOW = Vk2dTextStyle(
			Vk2dTextStyle.FillStyle(srgbToLinear(rgb(70, 50, 30))),
			Vk2dTextStyle.StrokeStyle(
				srgbToLinear(rgb(70, 50, 30)),
				0.08f, true, 3f,
			)
		)
		val CHAT_LOG_BASE = Vk2dTextStyle(Vk2dTextStyle.FillStyle(
			srgbToLinear(rgb(186, 146, 77))
		), Vk2dTextStyle.StrokeStyle.NONE)
		val CHAT_LOG_BOLD = CHAT_LOG_BASE.withDifferentFillColor(BOLD_COLOR)!!

		fun colored(color: Int) = Vk2dTextStyle(Vk2dTextStyle.FillStyle(color), Vk2dTextStyle.StrokeStyle(
			multiplyAlpha(color, 0.4f), 0.35f, true, 3f,
		))

		val SKIP_BUTTON = run{
			val lowColor = srgbToLinear(rgb(73, 52, 37))
			val highColor = srgbToLinear(rgb(109, 93, 81))
			Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					lowColor, lowColor, highColor, highColor, highColor,
					0.5f, 0.5f, 1f, 1f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient.plain(0), true,
			)
		}
	}

	object ActionBar {
		val ACTION = Vk2dTextStyle(
			STRONG_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
				srgbToLinear(rgba(0, 0, 0, 200)),
				0.09f, true, 0.5f
			)
		)
		val NAME = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle(
				STRONG_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
					srgbToLinear(rgb(0, 0, 0)),
					0.05f, true, 1f
				)
			),
			Vk2dTextStyle(BLACK_FILL, Vk2dTextStyle.StrokeStyle(
				srgbToLinear(rgba(0, 0, 0, 200)),
				0.1f, true, 0.5f
			)), 0.025f,
		)
	}

	val TARGET_SELECTION_BACK = run {
		val strokeColor = srgbToLinear(rgb(180, 154, 110))

		Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient.plain(0), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient(
				strokeColor,strokeColor, 0, 0, 0,
				0.1f, 0.125f, 12345f, 12345f,
			), true,
		)
	}

	val TARGET_SELECTION_FRONT = run {
		val lowTargetingColor = srgbToLinear(rgb(126, 1, 1))
		val highTargetingColor = srgbToLinear(rgb(175, 61, 1))

		Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				lowTargetingColor, lowTargetingColor,
				highTargetingColor, highTargetingColor, highTargetingColor,
				0.5f, 0.5f, 1f, 1f,
			), 1f, 0f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient.plain(0), true,
		)
	}

	val COMBATANT_BLOCK_NAME = Vk2dTextStyle.Shadowed(
		Vk2dTextStyle(STRONG_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
			rgba(0, 0, 0, 250), 0.2f, true, 2f
		)),
		Vk2dTextStyle.FillStyle(
			rgba(0, 0, 0, 200)
		).only(), 0.1f,
	)

	object BattleIndicators {
		fun base(outerColor: Int, innerColor: Int): Vk2dFancyTextStyle {
			val strokeColor = rgb(0, 0, 0)
			return Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					outerColor, outerColor, innerColor, innerColor, outerColor,
					0.2f, 0.2f, 0.8f, 0.8f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient(
					strokeColor, 0, 0, 0, 0,
					0.05f, 12345f, 12345f, 12345f,
				),
				Vk2dFancyTextStyle.Gradient(
					strokeColor, strokeColor, 0, 0, 0,
					0.075f, 0.11f, 12345f, 12345f,
				), true,
			)
		}

		fun miss(alpha: Int) = base(
			srgbToLinear(rgba(180, 150, 104, alpha)),
			srgbToLinear(rgba(229, 219, 208, alpha)),
		)

		fun levelUp(alpha: Int) = base(
			srgbToLinear(rgba(253, 235, 154, alpha)),
			srgbToLinear(rgba(253, 252, 235, alpha)),
		)
	}

	fun victoryBack(strokeColor: Int) : Vk2dFancyTextStyle {
		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient.plain(0), 1f, 0.01f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient(
				strokeColor, strokeColor, 0, 0, 0,
				0.05f, 0.075f, 12345f, 12345f,
			), true,
		)
	}

	fun victoryFront(innerColor: Int, outerColor: Int) : Vk2dFancyTextStyle {
		return Vk2dFancyTextStyle(
			Vk2dFancyTextStyle.Gradient(
				outerColor, outerColor, innerColor, outerColor, outerColor,
				0.2f, 0.5f, 0.8f, 1f,
			), 1f, 0.01f,
			Vk2dFancyTextStyle.Gradient.plain(0),
			Vk2dFancyTextStyle.Gradient.plain(0), true,
		)
	}

	object BattleDescription {
		val NAME = Vk2dTextStyle(
			STRONG_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
				srgbToLinear(rgba(0, 0, 0, 180)),
				0.2f, true, 1f,
			)
		)
	}

	object PartyTab {
		val POSITIVE_STAT = Vk2dTextStyle.FillStyle(srgbToLinear(rgb(
			152, 255, 0
		))).only()!!
		val NEUTRAL_STAT = STRONG_TEXT_FILL.only()!!
		val NEGATIVE_STAT = Vk2dTextStyle.FillStyle(srgbToLinear(rgb(
			255, 169, 169
		))).only()!!
		val STAT_DIFFERENCE = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle.FillStyle(
				srgbToLinear(rgb(255, 203, 51))
			).only(), SHADOW_FILL.only(), 0.1f
		)
		val LOWER_STATS = Vk2dTextStyle.Shadowed(
			STRONG_TEXT_FILL.only(), SHADOW_FILL.only(), 0.1f
		)
		val GROWTH_EXPERIENCE = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle.FillStyle(
				srgbToLinear(rgb(253, 221, 95))
			).only(), SHADOW_FILL.only(), 0.1f,
		)
	}

	object Encyclopedia {
		val SECTION_TITLE = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle(Vk2dTextStyle.FillStyle(
				srgbToLinear(rgb(222, 166, 83))
			), Vk2dTextStyle.StrokeStyle.NONE),
			Vk2dTextStyle(Vk2dTextStyle.FillStyle(
				srgbToLinear(rgb(131, 80, 37))
			).withManipulatedDistance(0.3f, -0.2f), Vk2dTextStyle.StrokeStyle.NONE),
			0.15f
		)

		val DESCRIPTION = Vk2dTextStyle.Shadowed(
			STRONG_TEXT_FILL.only(), SHADOW_FILL3.only(), 0.15f
		)

		val LIST_ENTRY_STROKE = Vk2dTextStyle.StrokeStyle(
			rgba(0, 0, 0, 100),
			0.12f, true, 1f,
		)

		fun listEntry(color: Int) = Vk2dTextStyle(Vk2dTextStyle.FillStyle(color), LIST_ENTRY_STROKE)

		val DETAILS_TITLE = Vk2dTextStyle(STRONG_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
			rgba(0, 0, 0, 150), 0.15f, true, 1f
		))

		val DETAILS_PROPERTY_PEOPLE = Vk2dTextStyle.Shadowed(Vk2dTextStyle.FillStyle(
			srgbToLinear(rgb(255, 243, 159))
		).only(), SHADOW_FILL3.only(), 0.15f)

		val DETAILS_PROPERTY_BESTIARY = Vk2dTextStyle.Shadowed(
			BRIGHT_TEXT_FILL.only(), SHADOW_FILL3.only(), 0.15f,
		)

		fun elementalResistance(color: Int) = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle.FillStyle(color).only(), SHADOW_FILL3.only(), 0.15f
		)
	}

	object ShopUI {
		val ITEM_VALUE = Vk2dTextStyle.Shadowed(
			Vk2dTextStyle(BRIGHT_TEXT_FILL, Vk2dTextStyle.StrokeStyle(
				rgba(0, 0, 0, 100), 0.1f, true, 1f
			)),
			Vk2dTextStyle(SHADOW_FILL2, Vk2dTextStyle.StrokeStyle(
				rgba(0, 0, 0, 50),0.1f, true, 1f,
			)), 0.1f,
		)
	}

	object Cutscenes {

		fun chapterName(opacity: Float): Vk2dFancyTextStyle {
			var innerColor = srgbToLinear(rgb(241, 226, 188))
			innerColor = changeAlpha(innerColor, opacity)
			var outerColor = srgbToLinear(rgb(232, 198, 124))
			outerColor = changeAlpha(outerColor, opacity)

			return Vk2dFancyTextStyle(
				Vk2dFancyTextStyle.Gradient(
					outerColor, outerColor, innerColor, innerColor, outerColor,
					0.27f, 0.27f, 0.65f, 0.65f,
				), 1f, 0f,
				Vk2dFancyTextStyle.Gradient.plain(0),
				Vk2dFancyTextStyle.Gradient.plain(0), true,
			)
		}

		val CAPTION = run {
			val outerColor = srgbToLinear(rgb(157, 230, 252))
			val innerColor = srgbToLinear(rgb(248, 255, 255))
			Vk2dFancyTextStyle.Shadowed(
				Vk2dFancyTextStyle(
					Vk2dFancyTextStyle.Gradient(
						outerColor, outerColor, innerColor, innerColor, outerColor,
						0.15f, 0.15f, 0.55f, 0.55f,
					), 1f, 0f,
					Vk2dFancyTextStyle.Gradient.plain(0),
					Vk2dFancyTextStyle.Gradient.plain(0), true,
				),
				Vk2dFancyTextStyle(
					Vk2dFancyTextStyle.Gradient.plain(rgb(0, 0, 255)),
					1f, 0f, Vk2dFancyTextStyle.Gradient.plain(0),
					Vk2dFancyTextStyle.Gradient.plain(0), true,
				), 0.08f,
			)
		}
	}
}
