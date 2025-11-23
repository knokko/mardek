package mardek.state.ingame.battle

import mardek.content.skill.SkillTargetType
import mardek.input.InputKey

internal fun battleScrollHorizontally(battle: BattleState, key: InputKey, context: BattleUpdateContext) {
	val state = battle.state as BattleStateMachine.SelectMove
	val selectedMove = state.selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target == null) {
		if (key == InputKey.MoveLeft) state.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
		else state.selectedMove = BattleMoveSelectionFlee
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(battle), battle.opponents, battle.battle.enemyLayout)
			changeSelectedMove(battle, BattleMoveSelectionAttack(battle.opponents[nextIndex]!!), context)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(battle), battle.players, battle.playerLayout)
			changeSelectedMove(battle, BattleMoveSelectionAttack(battle.players[nextIndex]!!), context)
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(battle, BattleMoveSelectionItem(item = null, target = null), context)
		else changeSelectedMove(battle, BattleMoveSelectionAttack(target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target != null) {
		if (selectedMove.target is BattleSkillTargetSingle) {
			if (key == InputKey.MoveLeft && selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType != SkillTargetType.Self
			) {
				val nextIndex = closestTarget(
					selectedMove.target.target.getPosition(battle), battle.opponents, battle.battle.enemyLayout
				)
				changeSelectedMove(battle, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					battle.opponents[nextIndex]!!
				)), context)
			}
			if (key == InputKey.MoveLeft && !selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType == SkillTargetType.Any && battle.livingOpponents().size > 1
			) {
				changeSelectedMove(battle, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllEnemies
				), context)
			}
			if (key == InputKey.MoveRight && !selectedMove.target.target.isOnPlayerSide) {
				val nextIndex = closestTarget(
					selectedMove.target.target.getPosition(battle), battle.players, battle.playerLayout
				)
				changeSelectedMove(battle, BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
					battle.players[nextIndex]!!
				)), context)
			}
			if (key == InputKey.MoveRight && selectedMove.target.target.isOnPlayerSide &&
				selectedMove.skill.targetType == SkillTargetType.Any && battle.livingPlayers().size > 1
			) {
				changeSelectedMove(battle, BattleMoveSelectionSkill(
					selectedMove.skill, BattleSkillTargetAllAllies
				), context)
			}
		}
		if (selectedMove.target is BattleSkillTargetAllAllies &&
			selectedMove.skill.targetType != SkillTargetType.AllAllies && key == InputKey.MoveLeft
		) {
			changeSelectedMove(battle, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(state.onTurn)
			), context)
		}
		if (selectedMove.target is BattleSkillTargetAllEnemies &&
			selectedMove.skill.targetType != SkillTargetType.AllEnemies && key == InputKey.MoveRight
		) {
			val firstEnemyTarget = battle.livingOpponents().first()
			changeSelectedMove(battle, BattleMoveSelectionSkill(
				selectedMove.skill, BattleSkillTargetSingle(firstEnemyTarget)
			), context)
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		if (key == InputKey.MoveLeft) changeSelectedMove(battle, BattleMoveSelectionWait, context)
		else changeSelectedMove(battle, BattleMoveSelectionSkill(skill = null, target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
		if (key == InputKey.MoveLeft && selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(
				selectedMove.target.getPosition(battle), battle.opponents, battle.battle.enemyLayout
			)
			changeSelectedMove(battle, BattleMoveSelectionItem(selectedMove.item, battle.opponents[nextIndex]), context)
		}
		if (key == InputKey.MoveRight && !selectedMove.target.isOnPlayerSide) {
			val nextIndex = closestTarget(selectedMove.target.getPosition(battle), battle.players, battle.playerLayout)
			changeSelectedMove(battle, BattleMoveSelectionItem(selectedMove.item, battle.players[nextIndex]), context)
		}
	}
	if (selectedMove is BattleMoveSelectionWait) {
		if (key == InputKey.MoveLeft) changeSelectedMove(battle, BattleMoveSelectionFlee, context)
		else changeSelectedMove(battle, BattleMoveSelectionItem(item = null, target = null), context)
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
	if (selectedMove is BattleMoveSelectionFlee) {
		if (key == InputKey.MoveLeft) changeSelectedMove(battle, BattleMoveSelectionAttack(target = null), context)
		else changeSelectedMove(battle, BattleMoveSelectionWait, context)
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
}

internal fun battleScrollVertically(battle: BattleState, key: InputKey, context: BattleUpdateContext) {
	val state = battle.state as BattleStateMachine.SelectMove
	val selectedMove = state.selectedMove
	val player = state.onTurn.player
	val playerState = context.characterStates[player]!!

	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		val newTarget = nextTarget(key, selectedMove.target, battle)
		if (newTarget !== selectedMove.target) {
			changeSelectedMove(battle, BattleMoveSelectionAttack(newTarget), context)
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
				changeSelectedMove(battle, BattleMoveSelectionSkill(skill = skills[index], target = null), context)
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
		} else {
			if (selectedMove.skill.targetType != SkillTargetType.Self && selectedMove.target is BattleSkillTargetSingle) {
				val newTarget = nextTarget(key, selectedMove.target.target, battle)
				if (newTarget !== selectedMove.target.target) {
					changeSelectedMove(battle, BattleMoveSelectionSkill(
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
				changeSelectedMove(battle, BattleMoveSelectionItem(item = items[index], target = null), context)
				context.soundQueue.insert(context.sounds.ui.scroll1)
			}
		} else {
			val newTarget = nextTarget(key, selectedMove.target, battle)
			if (newTarget !== selectedMove.target) {
				changeSelectedMove(battle, BattleMoveSelectionItem(selectedMove.item, newTarget), context)
			}
		}
	}
}

internal fun battleClick(battle: BattleState, context: BattleUpdateContext) {
	val state = battle.state as BattleStateMachine.SelectMove
	val selectedMove = state.selectedMove
	val firstEnemyTarget = battle.livingOpponents().first()

	if (selectedMove is BattleMoveSelectionAttack) {
		val target = selectedMove.target
		if (target == null) {
			changeSelectedMove(battle, BattleMoveSelectionAttack(target = firstEnemyTarget), context)
		} else {
			battle.confirmMove(context, BattleStateMachine.MeleeAttack.MoveTo(
				state.onTurn, target, null, context
			))
		}
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
		val playerState = context.characterStates[state.onTurn.player]!!
		val skill = state.onTurn.player.characterClass.skillClass.actions.firstOrNull { playerState.canCastSkill(it) }
		if (skill != null) {
			changeSelectedMove(battle, BattleMoveSelectionSkill(skill = skill, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		} else context.soundQueue.insert(context.sounds.ui.clickReject)
	}
	if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target == null) {
			val firstTarget = when (selectedMove.skill.targetType) {
				SkillTargetType.Self -> BattleSkillTargetSingle(state.onTurn)
				SkillTargetType.AllEnemies -> BattleSkillTargetAllEnemies
				SkillTargetType.AllAllies -> BattleSkillTargetAllAllies
				else -> BattleSkillTargetSingle(
					if (selectedMove.skill.isPositive()) state.onTurn else firstEnemyTarget
				)
			}
			changeSelectedMove(battle, BattleMoveSelectionSkill(selectedMove.skill, firstTarget), context)
		} else {
			var manaCost = selectedMove.skill.manaCost
			if (selectedMove.target is BattleSkillTargetAllAllies || selectedMove.target is BattleSkillTargetAllEnemies) {
				manaCost *= 2
			}

			if (manaCost <= state.onTurn.currentMana) {
				state.onTurn.currentMana -= manaCost
				val nextMove = if (selectedMove.skill.isMelee) BattleStateMachine.MeleeAttack.MoveTo(
					state.onTurn, (selectedMove.target as BattleSkillTargetSingle).target,
					selectedMove.skill, context
				) else if (selectedMove.skill.isBreath) BattleStateMachine.BreathAttack.MoveTo(
					state.onTurn, selectedMove.target.getTargets(state.onTurn, battle),
					selectedMove.skill, context
				) else BattleStateMachine.CastSkill(
					state.onTurn, selectedMove.target.getTargets(state.onTurn, battle),
					selectedMove.skill, null, context
				)
				battle.confirmMove(context, nextMove)
			} else {
				context.soundQueue.insert(context.sounds.ui.clickReject)
			}
		}
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
		val playerState = context.characterStates[state.onTurn.player]!!
		val item = playerState.inventory.firstOrNull { it != null && it.item.consumable != null }
		if (item != null) {
			changeSelectedMove(battle, BattleMoveSelectionItem(item = item.item, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickConfirm)
		} else context.soundQueue.insert(context.sounds.ui.clickReject)
	}
	if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target == null) {
			val target = if (selectedMove.item.consumable!!.isPositive()) state.onTurn else firstEnemyTarget
			changeSelectedMove(battle, BattleMoveSelectionItem(selectedMove.item, target), context)
		} else {
			val playerState = context.characterStates[state.onTurn.player]!!
			if (!playerState.removeItem(selectedMove.item)) throw IllegalStateException()
			battle.confirmMove(context, BattleStateMachine.UseItem(
				state.onTurn, selectedMove.target, selectedMove.item
			))
		}
	}
	if (selectedMove is BattleMoveSelectionWait) battle.confirmMove(context, BattleStateMachine.Wait())
	if (selectedMove is BattleMoveSelectionFlee) battle.state = BattleStateMachine.RanAway()
}

internal fun battleCancel(battle: BattleState, context: BattleUpdateContext) {
	val selectedMove = (battle.state as BattleStateMachine.SelectMove).selectedMove
	if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
		changeSelectedMove(battle, BattleMoveSelectionAttack(target = null), context)
	} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(battle, BattleMoveSelectionSkill(skill = selectedMove.skill, target = null), context)
		} else {
			changeSelectedMove(battle, BattleMoveSelectionSkill(skill = null, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
		if (selectedMove.target != null) {
			changeSelectedMove(battle, BattleMoveSelectionItem(item = selectedMove.item, target = null), context)
		} else {
			changeSelectedMove(battle,BattleMoveSelectionItem(item = null, target = null), context)
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	} else {
		battle.confirmMove(context, BattleStateMachine.Wait())
	}
}

private fun changeSelectedMove(battle: BattleState, newMove: BattleMoveSelection, context: BattleUpdateContext) {
	val state = battle.state as BattleStateMachine.SelectMove
	val oldTargets = state.selectedMove.targets(battle)
	state.selectedMove = newMove
	val newTargets = newMove.targets(battle)

	if (oldTargets.isNotEmpty() && newTargets.isNotEmpty() && !oldTargets.contentEquals(newTargets)) {
		context.soundQueue.insert(context.sounds.ui.scroll1)
	}
	if (oldTargets.isEmpty() && newTargets.isNotEmpty()) context.soundQueue.insert(context.sounds.ui.clickConfirm)
	if (oldTargets.isNotEmpty() && newTargets.isEmpty()) context.soundQueue.insert(context.sounds.ui.clickCancel)

	for (target in newTargets) target.renderInfo.lastPointedTo = System.nanoTime()
}
