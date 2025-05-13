package mardek.importer.audio

import mardek.content.audio.*

private fun importFixed(name: String) = SoundEffect(
	flashName = name,
	oggData = importSoundData(name)
)

internal fun importFixedSoundEffects(audio: AudioContent) {
	audio.fixedEffects = FixedSoundEffects(
		ui = UiSoundEffects(
			clickConfirm = importFixed("4_sfx_menuBlip4"),
			clickCancel = importFixed("5_sfx_menuBlip3"),
			clickReject = importFixed("5400_sfx_error"),
			scroll = importFixed("7_sfx_menuBlip1"),
			partyScroll = importFixed("2_sfx_menuSwish"),
			toggleSkill = importFixed("6_sfx_menuBlip2"),
			openMenu = importFixed("3_sfx_menuOpen"),
		),
		battle = BattleSoundEffects(
			flee = importFixed("sfx_Escape"),
			punch = importFixed("5412_sfx_punch"),
			miss = importFixed("5423_sfx_Miss"),
			critical = importFixed("5420_sfx_Slam"),
		),
		openChest = importFixed("5411_sfx_Open1")
	)
}
