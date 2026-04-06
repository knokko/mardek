package mardek.state.ingame.menu

import mardek.content.skill.*
import mardek.input.InputKey
import mardek.state.ingame.UsedPartyMember
import kotlin.math.max

/**
 * This class is used as return type for [SkillsTab.determineSkillList], and represents a skill that the player can see
 * (because the player has at least 1 mastery point in the skill, or because the equipment allows it).
 */
class VisibleSkill(
	val skill: Skill,

	/**
	 * The number of mastery points that the character has for this skill
	 */
	val mastery: Int,

	/**
	 * Whether the skill can be toggled *now*. This will always be `false` for [ActiveSkill]s.
	 */
	val canToggle: Boolean,

	/**
	 * Whether the skill is currently toggled. This will always be `false` for [ActiveSkill]s.
	 */
	val isToggled: Boolean,
)

/**
 * The "Skills" tab of the in-game menu.
 *
 * This class tracks at which skills the player is looking (e.g. the Passive skills of Deugan).
 *
 * Note that this class does *not* track which skills are currently toggled: these are stored in the
 * [mardek.content.characters.CharacterState]s.
 */
class SkillsTab(party: List<UsedPartyMember>): InGameMenuTab() {

	/**
	 * The index (into [mardek.state.ingame.CampaignState.party]) of the selected party member (whose skills are
	 * being shown)
	 */
	var partyIndex = party[0].index

	/**
	 * The 'skill type index' of the skills that the player is viewing:
	 * - 0 for active skills
	 * - 1 for melee attack reaction skills
	 * - 2 for melee defense reaction skills
	 * - 3 for magic attack reaction skills
	 * - 4 for magic defense reaction skills
	 * - 5 for passive skills
	 *
	 * Note that the value of this field is only meaningful when `inside` is `true`.
	 */
	var skillTypeIndex = 0

	/**
	 * The index (into [determineSkillList]) of the currently-selected skill.
	 *
	 * Note that the value of this field is only meaningful when `inside` is `true`.
	 *
	 * **Note that this index can be out-of-bounds!** This is unavoidable, since [determineSkillList] may return an
	 * empty list.
	 */
	var skillIndex = 0

	override fun getText() = "Skills"

	override fun canGoInside() = true

	private fun validatePartyIndex(context: UiUpdateContext) {
		if (context.fullParty[partyIndex] == null) partyIndex = context.usedParty[0].index
	}

	private fun validateSkillIndex(context: UiUpdateContext) {
		val numSkills = determineSkillList(context).size
		if (skillIndex >= numSkills) skillIndex = max(0, numSkills - 1)
	}

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
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
						val characterState = context.fullParty[partyIndex]!!.second
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
				while (partyIndex >= 0 && context.fullParty[partyIndex] == null) partyIndex -= 1
				if (partyIndex < 0) partyIndex = context.usedParty.last().index
			}

			if (key == InputKey.MoveRight) {
				partyIndex += 1
				while (partyIndex < context.fullParty.size && context.fullParty[partyIndex] == null) partyIndex += 1
				if (partyIndex >= context.fullParty.size) partyIndex = context.usedParty[0].index
			}

			if (partyIndex != oldPartyIndex) context.soundQueue.insert(context.sounds.ui.scroll1)
		}

		super.processKeyPress(key, context)
	}

	/**
	 * Determines which skills the player can currently see. This contains all skills of the right type such that either
	 * - The currently-selected playable character has at least 1 mastery point in the skill
	 * (see [mardek.content.characters.CharacterState.skillMastery]), or
	 * - The currently-selected playable character has equipped an item that allows the skill to be learned.
	 */
	fun determineSkillList(context: UiUpdateContext): List<VisibleSkill> {
		validatePartyIndex(context)
		val (assetCharacter, characterState) = context.fullParty[partyIndex]!!

		fun filterVisibleSkill(skill: Skill): Boolean {
			val mastery = characterState.skillMastery[skill]
			if (mastery != null && mastery > 0) return true

			return characterState.equipment.values.any {
				item -> item.equipment != null && item.equipment!!.skills.contains(skill)
			}
		}

		fun mapVisibleSkill(skill: Skill): VisibleSkill {
			val mastery = characterState.skillMastery[skill] ?: 0

			var canToggle = (if (mastery >= skill.masteryPoints) true else characterState.equipment.values.any {
				item -> item.equipment != null && item.equipment!!.skills.contains(skill)
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
