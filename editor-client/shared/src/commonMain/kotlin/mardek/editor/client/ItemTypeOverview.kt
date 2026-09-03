package mardek.editor.client

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.inventory.ItemType

val itemTypes = mutableListOf(
	ItemType("WEAPON: SWORD", rgb(200, 0, 0), "Sword"),
	ItemType("WEAPON: GREATAXE", rgb(200, 0, 0), "Greataxe"),
	ItemType("HELMET: FULL", rgb(0, 0, 200), "Full Helmet"),
)

val baseModifiers = arrayOf(300.dp, 300.dp, 300.dp).map { Modifier.width(it) }

@Composable
fun ItemTypeOverview() {
	val scrollState = rememberScrollState()
	Column(modifier = Modifier.fillMaxSize()) {
		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Text("Upper name", modifier = baseModifiers[0])
			Text("Nice name", modifier = baseModifiers[1])
			Text("Grid color", modifier = baseModifiers[2])
		}
		LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
			items(itemTypes) { itemType ->
				Row(modifier = Modifier.horizontalScroll(scrollState)) {
					Text(itemType.displayName, modifier = baseModifiers[0])
					Text(itemType.gridColor.toString(), modifier = baseModifiers[1])
					Text(itemType.niceName, modifier = baseModifiers[2])
				}
			}
		}
	}
}
