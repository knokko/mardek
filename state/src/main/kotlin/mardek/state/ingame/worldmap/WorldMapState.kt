package mardek.state.ingame.worldmap

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.FixedSoundEffects
import mardek.content.world.WorldMap
import mardek.content.world.WorldMapNode
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignStateMachine
import mardek.state.ingame.story.StoryState
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

/**
 * This class tracks the state when the player is currently on the world map. It tracks e.g. in which node/area the
 * player is, and whether it's currently walking towards another node.
 */
@BitStruct(backwardCompatible = true)
class WorldMapState(

	/**
	 * The world map on which the player is standing/walking. This world map must contain [currentNode].
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "world maps")
	val map: WorldMap,

	/**
	 * The node on which the player is standing (when `nextNode == null`), or walking away from
	 * (when `nextNode != null`).
	 */
	@BitField(id = 1)
	@ReferenceField(stable = true, label = "world map nodes")
	var currentNode: WorldMapNode,

	/**
	 * The position from which the player left the area state, and entered the world map. When the player re-enters
	 * when `nextEntrance == null`, the player will enter the area at the entrance that is closest to `exitPoint`.
	 */
	@BitField(id = 2)
	val exitPoint: AreaExitPoint,
) : CampaignStateMachine() {

	/**
	 * This field will be non-null if and only if the player is currently walking towards another node on this map.
	 * When non-null, this field contains the destination node, as well as the arrival time.
	 */
	@BitField(id = 3, optional = true)
	var nextNode: NextWorldMapNode? = null

	/**
	 * When non-null, this field determines through which entrance the player would enter the area, if the player
	 * would enter [currentNode] before moving to another node.
	 *
	 * This field is initially `null`, but will be changed whenever the player moves to another node on the world map.
	 * So this field is only `null` before the player has moved on the world map.
	 *
	 * If the player enters the area when this field is `null`, the [exitPoint] determines through which entrance the
	 * player will enter the area.
	 */
	@BitField(id = 4, optional = true)
	@ReferenceField(stable = true, label = "world map entrances")
	var nextEntrance: WorldMapNode.Entrance? = null

	/**
	 * When non-null, the player has decided to leave the world map, and enter an area. When
	 * `currentTime >= exiting.exitAt`, the player will enter the area.
	 *
	 * This field is initially `null`, which means the player can still interact with the world map. Once the player
	 * chooses an area to enter, this field will become non-null, and the world map fade-out starts.
	 */
	@BitField(id = 5, optional = true)
	var exiting: ExitingWorldMap? = null

	/**
	 * The in-game time that passed since the player entered the world map. When `currentTime >= nextNode.arrivalTime`,
	 * the player reaches the next node.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentTime = Duration.ZERO

	@Suppress("unused")
	private constructor() : this(WorldMap(), WorldMapNode(), AreaExitPoint())

	/**
	 * This method should be called during each invocation of [mardek.state.ingame.CampaignState.update], when the
	 * player is currently on the world map.
	 *
	 * If this method returns `null`, the player stays on the world map. If this method returns a non-null entrance,
	 * the player should leave the world map, and enter an area through that entrance.
	 */
	fun update(sounds: FixedSoundEffects, soundQueue: SoundQueue, timeStep: Duration): WorldMapNode.Entrance? {
		exiting?.run {
			if (currentTime >= exitAt) return entrance
		}
		nextNode?.run {
			if (currentTime >= arrivalTime) {
				soundQueue.insert(sounds.ui.toggleSkill)
				currentNode = destination
				nextNode = null
			}
		}
		currentTime += timeStep
		return null
	}

	/**
	 * This method should be called whenever the player presses a key, while the player is on the world map.
	 */
	fun pressKey(storyState: StoryState, key: InputKey) {
		if (nextNode != null || exiting != null) return

		if (key == InputKey.Interact) {
			val nextEntrance = this.nextEntrance
			if (nextEntrance != null) {
				this.exiting = ExitingWorldMap(nextEntrance, currentTime + FADE_DURATION)
				return
			}

			var bestEntrance = currentNode.entrances[0]
			var bestDistance = 123456.0
			for (candidateEntrance in currentNode.entrances.filter { it.area === exitPoint.area }) {
				val dx = exitPoint.x.toDouble() - candidateEntrance.x
				val dy = exitPoint.y.toDouble() - candidateEntrance.y
				val candidateDistance = sqrt(dx * dx + dy * dy)
				if (candidateDistance < bestDistance) {
					bestEntrance = candidateEntrance
					bestDistance = candidateDistance
				}
			}

			this.exiting = ExitingWorldMap(bestEntrance, currentTime + FADE_DURATION)
		}

		val keyAngle = when (key) {
			InputKey.MoveRight -> 0
			InputKey.MoveDown -> 90
			InputKey.MoveLeft -> 180
			InputKey.MoveUp -> 270
			else -> return
		}

		val edges = (map.edges.filter { it.node1 === currentNode }.map {
			Pair(it.node2, it.entrance2)
		} + map.edges.filter { it.node2 === currentNode }.map {
			Pair(it.node1, it.entrance1)
		}).filter { storyState.evaluate(it.first.wasDiscovered) != null }

		var bestNode = currentNode
		var bestAngle = 80.0
		for ((candidateNode, candidateEntrance) in edges) {
			val candidateAngle = toDegrees(atan2(
				(candidateNode.y - currentNode.y).toDouble(),
				(candidateNode.x - currentNode.x).toDouble(),
			))
			var relativeAngle = keyAngle - candidateAngle
			while (relativeAngle < -180) relativeAngle += 360
			while (relativeAngle > 180) relativeAngle -= 360
			relativeAngle = abs(relativeAngle)

			if (relativeAngle < bestAngle) {
				bestAngle = relativeAngle
				bestNode = candidateNode
				nextEntrance = candidateEntrance
			}
		}

		if (bestNode !== currentNode) {
			val dx = bestNode.x.toDouble() - currentNode.x
			val dy = bestNode.y.toDouble() - currentNode.y
			val distance = sqrt(dx * dx + dy * dy)
			nextNode = NextWorldMapNode(
				bestNode, currentTime,
				currentTime + distance * 5.milliseconds,
			)
		}

		return
	}

	companion object {
		/**
		 * The duration of the world map fade-in and fade-out.
		 */
		val FADE_DURATION = 500.milliseconds
	}
}
