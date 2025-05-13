package mardek.renderer.battle

import org.joml.Vector2f

class BattleCreatureRenderers(
	private val context: BattleRenderContext
) {
	fun render() {
		context.resources.partRenderer.startBatch(context.recorder)
		context.resources.partRenderer.render(context.battle.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for (enemy in context.battle.allOpponents()) SingleCreatureRenderer(context, enemy).render()
		for (player in context.battle.allPlayers()) SingleCreatureRenderer(context, player).render()

		context.resources.partRenderer.endBatch()
	}
}
