package mardek.editor.client.file

import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.TestRunState
import mardek.editor.client.navbar.NavbarTab

@Composable
fun FileTabs(wrappedContent: BitClientStruct, currentTab: NavbarTab, testRunState: TestRunState) {
	if (currentTab == NavbarTab.Exporting) ExportingMenu(wrappedContent, testRunState)
}
