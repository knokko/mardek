package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.ActionFlashScreen
import mardek.content.action.ActionHealParty
import mardek.content.action.ActionPlaySound
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.FixedActionNode
import java.util.UUID

internal fun hardcodeGallovarDialogues(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val targetGallovar = ActionTargetDefaultDialogueObject()
	val targetMardek = ActionTargetPartyMember(0)

	// TODO CHAP2 Add zombie outbreak dialogue
	// TODO CHAP3 Add trade quest dialogue
	val entryRoot = FixedActionNode(
		id = UUID.fromString("be8a2fe7-af10-4a47-a518-440f528435b3"),
		action = ActionTalk(targetGallovar, "smile", "Hello there, lads. " +
				"May YALORT not smite you this day. ...Can I be of particular assistance?"
		),
		next = ChoiceActionNode(
			id = UUID.fromString("a0f9abb1-9cb1-4826-bccd-d45fb574f7b3"),
			speaker = targetMardek,
			options = arrayOf(
				ChoiceEntry("susp", "Well, I need healing...", next = fixedActionChain(
					actions = arrayOf(
						ActionHealParty(),
						ActionPlaySound(content.audio.fixedEffects.saveCrystal),
						ActionFlashScreen(rgb(10, 250, 255)),
						ActionTalk(
							speaker = targetGallovar,
							expression = "smile",
							text = "By the Powers of YALORT, your wounds are gone. Fare thee well, children!",
						)
					),
					ids = arrayOf(
						UUID.fromString("38772812-94f2-4b80-86df-00f2d1c9e510"),
						UUID.fromString("ac85bca9-4825-4c1b-9dad-fa1482240b6e"),
						UUID.fromString("39bce653-39f0-450f-b8d9-fce3d7cfeec1"),
						UUID.fromString("eddb81c7-9c6b-403c-8081-8bc31f78f8a5"),
					),
				)),
				ChoiceEntry("susp", "Who's YALORT?", next = fixedActionChain(
					actions = arrayOf(
						ActionTalk(targetGallovar, "susp", "Surely you jest, children? " +
								"EVERYONE knows that YALORT is the One True Deity and our eternal benefactor! " +
								"He is the Eternal Dragon who created the world, " +
								"and grants priests such as myself magical power! You should know this, children!"
						),
						ActionTalk(targetMardek, "deep", "Well, I was just being silly..."),
						ActionTalk(targetGallovar, "norm",
							"Yes, I thought as much. But no worry! A sense of humour is a good thing!"
						),
					),
					ids = arrayOf(
						UUID.fromString("fdfd3655-ca74-426b-b1ef-f0128af5cad2"),
						UUID.fromString("3a2914f4-6033-4bf9-be30-81b0dc0a4965"),
						UUID.fromString("f2dd51fd-7873-4a83-82c0-9c6edf576ff9"),
					),
				)),
			)
		)
	)

	hardcoded["gz_monastery"] = mutableListOf(
		ActionSequence(name = "gallovar", root = entryRoot)
	)
}
