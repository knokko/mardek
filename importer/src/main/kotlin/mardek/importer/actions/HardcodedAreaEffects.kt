package mardek.importer.actions

import mardek.content.action.ActionContent
import mardek.content.action.effect.AreaActionEffect
import mardek.content.action.effect.AreaEffectFloat
import mardek.content.action.effect.AreaEffectsEmitter
import mardek.content.action.effect.AreaRingEffect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun hardcodeAreaActionEffects(content: ActionContent) {
	hardcodeRohophSoulEffects(content)
}

private fun hardcodeRohophSoulEffects(content: ActionContent) {
	content.areaEffects.add(AreaActionEffect(
		name = "RohophSoul",
		emitters = arrayOf(
			AreaEffectsEmitter(
				firstDispatchAfter = Duration.ZERO,
				period = Duration.ZERO,
				maxDispatches = 1,
				maxLifetime = Duration.ZERO,
				rings = arrayOf(
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(linear = 6f)
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(0.2f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(linear = 6f)
						),
					),
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(0.2f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(linear = 6f)
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(2f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(linear = 6f * 0.35f, max = 0.35f)
						),
					)
				)
			),
			AreaEffectsEmitter(
				firstDispatchAfter = 1.seconds / 6,
				period = 250.milliseconds,
				maxDispatches = 0,
				maxLifetime = 5.seconds,
				rings = arrayOf(
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(2f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(0.35f, linear = -0.35f),
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(3f, linear = 4.5f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(0.2f, linear = -0.2f),
						)
					),
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(3f, linear = 4.5f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(0.2f, linear = -0.2f),
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(5f, linear = 7.5f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(0f),
						),
					)
				)
			)
		)
	))

	content.areaEffects.add(AreaActionEffect(
		name = "RohophSoulAbsorb",
		emitters = arrayOf(
			AreaEffectsEmitter(
				firstDispatchAfter = Duration.ZERO,
				period = Duration.ZERO,
				maxDispatches = 1,
				maxLifetime = Duration.ZERO,
				rings = arrayOf(
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(1f)
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(0.2f, linear = -0.6f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(1f)
						),
					),
					AreaRingEffect(
						innerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(-40f, linear = 300f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat(0.08f, linear = -0.2f, min = 0.0075f)
						),
						outerBorder = AreaRingEffect.Border(
							radius = AreaEffectFloat(linear = 450f),
							red = AreaEffectFloat(1f),
							green = AreaEffectFloat(1f),
							blue = AreaEffectFloat(1f),
							alpha = AreaEffectFloat()
						),
					)
				)
			)
		)
	))
}
