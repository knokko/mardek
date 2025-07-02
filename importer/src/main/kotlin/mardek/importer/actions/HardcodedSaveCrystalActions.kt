package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.*

internal fun hardcodeSaveCrystalActions(content: Content, hardcoded: Map<String, MutableList<ActionSequence>>) {
	val entryRoot = FixedActionNode(
		action = ActionHealParty(),
		next = FixedActionNode(
			action = ActionPlaySound(content.audio.fixedEffects.saveCrystal),
			next = FixedActionNode(
				action = ActionFlashScreen(rgb(10, 250, 255)),
				next = FixedActionNode(
					action = ActionTalk(
						speaker = ActionTargetDialogueObject("Save Crystal"),
						expression = "",
						text = "A soothing light washes over you... Your wounds are healed. Would you like to save?",
					),
					next = ChoiceActionNode(
						speaker = ActionTargetPartyMember(0),
						expression = "norm",
						options = arrayOf(
							ChoiceEntry(text = "Save...", next = FixedActionNode(
								action = ActionSaveCampaign(),
								next = null
							)),
							ChoiceEntry(text = "Exit...", next = null)
						)
					)
				)
			)
		)
	)

	hardcoded[""]!!.add(ActionSequence("c_healingCrystal", entryRoot))
}
