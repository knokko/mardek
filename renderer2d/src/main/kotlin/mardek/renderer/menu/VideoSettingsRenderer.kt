package mardek.renderer.menu

import mardek.state.util.Rectangle

internal fun renderVideoSettingsTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val settings = context.videoSettings
		val device = settings.availableDevices[settings.preferredDevice]
		textBatch.drawString(
			"Graphics card: ${device.deviceNameString()} ${settings.preferredDevice}"
		)
	}
}
