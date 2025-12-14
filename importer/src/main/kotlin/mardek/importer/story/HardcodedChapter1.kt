package mardek.importer.story

import mardek.content.Content
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.ItemStack
import mardek.content.skill.ReactionSkillType
import mardek.content.story.CustomTimelineVariable
import mardek.content.story.Quest
import mardek.content.story.Timeline
import mardek.content.story.TimelineAssignment
import mardek.content.story.TimelineCharacterStateValue
import mardek.content.story.TimelineIntValue
import mardek.content.story.TimelineNode
import mardek.content.story.TimelineOptionalPlayerValue
import mardek.content.story.TimelineStringValue
import mardek.content.story.TimelineUnitValue
import java.util.UUID

internal fun hardcodeChapter1Timeline(content: Content): TimelineNode {
	content.story.timelines.add(leadPipeTimeline(content))
	return TimelineNode(
		id = UUID.fromString("7fe3e164-1548-44e1-b004-d8c3752256cf"),
		name = "Chapter 1",
		children = arrayOf(
			dragonLairTimeline(content),
			childhoodTimeline(content),
		),
		variables = arrayOf(
			TimelineAssignment(content.story.fixedVariables.chapter, TimelineIntValue(1))
		),
		isAbstract = true,
	)
}

private fun getPlayer(content: Content, spriteName: String) = content.playableCharacters.find {
	it.areaSprites.name == spriteName
}!!

private fun dragonLairTimeline(content: Content) = TimelineNode(
	id = UUID.fromString("51453d24-a965-4166-9d72-14e3d369fb56"),
	name = "Dragon's Lair",
	children = arrayOf(),
	variables = arrayOf(
		TimelineAssignment(content.story.fixedVariables.blockItemStorage, TimelineUnitValue()),
		TimelineAssignment(content.story.fixedVariables.blockRandomBattleMusic, TimelineUnitValue()),
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[0],
			TimelineOptionalPlayerValue(getPlayer(content, "mardek_hero")),
		),
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[1],
			TimelineOptionalPlayerValue(getPlayer(content, "deugan_hero")),
		),
		TimelineAssignment(
			getPlayer(content, "mardek_hero").stateVariable,
			TimelineCharacterStateValue(initialHeroMardekState(content))
		),
		TimelineAssignment(
			getPlayer(content, "deugan_hero").stateVariable,
			TimelineCharacterStateValue(initialHeroDeuganState(content))
		),
		TimelineAssignment(
			content.story.quests.find { it.tabName == "Hero Quest!" }!!.isActive,
			TimelineUnitValue(),
		),
	),
	isAbstract = false,
)

private fun getItem(content: Content, name: String) = content.items.items.find { it.flashName == name }!!

private fun masterSkill(player: PlayableCharacter, state: CharacterState, skillName: String) {
	val skill = player.characterClass.skillClass.actions.find { it.name == skillName }!!
	state.skillMastery[skill] = skill.masteryPoints
}

private fun masterSkill(
	content: Content,
	type: ReactionSkillType?,
	state: CharacterState,
	skillName: String,
	toggle: Boolean = true,
) {
	val skill = if (type == null) content.skills.passiveSkills.find { it.name == skillName }!!
	else content.skills.reactionSkills.find { it.name == skillName && it.type == type }!!

	state.skillMastery[skill] = skill.masteryPoints
	if (toggle) state.toggledSkills.add(skill)
}

private fun initialHeroMardekState(content: Content): CharacterState {
	val heroMardek = getPlayer(content, "mardek_hero")
	val state = CharacterState()
	state.equipment[0] = getItem(content, "M Blade")
	state.equipment[1] = getItem(content, "Hero's Shield")
	state.equipment[3] = getItem(content, "Hero's Armour")
	state.equipment[4] = getItem(content, "Dragon Amulet")
	state.inventory[0] = ItemStack(getItem(content, "Elixir"), 9)
	masterSkill(heroMardek, state, "Shock")
	masterSkill(heroMardek, state, "Smite Evil")
	masterSkill(heroMardek, state, "Recover")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Stunstrike")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "DMG+50%")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Quarry: BEAST")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Quarry: DRAGON")
	masterSkill(content, ReactionSkillType.MeleeDefense, state, "Nullify Physical")
	masterSkill(content, ReactionSkillType.RangedDefense, state, "Nullify Magic")
	masterSkill(content, ReactionSkillType.RangedDefense, state, "Absorb MP")
	masterSkill(content, null, state, "HP+50%")
	masterSkill(content, null, state, "EXP+40%")
	masterSkill(content, null, state, "Auto-Regen")
	state.currentLevel = 50
	state.currentHealth = state.determineMaxHealth(heroMardek.baseStats, state.activeStatusEffects)
	state.currentMana = state.determineMaxMana(heroMardek.baseStats, state.activeStatusEffects)
	return state
}

