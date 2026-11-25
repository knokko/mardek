package mardek.editor.client.template

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructList
import mardek.editor.client.component.tracking.ChildStructList
import mardek.editor.client.component.tracking.TrackStruct
import mardek.editor.client.component.tracking.TrackStructList
import mardek.editor.client.component.tracking.TrackStructListElements

abstract class StructListOverview(
	protected val list: BitClientStructList
) {

	protected val columns = mutableListOf<Column>()
	protected val rightButtons = mutableListOf<RightButton>()
	protected val bottomButtons = mutableListOf<BottomButton>()
	private lateinit var lazyListState: LazyListState

	@Composable
	protected open fun initializeState() {}

	@Composable
	fun Compose() {
		initializeState()
		lazyListState = rememberLazyListState()
		val controllerIDs = remember { mutableStateListOf<Long>() }
		var isLoading by remember { mutableStateOf(true) }

		if (isLoading) {
			Text("Loading...")
		} else {
			RenderListOrChildScreen(controllerIDs)
		}

		TrackStructList(list)
		TrackStructListElements(list) {
			controllerIDs.clear()
			controllerIDs.addAll(it.toList())
			isLoading = false
		}
	}

	@Composable
	protected open fun RenderListOrChildScreen(controllerIDs: List<Long>) {
		RenderList(controllerIDs)
	}

	@Composable
	private fun RenderList(controllerIDs: List<Long>) {
		val scrollState = rememberScrollState()

		Column(modifier = Modifier.fillMaxSize()) {
			Row (modifier = Modifier.fillMaxWidth().background(Color.LightGray)) {
				Row(modifier = Modifier.horizontalScroll(scrollState).padding(bottom = 9.dp, top = 6.dp)) {
					for (column in columns) {
						Column(modifier = column.modifier) {
							Text(column.title, fontSize = 1.1.em, modifier = Modifier.padding(start = 5.dp))
						}
					}
					for (button in rightButtons) {
						Column(modifier = button.modifier) {}
					}
				}
			}

			Box(Modifier.weight(1f)) {
				LazyColumn(modifier = Modifier.fillMaxHeight(), state = lazyListState) {
					items(controllerIDs, key = { it }) { controllerID ->
						RenderOuterRow(controllerID, scrollState)
					}
				}
			}

			if (bottomButtons.isNotEmpty()) {
				Row(modifier = Modifier.background(Color.LightGray).fillMaxWidth()) {
					for (button in bottomButtons) button(this)
				}
			}
		}
	}

	@Composable
	private fun RenderOuterRow(controllerID: Long, scrollState: ScrollState) {
		Row(modifier = Modifier.horizontalScroll(scrollState)) {
			RenderInnerRow(list.createConnectionToElement(controllerID, getViewId()))
		}
	}

	@Composable
	private fun RenderInnerRow(element: BitClientStruct) {
		Row {
			for (column in columns) {
				Column(modifier = column.modifier.align(alignment = Alignment.CenterVertically)) {
					column.content(this, element)
				}
			}
			for (button in rightButtons) {
				Column(modifier = button.modifier.align(alignment = Alignment.CenterVertically)) {
					button.content(this, element)
				}
			}
		}

		TrackStruct(element)
	}

	protected abstract fun getViewId(): Int

	class Column(
		val title: String,
		val modifier: Modifier,
		val content: @Composable ColumnScope.(element: BitClientStruct) -> Unit
	)

	class RightButton(
		val modifier: Modifier,
		val content: @Composable ColumnScope.(element: BitClientStruct) -> Unit
	)

	typealias BottomButton = @Composable RowScope.() -> Unit

	companion object {

		@Composable
		fun ComposeFromParent(
			parent: BitClientStruct, fieldName: String, viewID: Int,
			myConstructor: (BitClientStructList) -> StructListOverview
		) {
			val mutateScope = rememberCoroutineScope()
			var elements by remember { mutableStateOf<BitClientStructList?>(null)}

			elements?.let { myConstructor(it).Compose() }

			ChildStructList(mutateScope, parent, null, fieldName, viewID) { elements = it }
		}
	}
}
