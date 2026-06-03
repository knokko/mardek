package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.content.Content
import mardek.content.action.DialogueTextStyle

internal fun hardcodeDialogueTextStyles(content: Content) {
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_light",
		font = content.fonts.gdm,
		fillColor = rgb(255, 255, 255),
		strokeColor = rgba(255, 255, 255, 100),
		strokeWidth = 0.35f,
		strokeDistancePower = 3f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_water",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(160, 250, 250)),
		strokeColor = srgbToLinear(rgb(0, 170, 200)),
		strokeWidth = 0.35f,
		strokeDistancePower = 2.5f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_fire",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(250, 150, 5)),
		strokeColor = srgbToLinear(rgb(200, 25, 10)),
		strokeWidth = 0.35f,
		strokeDistancePower = 3f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_air",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(255, 255, 200)),
		strokeColor = srgbToLinear(rgb(190, 180, 50)),
		strokeWidth = 0.35f,
		strokeDistancePower = 2.5f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_earth",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(15, 250, 10)),
		strokeColor = srgbToLinear(rgb(50, 150, 20)),
		strokeWidth = 0.35f,
		strokeDistancePower = 2.5f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_ether",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(5, 250, 150)),
		strokeColor = srgbToLinear(rgb(30, 140, 95)),
		strokeWidth = 0.35f,
		strokeDistancePower = 2f,
	))
	content.actions.dialogueTextStyles.add(DialogueTextStyle(
		name = "gdm_dark",
		font = content.fonts.gdm,
		fillColor = srgbToLinear(rgb(0, 0, 0)),
		strokeColor = srgbToLinear(rgb(50, 20, 60)),
		strokeWidth = 0.35f,
		strokeDistancePower = 2f,
	))
}
