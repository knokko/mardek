package mardek.editor.client

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.inventory.ItemType

val itemTypes = mutableListOf(
	ItemType("WEAPON: SWORD", rgb(200, 0, 0), "Sword"),
	ItemType("WEAPON: GREATAXE", rgb(200, 0, 0), "Greataxe"),
	ItemType("HELMET: FULL", rgb(0, 0, 200), "Full Helmet"),
)

@Composable
fun ItemTypeOverview() {
	Row {
		Text("Raw name")
		Text("Grid color")
		Text("Nice name")
	}
	for (itemType in itemTypes) {
		Row {
			Text(itemType.displayName)
			Text(itemType.gridColor.toString())
			Text(itemType.niceName)
		}
	}
}
