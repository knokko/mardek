package mardek.state.ingame.battle

import mardek.content.characters.PlayableCharacter
import mardek.content.skill.SkillTargetType
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.characters.CharacterState

internal fun battleScrollHorizontally(state: BattleState, key: InputKey, soundQueue: SoundQueue) {
	val selectedMove = state.selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target == null) {
		if (key == InputKey.MoveLeft) state.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
		else state.selectedMove = BattleMoveSelectionFlee
		soundQueue.insert("menu-scroll")
	}
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isPlayer) {
			val nextIndex = closestTarget(
				state.playerLayout.positions[selectedMove.target.index], state.enemyStates, state.battle.enemyLayout
			)
			changeSelectedMove(state, BattleMoveSelectionAttack(
				CombatantReference(false, nextIndex, state)
			), soundQueue)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isPlayer) {
			val nextIndex = closestTarget(
				state.battle.enemyLayout.positions[selectedMove.target.index], state.playerStates, state.playerLayout
			)
			changeSelectedMove(state, BattleMoveSelectionAttack(
				CombatantReference(true, nextIndex, state)
			), soundQueue)
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionItem(item = null, target = null), soundQueue)
		else changeSelectedMove(state, BattleMoveSelectionAttack(target = null), soundQueue)
		soundQueue.insert("menu-scroll")
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target != null) {
		if (selectedMove.target is BattleSkillTargetSingle) {
			if (key == InputKey.MoveLeft && selectedMove.target.target.isPlayer &&
				selectedMove.skill.targetType != SkillTargetType.Self
			) {
				val nextIndex = closestTarget(
					state.playerLayout.positions[selectedMove.target.target.index], state.enemyStates, state.battle.enemyLayout
				)
				changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					CombatantReference(false, nextIndex, state)
				)), soundQueue)
			}
			if (key == InputKey.MoveLeft && !selectedMove.target.target.isPlayer &&
				selectedMove.skill.targetType == SkillTargetType.Any && state.enemyStates.count { it != null } > 1
			) {
				changeSelectedMove(state, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllEnemies
				), soundQueue)
			}
			if (key == InputKey.MoveRight && !selectedMove.target.target.isPlayer) {
				val nextIndex = closestTarget(
					state.battle.enemyLayout.positions[selectedMove.target.target.index], state.playerStates, state.playerLayout
				)
				changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					CombatantReference(true, nextIndex, state)
				)), soundQueue)
			}
			if (key == InputKey.MoveRight && selectedMove.target.target.isPlayer &&
				selectedMove.skill.targetType == SkillTargetType.Any && state.playerStates.count { it != null } > 1
			) {
				changeSelectedMove(state, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllAllies
				), soundQueue)
			}
		}
		if (selectedMove.target is BattleSkillTargetAllAllies &&
			selectedMove.skill.targetType != SkillTargetType.AllAllies && key == InputKey.MoveLeft
		) {
			changeSelectedMove(state, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(state.onTurn!!)
			), soundQueue)
		}
		if (selectedMove.target is BattleSkillTargetAllEnemies &&
			selectedMove.skill.targetType != SkillTargetType.AllEnemies && key == InputKey.MoveRight
		) {
			val firstEnemyTarget = CombatantReference(
				isPlayer = false, index = state.enemyStates.indexOfFirst { it != null }, state
			)
			changeSelectedMove(state, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(firstEnemyTarget)
			), soundQueue)
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionWait, soundQueue)
		else changeSelectedMove(state, BattleMoveSelectionSkill(skill = null, target = null), soundQueue)
		soundQueue.insert("menu-scroll")
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isPlayer) {
			val nextIndex = closestTarget(
				state.playerLayout.positions[selectedMove.target.index], state.enemyStates, state.battle.enemyLayout
			)
			changeSelectedMove(state, BattleMoveSelectionItem(
				selectedMove.item, CombatantReference(false, nextIndex, state)
			), soundQueue)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isPlayer) {
			val nextIndex = closestTarget(
				state.battle.enemyLayout.positions[selectedMove.target.index], state.playerStates, state.playerLayout
			)
			changeSelectedMove(state, BattleMoveSelectionItem(
				selectedMove.item, CombatantReference(true, nextIndex, state)
			), soundQueue)
		}
	}
	if (selectedMove is BattleMoveSelectionWait) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionFlee, soundQueue)
		else changeSelectedMove(state, BattleMoveSelectionItem(item = null, target = null), soundQueue)
		soundQueue.insert("menu-scroll")
	}
	if (selectedMove is BattleMoveSelectionFlee) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionAttack(target = null), soundQueue)
		else changeSelectedMove(state, BattleMoveSelectionWait, soundQueue)
		soundQueue.insert("menu-scroll")
	}
}

