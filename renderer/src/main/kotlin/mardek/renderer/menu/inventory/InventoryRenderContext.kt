package mardek.renderer.menu.inventory

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import mardek.renderer.RenderContext
import mardek.renderer.area.AreaSpriteBatch
import mardek.renderer.glyph.MardekGlyphBatch

internal class InventoryRenderContext(
	val context: RenderContext,
	val colorBatch: Vk2dColorBatch,
	val simpleSpriteBatch: Vk2dKim3Batch?,
	val areaSpriteBatch: AreaSpriteBatch?,
	val imageBatch: Vk2dImageBatch,
	val lateColorBatch: Vk2dColorBatch,
	val textBatch: MardekGlyphBatch,
)
