package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.FixedActionNode
import mardek.game.TestingInstance
import org.junit.jupiter.api.Assertions.assertEquals

object TestAccessoryShop {

	fun testReplacePCWithPlayerDialogue(instance: TestingInstance) {
		instance.apply {
			val area = content.areas.areas.find { it.properties.rawName == "gz_shop_Ac" }!!
			val shopkeeper = area.objects.characters[0]
			val actions = shopkeeper.ownActions!!
			assertEquals(1, actions.getAllChildNodes().count {
				it is FixedActionNode && it.action is ActionTalk &&
						(it.action as ActionTalk).speaker is ActionTargetPartyMember
			})
		}
	}
}
