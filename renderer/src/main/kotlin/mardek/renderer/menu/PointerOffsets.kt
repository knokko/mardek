package mardek.renderer.menu

import kotlin.math.abs

internal fun determinePointerOffset(): Float {
	val pointerPeriod = 500_000_000L
	val relativeTime = (System.nanoTime() - referenceTime) % pointerPeriod
	return 2f * abs(0.5f * pointerPeriod - relativeTime) / pointerPeriod
	// TODO CHAP1 Use this method in the Skills tab & Inventory tab, perhaps also in the battle/chest loot?
}
