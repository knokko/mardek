package mardek.state.ingame.menu

import mardek.content.skill.*
import mardek.input.InputKey
import mardek.state.ingame.characters.CharacterSelectionState
import kotlin.math.max

class VisibleSkill(val skill: Skill, val mastery: Int, val canToggle: Boolean, val isToggled: Boolean)

class SkillsTab(characterSelection: CharacterSelectionState): InGameMenuTab(true) {

	var partyIndex = characterSelection.party.indexOfFirst { it != null }
	var skillTypeIndex = 0
	var skillIndex = 0

	override fun getText() = "Skills"

	private fun validatePartyIndex(context: UiUpdateContext) {
		if (context.characterSelection.party[partyIndex] == null) {
			partyIndex = context.characterSelection.party.indexOfFirst { it != null }
			if (partyIndex == -1) throw IllegalStateException("Party must have at least 1 character at all times")
		}
	}

	private fun validateSkillIndex(context: UiUpdateContext) {
		val numSkills = determineSkillList(context).size
		if (skillIndex >= numSkills) skillIndex = max(0, numSkills - 1)
	}

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		val party = context.characterSelection.party
		validatePartyIndex(context)

		if (inside) {
			if (key == InputKey.MoveLeft) {
				skillTypeIndex -= 1
				if (skillTypeIndex < 0) skillTypeIndex = 5
				validateSkillIndex(context)
				context.soundQueue.insert(context.sounds.ui.scroll2)
			}

			if (key == InputKey.MoveRight) {
				skillTypeIndex += 1
				if (skillTypeIndex > 5) skillTypeIndex = 0
				validateSkillIndex(context)
				context.soundQueue.insert(context.sounds.ui.scroll2)
			}

			val oldSkillIndex = skillIndex
			if (key == InputKey.MoveDown) {
				skillIndex += 1
				if (skillIndex >= determineSkillList(context).size) skillIndex = 0
			}

			if (key == InputKey.MoveUp) {
				skillIndex -= 1
				if (skillIndex < 0) skillIndex = max(0, determineSkillList(context).size - 1)
			}

			if (skillIndex != oldSkillIndex) context.soundQueue.insert(context.sounds.ui.scroll1)

			if (key == InputKey.Interact) {
				val visibleSkills = determineSkillList(context)
				if (visibleSkills.isNotEmpty()) {
					val entry = visibleSkills[skillIndex]
					if (entry.canToggle) {
						val assetCharacter = context.characterSelection.party[partyIndex]!!
						val characterState = context.characterStates[assetCharacter]!!
						if (characterState.toggledSkills.contains(entry.skill)) {
							characterState.toggledSkills.remove(entry.skill)
							context.soundQueue.insert(context.sounds.ui.clickCancel)
						} else {
							characterState.toggledSkills.add(entry.skill)
							context.soundQueue.insert(context.sounds.ui.toggleSkill)
						}
					} else context.soundQueue.insert(context.sounds.ui.clickReject)
				}
			}
		} else {
			val oldPartyIndex = partyIndex
			if (key == InputKey.MoveLeft) {
				partyIndex -= 1
				while (partyIndex >= 0 && party[partyIndex] == null) partyIndex -= 1
				if (partyIndex < 0) partyIndex = party.indexOfLast { it != null }
			}

			if (key == InputKey.MoveRight) {
				partyIndex += 1
				while (partyIndex < party.size && party[partyIndex] == null) partyIndex += 1
				if (partyIndex >= party.size) partyIndex = party.indexOfFirst { it != null }
			}

			if (partyIndex != oldPartyIndex) context.soundQueue.insert(context.sounds.ui.scroll1)
		}

		super.processKeyPress(key, context)
	}

	fun determineSkillList(context: UiUpdateContext): List<VisibleSkill> {
		validatePartyIndex(context)
		val assetCharacter = context.characterSelection.party[partyIndex]!!
		val characterState = context.characterStates[assetCharacter]!!

		fun filterVisibleSkill(skill: Skill): Boolean {
			val mastery = characterState.skillMastery[skill]
			if (mastery != null && mastery > 0) return true

			return characterState.equipment.any {
				item -> item?.equipment != null && item.equipment!!.skills.contains(skill)
			}
		}

		fun mapVisibleSkill(skill: Skill): VisibleSkill {
			val mastery = characterState.skillMastery[skill] ?: 0

			var canToggle = (if (mastery >= skill.masteryPoints) true else characterState.equipment.any {
				item -> item?.equipment != null && item.equipment!!.skills.contains(skill)
			}) && skill !is ActiveSkill
			if (canToggle && !characterState.toggledSkills.contains(skill)) {
				if (skill is PassiveSkill) {
					val requiredPoints = skill.enablePoints
					val remainingPoints = characterState.determineSkillEnablePoints() - characterState.toggledSkills.sumOf {
						if (it is PassiveSkill) it.enablePoints else 0
					}
					if (requiredPoints > remainingPoints) canToggle = false
				} else {
					val requiredPoints = (skill as ReactionSkill).enablePoints
					val remainingPoints = characterState.determineSkillEnablePoints() - characterState.toggledSkills.sumOf {
						if (it is ReactionSkill && it.type == skill.type) it.enablePoints else 0
					}
					if (requiredPoints > remainingPoints) canToggle = false
				}
			}
			val isToggled = characterState.toggledSkills.contains(skill)
			return VisibleSkill(skill = skill, mastery = mastery, canToggle = canToggle, isToggled = isToggled)
		}

		if (skillTypeIndex == 0) {
			return assetCharacter.characterClass.skillClass.actions.filter(::filterVisibleSkill).map(::mapVisibleSkill)
		}
		if (skillTypeIndex == 5) {
			return context.skills.passiveSkills.filter(::filterVisibleSkill).map(::mapVisibleSkill)
		}

		val reactionType = when (skillTypeIndex) {
			1 -> ReactionSkillType.MeleeAttack
			2 -> ReactionSkillType.MeleeDefense
			3 -> ReactionSkillType.RangedAttack
			4 -> ReactionSkillType.RangedDefense
			else -> throw IllegalStateException("Unexpected skill type index $skillTypeIndex")
		}
		return context.skills.reactionSkills.filter { it.type == reactionType && filterVisibleSkill(it) }.map(::mapVisibleSkill)
	}
}
