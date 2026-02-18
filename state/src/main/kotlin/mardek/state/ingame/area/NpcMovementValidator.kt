package mardek.state.ingame.area

import mardek.content.area.Chest
import mardek.content.area.Direction
import mardek.content.area.objects.AreaCharacter
import mardek.content.area.objects.StaticAreaObject
import kotlin.math.abs

private fun StaticAreaObject.position() = AreaPosition(x, y)

/**
 * Determines whether the game should allow `npc` to randomly walk to `candidatePosition`.
 *
 * There are several reasons why we might want to forbid this, for instance:
 * - because the tile is inaccessible, or
 * - the character would block the only path from the player to e.g. a door or area transition, or
 * - the character is standing next to the player (talking to NPCs is quite annoying if they keep moving away..)
 *
 * This method will return `false` if any of these clauses apply.
 */
internal fun shouldAllowNpcMove(
	currentState: AreaState,
	openedChests: Set<Chest>,
	npc: AreaCharacter,
	candidatePosition: AreaPosition,
): Boolean {
	// First of all, don't allow NPCs to move to inaccessible tiles
	if (!currentState.canWalkTo(candidatePosition)) return false

	// And we shouldn't allow NPCs to walk into the player
	val playerPosition = currentState.getPlayerPosition(0)
	if (candidatePosition == currentState.getPlayerPosition(0)) return false
	val suspension = currentState.suspension
	var nextPlayerPosition = playerPosition
	if (suspension is AreaSuspensionPlayerWalking) {
		nextPlayerPosition = suspension.destination.position
		if (candidatePosition == nextPlayerPosition) return false
	}

	// If we reach this line, the movement is *possible*, but we need to check whether it is desirable.

	// Don't allow NPCs to walk away when they are standing next to the current or next position of the player,
	// since that's very annoying when players try to talk to that NPC.
	// Besides, it makes sure NPCs don't walk away while talking to the player.
	val npcState = currentState.getCharacterState(npc)!!
	if (abs(npcState.x - playerPosition.x) + abs(npcState.y - playerPosition.y) <= 1) return false
	if (abs(npcState.x - nextPlayerPosition.x) + abs(npcState.y - nextPlayerPosition.y) <= 1) return false

	// Furthermore, we don't allow any movements that would block the path of the player to any 'point of interest'
	val oldReachableTiles = determineReachableTiles(currentState, npcState.toPosition())
	val predictedReachableTiles = determineReachableTiles(currentState, candidatePosition)

	val ao = currentState.area.objects

	// so don't allow NPCs to block access to portals and transitions
	for (interestingObject in ao.portals + ao.transitions) {
		if (oldReachableTiles.contains(interestingObject.position()) &&
			!predictedReachableTiles.contains(interestingObject.position())
		) {
			return false
		}
	}

	// and we don't allow NPCs to occupy the only tile from which the player can interact with something
	val interactionPoints = (ao.decorations.filter {
		it.ownActions != null || it.sharedActionSequence != null
	} + ao.doors + ao.shops + ao.switchOrbs + ao.talkTriggers).map { it.position() }.toMutableList()

	interactionPoints.addAll(currentState.area.chests.filter {
		!openedChests.contains(it)
	}.map { AreaPosition(it.x, it.y) })

	interactionPoints.addAll(currentState.characterStates.filter {
		it.key !== npc
	}.map { it.value.toPosition() })

	for (interactionPoint in interactionPoints) {
		var oldAccessibleNeighbors = 0
		var newAccessibleNeighbours = 0
		for (direction in Direction.allProper()) {
			val x = interactionPoint.x + direction.deltaX
			val y = interactionPoint.y + direction.deltaY
			val position = AreaPosition(x, y)
			if (oldReachableTiles.contains(position)) oldAccessibleNeighbors += 1
			if (predictedReachableTiles.contains(position)) newAccessibleNeighbours += 1
		}

		if (newAccessibleNeighbours == 0 && oldAccessibleNeighbors > 0) return false
	}

	return true
}

/**
 * Determines the set of area positions to which the player can walk (from its current position).
 * This method will take collision rules into account (e.g. the player cannot walk through tiles or other characters).
 * Furthermore, this method assumes that the player cannot walk through `extraBlockedPosition`.
 */
internal fun determineReachableTiles(state: AreaState, extraBlockedPosition: AreaPosition): Set<AreaPosition> {
	val pp = state.getPlayerPosition(0)
	val reachableTiles = hashSetOf(pp)
	val candidateTiles = Direction.allProper().map {
		AreaPosition(pp.x + it.deltaX, pp.y + it.deltaY)
	}.toMutableList()

	while (candidateTiles.isNotEmpty()) {
		val candidate = candidateTiles.removeLast()

		// Check whether the candidate tile is accessible
		if (candidate == extraBlockedPosition) continue
		if (!state.canWalkTo(candidate.x, candidate.y)) continue

		// Don't leave the area bounds, to avoid potential endless loops
		if (candidate.x < state.area.minTileX || candidate.x > state.area.maxTileX) continue
		if (candidate.y < state.area.minTileY || candidate.y > state.area.maxTileY) continue

		// Skip tiles that we already included, to avoid endless loops
		if (!reachableTiles.add(candidate)) continue

		// If the candidate tile is accessible, and we haven't processed it yet, we should try its neighbours
		for (direction in Direction.allProper()) {
			candidateTiles.add(AreaPosition(candidate.x + direction.deltaX, candidate.y + direction.deltaY))
		}
	}

	return reachableTiles
}