internal fun battleScrollVertically(
	state: BattleState, key: InputKey, soundQueue: SoundQueue,
	characterStates: Map<PlayableCharacter, CharacterState>
) {
	val selectedMove = state.selectedMove
	val player = state.players[state.onTurn!!.index]!!
	val playerState = characterStates[player]!!

	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		val newTarget = nextTarget(key, selectedMove.target, state)
		if (newTarget.index != selectedMove.target.index) {
			changeSelectedMove(state, BattleMoveSelectionAttack(newTarget), soundQueue)
		}
	}

	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target == null) {
			val skills = player.characterClass.skillClass.actions.filter { playerState.canCastSkill(it) }
			if (skills.size > 1) {
				var index = skills.indexOf(selectedMove.skill)
				if (key == InputKey.MoveUp) {
					index -= 1
					if (index < 0) index = skills.size - 1
				} else {
					index += 1
					if (index >= skills.size) index = 0
				}
				changeSelectedMove(state, BattleMoveSelectionSkill(skill = skills[index], target = null), soundQueue)
				soundQueue.insert("menu-scroll")
			}
		} else {
			if (selectedMove.skill.targetType != SkillTargetType.Self && selectedMove.target is BattleSkillTargetSingle) {
				val newTarget = nextTarget(key, selectedMove.target.target, state)
				if (newTarget.index != selectedMove.target.target.index) {
					changeSelectedMove(state, BattleMoveSelectionSkill(
						selectedMove.skill, BattleSkillTargetSingle(newTarget)
					), soundQueue)
				}
			}
		}
	}

	if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target == null) {
			val items = playerState.inventory.filter {
				it != null && it.item.consumable != null
			}.mapNotNull { it!!.item }.toSet().toList()
			if (items.size > 1) {
				var index = items.indexOf(selectedMove.item)
				if (key == InputKey.MoveUp) {
					index -= 1
					if (index < 0) index = items.size - 1
				} else {
					index += 1
					if (index >= items.size) index = 0
				}
				changeSelectedMove(state, BattleMoveSelectionItem(item = items[index], target = null), soundQueue)
				soundQueue.insert("menu-scroll")
			}
		} else {
			val newTarget = nextTarget(key, selectedMove.target, state)
			if (newTarget.index != selectedMove.target.index) {
				changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, newTarget), soundQueue)
			}
		}
	}
}

