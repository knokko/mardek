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

@Composable
fun ItemsContentTabs(itemsContent: BitClientStruct, currentTab: NavbarTab) {
	if (currentTab == NavbarTab.ItemTypes) OuterItemTypeOverview(itemsContent)
	if (currentTab == NavbarTab.Items) OuterItemOverview(itemsContent)

	TrackStruct(itemsContent)
}

@Composable
private fun OuterItemTypeOverview(itemsContent: BitClientStruct) {
	val mutateScope = rememberCoroutineScope()
	var itemTypes by remember { mutableStateOf<BitClientStructList?>(null)}

	itemTypes?.let { ItemTypeOverview(it) }

	ChildStructList(mutateScope, itemsContent, null, "itemTypes", 0) { itemTypes = it }
}

@Composable
private fun OuterItemOverview(itemsContent: BitClientStruct) {
	val mutateScope = rememberCoroutineScope()
	var items by remember { mutableStateOf<BitClientStructList?>(null)}

	items?.let { ItemOverview(it, itemsContent) }

	ChildStructList(mutateScope, itemsContent, null, "items", 0) { items = it }
}
