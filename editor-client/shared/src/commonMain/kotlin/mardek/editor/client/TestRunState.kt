package mardek.editor.client

import mardek.exporting.ContentWrapper

class TestRunState {

	var content: ContentWrapper? = null
		private set
	private var stateCallback: ((Int) -> Unit)? = null
	private var state = 0

	fun requestStart(content: ContentWrapper, stateCallback: (Int) -> Unit) {
		synchronized(this) {
			this.content = content
			this.stateCallback = stateCallback
			this.state = 1
			stateCallback(state)
		}
	}

	fun reportProgress() {
		synchronized(this) {
			this.state += 1
		}
		stateCallback!!.invoke(state)
	}

	fun reset() {
		stateCallback!!(0)
		synchronized(this) {
			this.content = null
			this.stateCallback = null
			this.state = 0
		}
	}
}