internal fun battleClick(
	state: BattleState, soundQueue: SoundQueue,
	characterStates: Map<PlayableCharacter, CharacterState>
) {
	val selectedMove = state.selectedMove
	val firstEnemyTarget = CombatantReference(
		isPlayer = false, index = state.enemyStates.indexOfFirst { it != null }, state
	)

	if (selectedMove is BattleMoveSelectionAttack) {
		if (selectedMove.target == null) {
			changeSelectedMove(state, BattleMoveSelectionAttack(target = firstEnemyTarget), soundQueue)
		} else {
			state.confirmMove(BattleMoveBasicAttack(selectedMove.target), soundQueue)
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		val player = state.players[state.onTurn!!.index]!!
		val playerState = characterStates[player]!!
		val skill = player.characterClass.skillClass.actions.firstOrNull { playerState.canCastSkill(it) }
		if (skill != null) {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = skill, target = null), soundQueue)
			soundQueue.insert("click-confirm")
		} else soundQueue.insert("click-reject")
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target == null) {
			val firstTarget = when (selectedMove.skill.targetType) {
				SkillTargetType.Self -> BattleSkillTargetSingle(state.onTurn!!)
				SkillTargetType.AllEnemies -> BattleSkillTargetAllEnemies
				SkillTargetType.AllAllies -> BattleSkillTargetAllAllies
				else -> BattleSkillTargetSingle(
					if (selectedMove.skill.isPositive()) state.onTurn!! else firstEnemyTarget
				)
			}
			changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, firstTarget), soundQueue)
		} else {
			var manaCost = selectedMove.skill.manaCost
			if (selectedMove.target is BattleSkillTargetAllAllies || selectedMove.target is BattleSkillTargetAllEnemies) {
				manaCost *= 2
			}

			val playerState = state.onTurn!!.getState()
			if (manaCost <= playerState.currentMana) {
				playerState.currentMana -= manaCost
				state.confirmMove(BattleMoveSkill(selectedMove.skill, selectedMove.target, null), soundQueue)
			} else {
				soundQueue.insert("click-reject")
			}
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		val player = state.players[state.onTurn!!.index]!!
		val playerState = characterStates[player]!!
		val item = playerState.inventory.firstOrNull { it != null && it.item.consumable != null }
		if (item != null) {
			changeSelectedMove(state, BattleMoveSelectionItem(item = item.item, target = null), soundQueue)
			soundQueue.insert("click-confirm")
		} else soundQueue.insert("click-reject")
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target == null) {
			val target = if (selectedMove.item.consumable!!.isPositive()) state.onTurn!! else firstEnemyTarget
			changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, target), soundQueue)
		} else {
			val player = state.players[state.onTurn!!.index]!!
			val playerState = characterStates[player]!!
			if (!playerState.removeItem(selectedMove.item)) throw IllegalStateException()
			state.confirmMove(BattleMoveItem(selectedMove.item, selectedMove.target), soundQueue)
		}
	}
	if (selectedMove is BattleMoveSelectionWait) state.confirmMove(BattleMoveWait, soundQueue)
	if (selectedMove is BattleMoveSelectionFlee) state.runAway()
}

internal fun battleCancel(state: BattleState, soundQueue: SoundQueue) {
	val selectedMove = state.selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		changeSelectedMove(state, BattleMoveSelectionAttack(target = null), soundQueue)
	} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = selectedMove.skill, target = null), soundQueue)
		} else {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = null, target = null), soundQueue)
			soundQueue.insert("click-cancel")
		}
	} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(state, BattleMoveSelectionItem(item = selectedMove.item, target = null), soundQueue)
		} else {
			changeSelectedMove(state,BattleMoveSelectionItem(item = null, target = null), soundQueue)
			soundQueue.insert("click-cancel")
		}
	} else {
		state.confirmMove(BattleMoveWait, soundQueue)
	}
}

private fun changeSelectedMove(state: BattleState, newMove: BattleMoveSelection, soundQueue: SoundQueue) {
	val oldTargets = state.selectedMove.targets(state)
	state.selectedMove = newMove
	val newTargets = newMove.targets(state)

	if (oldTargets.isNotEmpty() && newTargets.isNotEmpty() && !oldTargets.contentEquals(newTargets)) {
		soundQueue.insert("menu-scroll")
	}
	if (oldTargets.isEmpty() && newTargets.isNotEmpty()) soundQueue.insert("click-confirm")
	if (oldTargets.isNotEmpty() && newTargets.isEmpty()) soundQueue.insert("click-cancel")

	for (target in newTargets) target.getState().lastPointedTo = System.nanoTime()
}
