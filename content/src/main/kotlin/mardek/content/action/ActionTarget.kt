package mardek.content.action

import mardek.content.characters.PlayableCharacter

sealed class ActionTarget {
}

class ActionTargetPartyMember(val index: Int) : ActionTarget()

class ActionTargetPlayer(val player: PlayableCharacter) : ActionTarget()

class ActionTargetWholeParty : ActionTarget()
