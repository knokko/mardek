package mardek.editor.client.inventory

import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.component.tracking.TrackStruct
import mardek.editor.client.navbar.NavbarTab
import mardek.editor.client.template.StructListOverview

@Composable
fun ItemsContentTabs(itemsContent: BitClientStruct, currentTab: NavbarTab) {
	if (currentTab == NavbarTab.ItemTypes) StructListOverview.ComposeFromParent(
		itemsContent, "itemTypes", 1, ::ItemTypeOverview
	)
	if (currentTab == NavbarTab.Items) {
		StructListOverview.ComposeFromParent(itemsContent, "items", 1) {
			ItemOverview(it, itemsContent)
		}
	}

	TrackStruct(itemsContent)
}
