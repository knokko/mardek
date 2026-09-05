package mardek.editor.client

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.BITSER
import mardek.content.inventory.ItemType
import java.lang.Thread.sleep

val itemTypes = mutableStateListOf(
	ItemType("WEAPON: SWORD", rgb(200, 0, 0), "Sword"),
	ItemType("WEAPON: GREATAXE", rgb(200, 0, 0), "Greataxe"),
	ItemType("HELMET: FULL", rgb(0, 0, 200), "Full Helmet"),
)

val test = Thread {
	sleep(3000)
	repeat(100) {
		if (Math.random() < 0.5) sleep(it.toLong())
		itemTypes.add(ItemType("TEST", rgb(200, 200, 0), "Test"))
	}
}.start()

val fontSize = 1.1.em

val baseModifiers = arrayOf(300.dp, 300.dp, 300.dp).map { Modifier.width(it) }

@Composable
fun ItemTypeOverview() {
	val scrollState = rememberScrollState()
	Column(modifier = Modifier.fillMaxSize()) {
		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Text("Upper name", modifier = baseModifiers[0], fontSize = fontSize)
			Text("Grid color", modifier = baseModifiers[2], fontSize = fontSize)
			Text("Nice name", modifier = baseModifiers[1], fontSize = fontSize)
		}
		LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
			items(itemTypes) { itemType ->
				Row(modifier = Modifier.horizontalScroll(scrollState).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
					Box(modifier = baseModifiers[0]) {
						TextField(
							state = rememberTextFieldState(initialText = itemType.displayName),
							modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp),
							textStyle = TextStyle(fontSize = fontSize),
							contentPadding = PaddingValues(5.dp),
							lineLimits = TextFieldLineLimits.SingleLine,
							inputTransformation = {
								val newText = this.toString()
								println("new text is $newText")
							}
						)
					}

					Text(itemType.gridColor.toString(), modifier = baseModifiers[1], fontSize = fontSize)
					Text(itemType.niceName, modifier = baseModifiers[2], fontSize = fontSize)
				}
			}
		}
	}
}
