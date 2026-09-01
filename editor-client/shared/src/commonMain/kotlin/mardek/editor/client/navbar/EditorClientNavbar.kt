package mardek.editor.client.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mardek.editor.client.util.TriangleShape

@Composable
fun EditorClientNavbar(currentTab: NavbarTab, setCurrentTab: (NavbarTab) -> Unit) {
	val backgroundColor = Color(70, 50, 40)
	val textColor = Color(230, 230, 230)

	Row(modifier = Modifier.fillMaxWidth()) {
		for (category in NavbarCategory.entries) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(category.displayName, fontSize = 1.2.em, modifier = Modifier.clickable(
					indication = null,
					interactionSource = null,
					onClick = { setCurrentTab(NavbarTab.entries.first { it.category == category }) }
				).padding(start = 10.dp, end = 10.dp, bottom = 2.dp, top = 7.dp).pointerHoverIcon(PointerIcon.Hand), textDecoration = TextDecoration.None)

				if (currentTab.category == category) {
					Box(modifier = Modifier.size(15.dp, 10.dp).clip(TriangleShape).background(backgroundColor))
				}
			}
		}
	}

	Row(modifier = Modifier.background(color = backgroundColor).fillMaxWidth()) {
		for (tab in NavbarTab.entries) {
			if (tab.category != currentTab.category) continue

			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(tab.displayName, color = textColor, fontSize = 1.2.em, modifier = Modifier.clickable(
					indication = null,
					interactionSource = null,
					onClick = { setCurrentTab(tab) }
				).padding(start = 10.dp, end = 10.dp, bottom = 2.dp, top = 7.dp).pointerHoverIcon(PointerIcon.Hand))

				if (tab == currentTab) {
					Box(modifier = Modifier.size(15.dp, 10.dp).clip(TriangleShape).background(Color.White))
				}
			}
		}
	}
}
