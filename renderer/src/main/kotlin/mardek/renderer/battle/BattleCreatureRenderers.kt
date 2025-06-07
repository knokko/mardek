package mardek.renderer.battle

import org.joml.Vector2f

class BattleCreatureRenderers(
	private val context: BattleRenderContext
) {
	fun render() {
		context.resources.partRenderer.render(context.battle.battle.background.sprite, arrayOf(
			Vector2f(-1f, -1f), Vector2f(1f, -1f), Vector2f(1f, 1f), Vector2f(-1f, 1f)
		), null)

		for (enemy in context.battle.allOpponents().sortedBy {
			opponent -> opponent.getPosition(context.battle).y
		}) SingleCreatureRenderer(context, enemy).render()
		for (player in context.battle.allPlayers().sortedBy {
			player -> player.getPosition(context.battle).y
		}) SingleCreatureRenderer(context, player).render()
	}
}