private fun initialHeroDeuganState(content: Content): CharacterState {
	val heroDeugan = getPlayer(content, "deugan_hero")
	val state = CharacterState()
	state.equipment[0] = getItem(content, "Balmung")
	state.equipment[3] = getItem(content, "Hero's Coat")
	state.equipment[4] = getItem(content, "Dragon Amulet")
	state.inventory[0] = ItemStack(getItem(content, "Dragon Amulet"), 9)
	masterSkill(heroDeugan, state, "Shock")
	masterSkill(heroDeugan, state, "Pyromagia")
	masterSkill(heroDeugan, state, "Frostasia")
	masterSkill(heroDeugan, state, "Sunder")
	masterSkill(heroDeugan, state, "Recover")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Snakebite")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "DMG+50%")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Quarry: BEAST")
	masterSkill(content, ReactionSkillType.MeleeAttack, state, "Quarry: DRAGON")
	masterSkill(content, ReactionSkillType.MeleeDefense, state, "Nullify Physical")
	masterSkill(content, ReactionSkillType.RangedAttack, state, "M DMG+30%")
	masterSkill(content, ReactionSkillType.RangedDefense, state, "Nullify Magic")
	masterSkill(content, ReactionSkillType.RangedDefense, state, "Absorb MP")
	masterSkill(content, null, state, "HP+50%")
	masterSkill(content, null, state, "EXP+40%")
	masterSkill(content, null, state, "Auto-Regen")
	state.currentLevel = 50
	state.currentHealth = state.determineMaxHealth(heroDeugan.baseStats, state.activeStatusEffects)
	state.currentMana = state.determineMaxMana(heroDeugan.baseStats, state.activeStatusEffects)
	return state
}

private fun childhoodTimeline(content: Content) = TimelineNode(
	id = UUID.fromString("a46e576e-fa52-4712-896f-0aa54cee43ec"),
	name = "Childhood",
	children = arrayOf(
		beforeFallingStar(content),
		searchingFallenStar(content),
		afterRohoph(content),
	),
	variables = arrayOf(
		TimelineAssignment(
			content.story.quests.find { it.tabName == "Hero Quest!" }!!.wasCompleted,
			TimelineUnitValue(), appliesToFutureNodes = true,
		),
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[0],
			TimelineOptionalPlayerValue(getPlayer(content, "mardek_child")),
		),
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[1],
			TimelineOptionalPlayerValue(getPlayer(content, "deugan_child")),
		),
		TimelineAssignment(
			getPlayer(content, "mardek_child").stateVariable,
			TimelineCharacterStateValue(initialChildMardekState(content))
		),
		TimelineAssignment(
			getPlayer(content, "deugan_child").stateVariable,
			TimelineCharacterStateValue(initialChildDeuganState(content))
		),
		addWorldMapNode(content, "Heroes' Den"),
		addWorldMapNode(content, "Goznor")
	),
	isAbstract = true,
)

private fun addWorldMapNode(content: Content, name: String) = TimelineAssignment(
	content.worldMaps.find { it.name == "Belfan" }!!.nodes.find {
		it.area.properties.displayName == name
	}!!.wasDiscovered,
	TimelineUnitValue(),
	appliesToFutureNodes = true,
)

private fun initialChildMardekState(content: Content): CharacterState {
	val childMardek = getPlayer(content, "mardek_child")
	val state = CharacterState()
	state.equipment[0] = getItem(content, "Stick")
	state.equipment[3] = getItem(content, "Tunic")
	state.currentLevel = 1
	state.currentHealth = state.determineMaxHealth(childMardek.baseStats, state.activeStatusEffects)
	state.currentMana = state.determineMaxMana(childMardek.baseStats, state.activeStatusEffects)
	return state
}

private fun initialChildDeuganState(content: Content): CharacterState {
	val childDeugan = getPlayer(content, "deugan_child")
	val state = CharacterState()
	state.equipment[0] = getItem(content, "Big Stick")
	state.equipment[3] = getItem(content, "Tunic")
	state.currentLevel = 1
	state.currentHealth = state.determineMaxHealth(childDeugan.baseStats, state.activeStatusEffects)
	state.currentMana = state.determineMaxMana(childDeugan.baseStats, state.activeStatusEffects)
	return state
}

