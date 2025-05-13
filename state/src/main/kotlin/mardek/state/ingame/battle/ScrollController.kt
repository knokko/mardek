package mardek.state.ingame.battle

import mardek.content.skill.SkillTargetType
import mardek.input.InputKey

internal fun battleScrollHorizontally(state: BattleState, key: InputKey, context: BattleUpdateContext) {
	val selectedMove = state.selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target == null) {
		if (key == InputKey.MoveLeft) state.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
		else state.selectedMove = BattleMoveSelectionFlee
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(state), state.opponents, state.battle.enemyLayout)
			changeSelectedMove(state, BattleMoveSelectionAttack(state.opponents[nextIndex]!!), context)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(state), state.players, state.playerLayout)
			changeSelectedMove(state, BattleMoveSelectionAttack(state.players[nextIndex]!!), context)
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionItem(item = null, target = null), context)
		else changeSelectedMove(state, BattleMoveSelectionAttack(target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target != null) {
		if (selectedMove.target is BattleSkillTargetSingle) {
			if (key == InputKey.MoveLeft && selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType != SkillTargetType.Self
			) {
				val nextIndex = closestTarget(
					selectedMove.target.target.getPosition(state), state.opponents, state.battle.enemyLayout
				)
				changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					state.opponents[nextIndex]!!
				)), context)
			}
			if (key == InputKey.MoveLeft && !selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType == SkillTargetType.Any && state.livingOpponents().size > 1
			) {
				changeSelectedMove(state, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllEnemies
				), context)
			}
			if (key == InputKey.MoveRight && !selectedMove.target.target.isOnPlayerSide) {
				val nextIndex = closestTarget(
					selectedMove.target.target.getPosition(state), state.players, state.playerLayout
				)
				changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					state.players[nextIndex]!!
				)), context)
			}
			if (key == InputKey.MoveRight && selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType == SkillTargetType.Any && state.livingPlayers().size > 1
			) {
				changeSelectedMove(state, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllAllies
				), context)
			}
		}
		if (selectedMove.target is BattleSkillTargetAllAllies &&
			selectedMove.skill.targetType != SkillTargetType.AllAllies && key == InputKey.MoveLeft
		) {
			changeSelectedMove(state, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(state.onTurn!!)
			), context)
		}
		if (selectedMove.target is BattleSkillTargetAllEnemies &&
			selectedMove.skill.targetType != SkillTargetType.AllEnemies && key == InputKey.MoveRight
		) {
			val firstEnemyTarget = state.livingOpponents().first()
			changeSelectedMove(state, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(firstEnemyTarget)
			), context)
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionWait, context)
		else changeSelectedMove(state, BattleMoveSelectionSkill(skill = null, target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(
				selectedMove.target.getPosition(state), state.opponents, state.battle.enemyLayout
			)
			changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, state.opponents[nextIndex]), context)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(state), state.players, state.playerLayout)
			changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, state.players[nextIndex]), context)
		}
	}
	if (selectedMove is BattleMoveSelectionWait) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionFlee, context)
		else changeSelectedMove(state, BattleMoveSelectionItem(item = null, target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
	if (selectedMove is BattleMoveSelectionFlee) {
		if (key == InputKey.MoveLeft) changeSelectedMove(state, BattleMoveSelectionAttack(target = null), context)
		else changeSelectedMove(state, BattleMoveSelectionWait, context)
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
}

internal fun battleScrollVertically(state: BattleState, key: InputKey, context: BattleUpdateContext) {
	val selectedMove = state.selectedMove
	val onTurn = state.onTurn as PlayerCombatantState
	val playerState = context.characterStates[onTurn.player]!!

	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		val newTarget = nextTarget(key, selectedMove.target, state)
		if (newTarget !== selectedMove.target) {
			changeSelectedMove(state, BattleMoveSelectionAttack(newTarget), context)
		}
	}

	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target == null) {
			val skills = onTurn.player.characterClass.skillClass.actions.filter { playerState.canCastSkill(it) }
			if (skills.size > 1) {
				var index = skills.indexOf(selectedMove.skill)
				if (key == InputKey.MoveUp) {
					index -= 1
					if (index < 0) index = skills.size - 1
				} else {
					index += 1
					if (index >= skills.size) index = 0
				}
				changeSelectedMove(state, BattleMoveSelectionSkill(skill = skills[index], target = null), context)
				context.soundQueue.insert(context.sounds.ui.scroll)
			}
		} else {
			if (selectedMove.skill.targetType != SkillTargetType.Self && selectedMove.target is BattleSkillTargetSingle) {
				val newTarget = nextTarget(key, selectedMove.target.target, state)
				if (newTarget !== selectedMove.target.target) {
					changeSelectedMove(state, BattleMoveSelectionSkill(
						selectedMove.skill, BattleSkillTargetSingle(newTarget)
					), context)
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
				changeSelectedMove(state, BattleMoveSelectionItem(item = items[index], target = null), context)
				context.soundQueue.insert(context.sounds.ui.scroll)
			}
		} else {
			val newTarget = nextTarget(key, selectedMove.target, state)
			if (newTarget !== selectedMove.target) {
				changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, newTarget), context)
			}
		}
	}
}

