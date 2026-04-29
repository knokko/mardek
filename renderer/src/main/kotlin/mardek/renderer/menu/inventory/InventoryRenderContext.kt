package mardek.renderer.menu.inventory

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.renderer.RenderContext
import mardek.renderer.area.AreaSpriteBatch

internal class InventoryRenderContext(
	val context: RenderContext,
	val colorBatch: Vk2dColorBatch,
	val simpleSpriteBatch: Vk2dKim3Batch?,
	val areaSpriteBatch: AreaSpriteBatch?,
	val imageBatch: Vk2dImageBatch,
	val lateColorBatch: Vk2dColorBatch,
	val simpleTextBatch: Vk2dSimpleTextBatch,
	val fancyTextBatch: Vk2dFancyTextBatch,
)
