package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import mardek.editor.client.navbar.EditorClientNavbar
import mardek.editor.client.navbar.NavbarTab

@Composable
@Preview
fun App() {
    var currentTab by remember { mutableStateOf(NavbarTab.Settings) }
    Column {
        EditorClientNavbar(currentTab) { currentTab = it }
        if (currentTab == NavbarTab.ItemTypes) ItemTypeOverview()
    }
}
