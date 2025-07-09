package mardek.renderer.util

import java.lang.StringBuilder

fun renderDescription(description: String, lineWidth: Int, drawLine: (String) -> Unit) {
	val paragraphs = description.split("\\n")
	for (paragraph in paragraphs) {
		val splitDescription = paragraph.split(" ")
		val currentLine = StringBuilder(lineWidth)

		for (word in splitDescription) {
			if (currentLine.isNotEmpty() && currentLine.length + word.length >= lineWidth) {
				drawLine(currentLine.toString())
				currentLine.clear()
			}
			currentLine.append(word).append(' ')
		}
		if (currentLine.isNotEmpty()) drawLine(currentLine.toString())
	}
}
