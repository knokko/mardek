package mardek.importer.inventory

import mardek.assets.inventory.InventoryAssets

fun exportItemTextures(assets: InventoryAssets): IntArray {
	var nextSpriteIndex = 0
	for (item in assets.items) {
		item.spriteIndex = nextSpriteIndex
		nextSpriteIndex += item.sprite!!.size
	}

	val sprites = IntArray(nextSpriteIndex)
	for (item in assets.items) {
		for ((offset, value) in item.sprite!!.withIndex()) sprites[item.spriteIndex + offset] = value
		item.sprite = null
	}

	return sprites
}