@Suppress("UNCHECKED_CAST")
private fun beforeFallingStar(content: Content) = TimelineNode(
	id = UUID.fromString("f6877e8f-2111-47f0-95e9-4edc546efa4c"),
	name = "Night before the falling 'star'",
	children = arrayOf(
		droppedDeuganBeforeFallingStar(content),
	),
	variables = arrayOf(
		TimelineAssignment(
			content.story.customVariables.find { it.name == "TimeOfDay" }!! as CustomTimelineVariable<String>,
			TimelineStringValue("Evening")
		),
	),
	isAbstract = false,
)

private fun droppedDeuganBeforeFallingStar(content: Content) = TimelineNode(
	id = UUID.fromString("add16e1a-129a-4f3a-a769-d92dbf37307a"),
	name = "Dropped Deugan home before the falling 'star'",
	children = arrayOf(),
	variables = arrayOf(
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[1],
			TimelineOptionalPlayerValue(null),
			priority = 1,
		),
		TimelineAssignment(
			content.playableCharacters.find { it.areaSprites.name == "deugan_child" }!!.isInventoryAvailable,
			TimelineUnitValue(),
		),
	),
	isAbstract = false,
)

private fun searchingFallenStar(content: Content) = TimelineNode(
	id = UUID.fromString("388e0bde-5d6c-4b9b-b2b2-bb99bbaa2dbb"),
	name = "Searching for the fallen 'star'",
	children = arrayOf(),
	variables = arrayOf(
		addWorldMapNode(content, "Soothwood"),
		TimelineAssignment(
			content.story.quests.find { it.tabName == "The Fallen Star" }!!.isActive,
			TimelineUnitValue(),
		),
	),
	activatesTimelines = arrayOf(content.story.timelines.find { it.name == "LeadPipeQuestTimeline" }!!),
	isAbstract = false,
)

private fun leadPipeTimeline(content: Content): Timeline {
	val quest = content.story.quests.find { it.tabName == "LeadPipes" }!!
	val rootNode = TimelineNode(
		id = UUID.fromString("2ee990c4-7d98-49be-bea8-07c270d43d24"),
		name = "LeadPipeQuest root",
		children = arrayOf(
			leadPipeAccepted(quest),
			leadPipeFinished(quest),
		),
		variables = arrayOf(),
		isAbstract = false,
	)
	return Timeline(
		id = UUID.fromString("ce185c61-3afe-4621-a90e-c92ee51db698"),
		name = "LeadPipeQuestTimeline",
		root = rootNode,
		needsActivation = true,
	)
}

private fun leadPipeAccepted(quest: Quest) = TimelineNode(
	id = UUID.fromString("c356db5c-2a14-4da0-8137-227b466272db"),
	name = "Accepted Pipe Quest",
	children = arrayOf(),
	variables = arrayOf(
		TimelineAssignment(quest.isActive, TimelineUnitValue())
	),
	isAbstract = false,
)

private fun leadPipeFinished(quest: Quest) = TimelineNode(
	id = UUID.fromString("e7560142-96b4-40a8-a47b-7277e8e59402"),
	name = "Finished Pipe Quest",
	children = arrayOf(),
	variables = arrayOf(
		TimelineAssignment(quest.wasCompleted, TimelineUnitValue())
	),
	ignoresTimelineActivation = true,
	isAbstract = false,
)

@Suppress("UNCHECKED_CAST")
private fun afterRohoph(content: Content) = TimelineNode(
	id = UUID.fromString("d0b90774-17e8-4ef3-82cc-0af7d799184a"),
	name = "After the conversation in Rohophs saucer is finished",
	children = arrayOf(
		droppedDeuganAfterRohoph(content)
	),
	variables = arrayOf(
		TimelineAssignment(
			content.story.customVariables.find { it.name == "TimeOfDay" }!! as CustomTimelineVariable<String>,
			TimelineStringValue("Evening")
		),
		TimelineAssignment(
			content.story.quests.find { it.tabName == "The Fallen Star" }!!.wasCompleted,
			TimelineUnitValue(),
			appliesToFutureNodes = true,
		),
	),
	isAbstract = false,
)

private fun droppedDeuganAfterRohoph(content: Content) = TimelineNode(
	id = UUID.fromString("e5da09cf-1663-4d82-be98-0d6ec6abe346"),
	name = "Dropped Deugan home before after Rohoph entered Mardeks body",
	children = arrayOf(),
	variables = arrayOf(
		TimelineAssignment(
			content.story.fixedVariables.forcedPartyMembers[1],
			TimelineOptionalPlayerValue(null),
		),
		TimelineAssignment(
			content.playableCharacters.find { it.areaSprites.name == "deugan_child" }!!.isInventoryAvailable,
			TimelineUnitValue(),
		),
	),
	isAbstract = false,
)