internal fun battleClick(state: BattleState, context: BattleUpdateContext) {
	val selectedMove = state.selectedMove
	val firstEnemyTarget = state.livingOpponents().first()

	if (selectedMove is BattleMoveSelectionAttack) {
		if (selectedMove.target == null) {
			changeSelectedMove(state, BattleMoveSelectionAttack(target = firstEnemyTarget), context)
		} else {
			state.confirmMove(context, BattleMoveBasicAttack(selectedMove.target))
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		val onTurn = state.onTurn as PlayerCombatantState
		val playerState = context.characterStates[onTurn.player]!!
		val skill = onTurn.player.characterClass.skillClass.actions.firstOrNull { playerState.canCastSkill(it) }
		if (skill != null) {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = skill, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		} else context.soundQueue.insert(context.sounds.ui.clickReject)
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
			changeSelectedMove(state, BattleMoveSelectionSkill(selectedMove.skill, firstTarget), context)
		} else {
			var manaCost = selectedMove.skill.manaCost
			if (selectedMove.target is BattleSkillTargetAllAllies || selectedMove.target is BattleSkillTargetAllEnemies) {
				manaCost *= 2
			}

			if (manaCost <= state.onTurn!!.currentMana) {
				state.onTurn!!.currentMana -= manaCost
				val nextMove = BattleMoveSkill(selectedMove.skill, selectedMove.target, null)
				state.confirmMove(context, nextMove)
			} else {
				context.soundQueue.insert(context.sounds.ui.clickReject)
			}
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		val playerState = context.characterStates[(state.onTurn as PlayerCombatantState).player]!!
		val item = playerState.inventory.firstOrNull { it != null && it.item.consumable != null }
		if (item != null) {
			changeSelectedMove(state, BattleMoveSelectionItem(item = item.item, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		} else context.soundQueue.insert(context.sounds.ui.clickReject)
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target == null) {
			val target = if (selectedMove.item.consumable!!.isPositive()) state.onTurn!! else firstEnemyTarget
			changeSelectedMove(state, BattleMoveSelectionItem(selectedMove.item, target), context)
		} else {
			val playerState = context.characterStates[(state.onTurn as PlayerCombatantState).player]!!
			if (!playerState.removeItem(selectedMove.item)) throw IllegalStateException()
			state.confirmMove(context, BattleMoveItem(selectedMove.item, selectedMove.target))
		}
	}
	if (selectedMove is BattleMoveSelectionWait) state.confirmMove(context, BattleMoveWait())
	if (selectedMove is BattleMoveSelectionFlee) state.runAway()
}

internal fun battleCancel(state: BattleState, context: BattleUpdateContext) {
	val selectedMove = state.selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		changeSelectedMove(state, BattleMoveSelectionAttack(target = null), context)
	} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = selectedMove.skill, target = null), context)
		} else {
			changeSelectedMove(state, BattleMoveSelectionSkill(skill = null, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(state, BattleMoveSelectionItem(item = selectedMove.item, target = null), context)
		} else {
			changeSelectedMove(state,BattleMoveSelectionItem(item = null, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	} else {
		state.confirmMove(context, BattleMoveWait())
	}
}

private fun changeSelectedMove(state: BattleState, newMove: BattleMoveSelection, context: BattleUpdateContext) {
	val oldTargets = state.selectedMove.targets(state)
	state.selectedMove = newMove
	val newTargets = newMove.targets(state)

	if (oldTargets.isNotEmpty() && newTargets.isNotEmpty() && !oldTargets.contentEquals(newTargets)) {
		context.soundQueue.insert(context.sounds.ui.scroll)
	}
	if (oldTargets.isEmpty() && newTargets.isNotEmpty()) context.soundQueue.insert(context.sounds.ui.clickConfirm)
	if (oldTargets.isNotEmpty() && newTargets.isEmpty()) context.soundQueue.insert(context.sounds.ui.clickCancel)

	for (target in newTargets) target.lastPointedTo = System.nanoTime()
}
