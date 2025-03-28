package mardek.renderer.ui.tabs

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.content.Content
import mardek.content.stats.CombatStat
import mardek.content.inventory.EquipmentSlotType
import mardek.content.skill.ActiveSkill
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.Kim1Renderer
import mardek.renderer.batch.KimRequest
import mardek.renderer.SharedResources
import mardek.state.ingame.CampaignState
import mardek.content.inventory.ItemStack
import mardek.renderer.ui.renderDescription
import mardek.state.ingame.menu.InventoryTab
import mardek.state.title.AbsoluteRectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

///////////////
// Constants //
///////////////
private const val PARTY_BAR_HEIGHT = 30;
private const val BAR_MARGIN = 5;

class PartyTabRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val frameIndex: Int,

	private val region: AbsoluteRectangle,
	private val state: CampaignState,
	private val content: Content,
	private val resources: SharedResources,
): TabRenderer() {


	private lateinit var kim1Batch: KimBatch
	private lateinit var kim2Batch: KimBatch

	override fun beforeRendering()
	{
		this.kim1Batch = resources.kim1Renderer.startBatch()
		this.kim2Batch = resources.kim2Renderer.startBatch()
	}

	override fun render()
	{
		val uiRenderer = resources.uiRenderers[frameIndex]
   		val textColor = srgbToLinear(rgb(0, 0, 255)) // White text

		// Draw "Hello, World!" at the center of the tab
		uiRenderer.drawString(
			resources.font, "Hello, World!", textColor, intArrayOf(),
			region.minX, region.minY, region.maxX, region.maxY,
			region.minY + (region.height / 2), region.width / 20, 1, TextAlignment.CENTER
		)
	}
}