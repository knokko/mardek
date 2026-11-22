package mardek.editor.client.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructList
import mardek.editor.client.component.ChildStructList
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.navbar.NavbarTab
import mardek.editor.client.template.StructListOverview

@Composable
fun ItemsContentTabs(itemsContent: BitClientStruct, currentTab: NavbarTab) {
	if (currentTab == NavbarTab.ItemTypes) StructListOverview.ComposeFromParent(
		itemsContent, "itemTypes", 1, ::ItemTypeOverview
	)
	if (currentTab == NavbarTab.Items) OuterItemOverview(itemsContent)

	TrackStruct(itemsContent)
}

@Composable
private fun OuterItemOverview(itemsContent: BitClientStruct) {
	val mutateScope = rememberCoroutineScope()
	var items by remember { mutableStateOf<BitClientStructList?>(null)}

	items?.let { ItemOverview(it, itemsContent) }

	ChildStructList(mutateScope, itemsContent, null, "items", 1) { items = it }
}
