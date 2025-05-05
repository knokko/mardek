package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.SkillTargetType
import mardek.content.stats.CombatStat
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.characters.CharacterState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class BattleState(
	@BitField(id = 0)
	val battle: Battle,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceField(stable = true, label = "playable characters")
	val players: Array<PlayableCharacter?>,

	@BitField(id = 2)
	val playerLayout: PartyLayout,

	campaignState: CampaignState,
) {

	val allPossibleCombatants = (0 until 4).flatMap { listOf(
		CombatantReference(false, it, this), CombatantReference(true, it, this)
	) }

	@BitField(id = 3)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val enemyStates = Array(4) { index ->
		val enemy = battle.enemies[index] ?: return@Array null
		CombatantState(enemy)
	}

	@BitField(id = 4)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val playerStates = Array(4) { index ->
		val player = players[index] ?: return@Array null
		CombatantState(player, campaignState.characterStates[player]!!)
	}

	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "combatants")
	var onTurn: CombatantReference? = null

	@BitField(id = 6) // TODO right annotation
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking

	var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(target = null)

	@BitField(id = 7)
	var outcome = BattleOutcome.Busy
		private set

	val startTime = System.nanoTime()

	private var updatedTime = 0.seconds
	private var moveDecisionTime = 0.seconds

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), CampaignState())

	private fun changeSelectedMove(newMove: BattleMoveSelection, soundQueue: SoundQueue) {
		val oldTargets = this.selectedMove.targets(this)
		this.selectedMove = newMove
		val newTargets = newMove.targets(this)

		if (oldTargets.isNotEmpty() && newTargets.isNotEmpty() && !oldTargets.contentEquals(newTargets)) {
			soundQueue.insert("menu-scroll")
		}
		if (oldTargets.isEmpty() && newTargets.isNotEmpty()) soundQueue.insert("click-confirm")
		if (oldTargets.isNotEmpty() && newTargets.isEmpty()) soundQueue.insert("click-cancel")

		for (target in newTargets) target.getState().lastPointedTo = System.nanoTime()
	}

	private fun confirmMove(chosenMove: BattleMove, soundQueue: SoundQueue) {
		this.selectedMove = BattleMoveSelectionAttack(target = null)
		this.currentMove = chosenMove
		this.moveDecisionTime = updatedTime
		if (currentMove is BattleMoveWait) soundQueue.insert("click-cancel")
		else soundQueue.insert("click-confirm")
	}

	fun processKeyPress(
		key: InputKey, characterStates: HashMap<PlayableCharacter, CharacterState>, soundQueue: SoundQueue
	) {
		val onTurn = this.onTurn
		if (onTurn != null && currentMove == BattleMoveThinking) {
			val selectedMove = this.selectedMove
			if (key == InputKey.Cancel) {
				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
					changeSelectedMove(BattleMoveSelectionAttack(target = null), soundQueue)
				} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
					if (selectedMove.target != null) {
						changeSelectedMove(BattleMoveSelectionSkill(skill = selectedMove.skill, target = null), soundQueue)
					} else {
						changeSelectedMove(BattleMoveSelectionSkill(skill = null, target = null), soundQueue)
					}
				} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
					if (selectedMove.target != null) {
						changeSelectedMove(BattleMoveSelectionItem(item = selectedMove.item, target = null), soundQueue)
					} else {
						changeSelectedMove(BattleMoveSelectionItem(item = null, target = null), soundQueue)
					}
				} else {
					this.currentMove = BattleMoveWait
					moveDecisionTime = updatedTime
					soundQueue.insert("click-cancel")
				}
			}

			val firstEnemyTarget = CombatantReference(
				isPlayer = false, index = enemyStates.indexOfFirst { it != null }, this
			)
			val firstPlayerTarget = CombatantReference(
				isPlayer = true, index = playerStates.indexOfFirst { it != null }, this
			)
			if (key == InputKey.Interact) {
				if (selectedMove is BattleMoveSelectionAttack) {
					if (selectedMove.target == null) {
						changeSelectedMove(BattleMoveSelectionAttack(target = firstEnemyTarget), soundQueue)
					} else {
						confirmMove(BattleMoveBasicAttack(selectedMove.target), soundQueue)
					}
				}
				if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
					val player = players[onTurn.index]!!
					val playerState = characterStates[player]!!
					val skill = player.characterClass.skillClass.actions.firstOrNull { playerState.canCastSkill(it) }
					if (skill != null) {
						changeSelectedMove(BattleMoveSelectionSkill(skill = skill, target = null), soundQueue)
						soundQueue.insert("click-confirm")
					} else soundQueue.insert("click-reject")
				}
				if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
					if (selectedMove.target == null) {
						val firstTarget = when (selectedMove.skill.targetType) {
							SkillTargetType.Self -> BattleSkillTargetSingle(onTurn)
							SkillTargetType.AllEnemies -> BattleSkillTargetAllEnemies
							SkillTargetType.AllAllies -> BattleSkillTargetAllAllies
							else -> BattleSkillTargetSingle(
								if (selectedMove.skill.isPositive()) firstPlayerTarget else firstEnemyTarget
							)
						}
						changeSelectedMove(BattleMoveSelectionSkill(selectedMove.skill, firstTarget), soundQueue)
					} else {
						var manaCost = selectedMove.skill.manaCost
						if (selectedMove.target is BattleSkillTargetAllAllies || selectedMove.target is BattleSkillTargetAllEnemies) {
							manaCost *= 2
						}

						val playerState = onTurn.getState()
						if (manaCost <= playerState.currentMana) {
							playerState.currentMana -= manaCost
							confirmMove(BattleMoveSkill(selectedMove.skill, selectedMove.target), soundQueue)
						} else {
							soundQueue.insert("click-reject")
						}
					}
				}
				if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
					val player = players[onTurn.index]!!
					val playerState = characterStates[player]!!
					val item = playerState.inventory.firstOrNull { it != null && it.item.consumable != null }
					if (item != null) {
						changeSelectedMove(BattleMoveSelectionItem(item = item.item, target = null), soundQueue)
						soundQueue.insert("click-confirm")
					} else soundQueue.insert("click-reject")
				}
				if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
					soundQueue.insert("click-confirm")
					if (selectedMove.target == null) {
						val target = if (selectedMove.item.consumable!!.isPositive()) firstPlayerTarget else firstEnemyTarget
						changeSelectedMove(BattleMoveSelectionItem(selectedMove.item, target), soundQueue)
					} else {
						val player = players[onTurn.index]!!
						val playerState = characterStates[player]!!
						if (!playerState.removeItem(selectedMove.item)) throw IllegalStateException()
						confirmMove(BattleMoveItem(selectedMove.item, selectedMove.target), soundQueue)
					}
				}
				if (selectedMove is BattleMoveSelectionWait) confirmMove(BattleMoveWait, soundQueue)
				if (selectedMove is BattleMoveSelectionFlee) {
					outcome = BattleOutcome.RanAway
					moveDecisionTime = updatedTime
				}
			}

			if (key == InputKey.MoveLeft || key == InputKey.MoveRight) {
				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target == null) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
					else this.selectedMove = BattleMoveSelectionFlee
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
					if (key == InputKey.MoveLeft && selectedMove.target.isPlayer) {
						val nextIndex = closestTarget(
							playerLayout.positions[selectedMove.target.index], enemyStates, battle.enemyLayout
						)
						changeSelectedMove(BattleMoveSelectionAttack(
							CombatantReference(false, nextIndex, this)
						), soundQueue)
					}
					if (key == InputKey.MoveRight && !selectedMove.target.isPlayer) {
						val nextIndex = closestTarget(
							battle.enemyLayout.positions[selectedMove.target.index], playerStates, playerLayout
						)
						changeSelectedMove(BattleMoveSelectionAttack(
							CombatantReference(true, nextIndex, this)
						), soundQueue)
					}
				}
				if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
					if (key == InputKey.MoveLeft) changeSelectedMove(BattleMoveSelectionItem(item = null, target = null), soundQueue)
					else changeSelectedMove(BattleMoveSelectionAttack(target = null), soundQueue)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null && selectedMove.target != null) {
					if (selectedMove.target is BattleSkillTargetSingle) {
						if (key == InputKey.MoveLeft && selectedMove.target.target.isPlayer && selectedMove.skill.targetType != SkillTargetType.Self) {
							val nextIndex = closestTarget(
								playerLayout.positions[selectedMove.target.target.index], enemyStates, battle.enemyLayout
							)
							changeSelectedMove(BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
								CombatantReference(false, nextIndex, this)
							)), soundQueue)
						}
						if (key == InputKey.MoveLeft && !selectedMove.target.target.isPlayer &&
								selectedMove.skill.targetType == SkillTargetType.Any && enemyStates.count { it != null } > 1
						) {
							changeSelectedMove(BattleMoveSelectionSkill(
								selectedMove.skill, BattleSkillTargetAllEnemies
							), soundQueue)
						}
						if (key == InputKey.MoveRight && !selectedMove.target.target.isPlayer) {
							val nextIndex = closestTarget(
								battle.enemyLayout.positions[selectedMove.target.target.index], playerStates, playerLayout
							)
							changeSelectedMove(BattleMoveSelectionSkill(selectedMove.skill, BattleSkillTargetSingle(
								CombatantReference(true, nextIndex, this)
							)), soundQueue)
						}
						if (key == InputKey.MoveRight && selectedMove.target.target.isPlayer &&
								selectedMove.skill.targetType == SkillTargetType.Any && playerStates.count { it != null } > 1
						) {
							changeSelectedMove(BattleMoveSelectionSkill(
								selectedMove.skill, BattleSkillTargetAllAllies
							), soundQueue)
						}
					}
					if (selectedMove.target is BattleSkillTargetAllAllies &&
							selectedMove.skill.targetType != SkillTargetType.AllAllies && key == InputKey.MoveLeft
					) {
						changeSelectedMove(BattleMoveSelectionSkill(
							selectedMove.skill, BattleSkillTargetSingle(firstPlayerTarget)
						), soundQueue)
					}
					if (selectedMove.target is BattleSkillTargetAllEnemies &&
							selectedMove.skill.targetType != SkillTargetType.AllEnemies && key == InputKey.MoveRight
					) {
						changeSelectedMove(BattleMoveSelectionSkill(
							selectedMove.skill, BattleSkillTargetSingle(firstEnemyTarget)
						), soundQueue)
					}
				}
				if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
					if (key == InputKey.MoveLeft) changeSelectedMove(BattleMoveSelectionWait, soundQueue)
					else changeSelectedMove(BattleMoveSelectionSkill(skill = null, target = null), soundQueue)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionItem && selectedMove.target != null) {
					if (key == InputKey.MoveLeft && selectedMove.target.isPlayer) {
						val nextIndex = closestTarget(
							playerLayout.positions[selectedMove.target.index], enemyStates, battle.enemyLayout
						)
						changeSelectedMove(BattleMoveSelectionItem(
							selectedMove.item, CombatantReference(false, nextIndex, this)
						), soundQueue)
					}
					if (key == InputKey.MoveRight && !selectedMove.target.isPlayer) {
						val nextIndex = closestTarget(
							battle.enemyLayout.positions[selectedMove.target.index], playerStates, playerLayout
						)
						changeSelectedMove(BattleMoveSelectionItem(
							selectedMove.item, CombatantReference(true, nextIndex, this)
						), soundQueue)
					}
				}
				if (selectedMove is BattleMoveSelectionWait) {
					if (key == InputKey.MoveLeft) changeSelectedMove(BattleMoveSelectionFlee, soundQueue)
					else changeSelectedMove(BattleMoveSelectionItem(item = null, target = null), soundQueue)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionFlee) {
					if (key == InputKey.MoveLeft) changeSelectedMove(BattleMoveSelectionAttack(target = null), soundQueue)
					else changeSelectedMove(BattleMoveSelectionWait, soundQueue)
					soundQueue.insert("menu-scroll")
				}
			}

			if (key == InputKey.MoveUp || key == InputKey.MoveDown) {
				val player = players[onTurn.index]!!
				val playerState = characterStates[player]!!

				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
					val newTarget = nextTarget(key, selectedMove.target, this)
					if (newTarget.index != selectedMove.target.index) {
						changeSelectedMove(BattleMoveSelectionAttack(newTarget), soundQueue)
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
							changeSelectedMove(BattleMoveSelectionSkill(skill = skills[index], target = null), soundQueue)
							soundQueue.insert("menu-scroll")
						}
					} else {
						if (selectedMove.skill.targetType != SkillTargetType.Self && selectedMove.target is BattleSkillTargetSingle) {
							val newTarget = nextTarget(key, selectedMove.target.target, this)
							if (newTarget.index != selectedMove.target.target.index) {
								changeSelectedMove(BattleMoveSelectionSkill(
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
							changeSelectedMove(BattleMoveSelectionItem(item = items[index], target = null), soundQueue)
							soundQueue.insert("menu-scroll")
						}
					} else {
						val newTarget = nextTarget(key, selectedMove.target, this)
						if (newTarget.index != selectedMove.target.index) {
							changeSelectedMove(BattleMoveSelectionItem(selectedMove.item, newTarget), soundQueue)
						}
					}
				}
			}
		}
	}

	internal fun getPlayerStat(stat: CombatStat, index: Int): Int {
		val player = players[index]!!
		val state = playerStates[index]!!
		val base = player.baseStats.find { it.stat == stat }?.adder ?: 0
		val extra = state.statModifiers[stat] ?: 0
		return base + extra
	}

	internal fun getMonsterStat(stat: CombatStat, index: Int): Int {
		val monster = battle.enemies[index]!!.monster
		val state = enemyStates[index]!!
		val base = monster.baseStats[stat] ?: 0
		val extra = state.statModifiers[stat] ?: 0
		return base + extra
	}

	private fun updateOnTurn(soundQueue: SoundQueue) {
		val combatants = allPossibleCombatants.filter { it.isAlive() }
		if (combatants.none { it.isPlayer }) outcome = BattleOutcome.GameOver
		if (combatants.none { !it.isPlayer }) outcome = BattleOutcome.Victory
		if (outcome != BattleOutcome.Busy) return

		val simulator = TurnOrderSimulator(this)
		if (simulator.checkReset()) {
			for (combatant in combatants) combatant.getState().spentTurnsThisRound = 0
		}
		beginTurn(soundQueue, simulator.next()!!)
	}

	private fun beginTurn(soundQueue: SoundQueue, combatant: CombatantReference) {
		combatant.getState().spentTurnsThisRound += 1
		// TODO Allow status effects to skip the turn
		onTurn = combatant

		currentMove = if (combatant.isPlayer) {
			soundQueue.insert("menu-party-scroll")
			selectedMove = BattleMoveSelectionAttack(target = null)
			BattleMoveThinking
		} else {
			moveDecisionTime = updatedTime
			BattleMoveWait
		}
	}

	fun update(
		characterStates: HashMap<PlayableCharacter, CharacterState>,
		soundQueue: SoundQueue, timeStep: Duration
	) {
		updatedTime += timeStep
		while (onTurn == null && outcome == BattleOutcome.Busy) updateOnTurn(soundQueue)
		if (outcome != BattleOutcome.Busy) return

		if (currentMove == BattleMoveWait && updatedTime > moveDecisionTime + 500.milliseconds) {
			onTurn = null
		}
	}
}
