package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.*
import mardek.content.story.DefinedVariableTimelineCondition
import mardek.content.story.NegateTimelineCondition
import java.util.UUID

internal fun hardcodeSaveCrystalActions(content: Content, hardcoded: Map<String, MutableList<ActionSequence>>) {
	val entryRoot = FixedActionNode(
		id = UUID.fromString("c9af39f3-f5a3-4db8-9c06-17a9f1d97a78"),
		action = ActionHealParty(),
		next = FixedActionNode(
			id = UUID.fromString("15a6b7b7-ddd6-4d65-9122-616f43f7e9ac"),
			action = ActionPlaySound(content.audio.fixedEffects.saveCrystal),
			next = FixedActionNode(
				id = UUID.fromString("57562f71-126d-4905-8987-354196f1df30"),
				action = ActionFlashScreen(rgb(10, 250, 255)),
				next = FixedActionNode(
					id = UUID.fromString("8d555bcd-9568-491d-81c8-8e5c663a0b00"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "",
						text = "A soothing light washes over you... Your wounds are healed. Would you like to save?",
					),
					next = ChoiceActionNode(
						id = UUID.fromString("6c7a13b7-7242-4557-9926-104f71d5b6aa"),
						speaker = ActionTargetPartyMember(0),
						options = arrayOf(
							ChoiceEntry("norm", text = "Save...", next = FixedActionNode(
								id = UUID.fromString("48cc1a8a-99d9-484d-984f-90d4c2de28aa"),
								action = ActionSaveCampaign(),
								next = null
							)),
							ChoiceEntry("norm", text = "Item storage...", next = FixedActionNode(
								id = UUID.fromString("80617055-9f71-40d3-b115-3786738c7377"),
								action = ActionItemStorage(),
								next = null
							), condition = NegateTimelineCondition(DefinedVariableTimelineCondition(
								content.story.fixedVariables.blockItemStorage
							))),
							ChoiceEntry("norm", text = "Exit...", next = null)
						)
					)
				)
			)
		)
	)

	hardcoded[""]!!.add(ActionSequence("c_healingCrystal", entryRoot))
}
