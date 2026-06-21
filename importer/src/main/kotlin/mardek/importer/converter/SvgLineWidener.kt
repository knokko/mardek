package mardek.importer.converter

import mardek.importer.util.projectFolder
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.anim.dom.SVGOMPathElement
import org.apache.batik.anim.dom.SVGOMSVGElement
import org.apache.batik.svggen.SVGGraphics2D
import org.apache.batik.util.XMLResourceDescriptor
import java.io.File
import java.io.OutputStreamWriter
import java.lang.Double.parseDouble
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

fun main() {
	val parserClassName = XMLResourceDescriptor.getXMLParserClassName()

	val folder = File("$projectFolder/flash/all-shapes-svg2/")
	val inputFiles = folder.listFiles()!!.filter { it.extension == "svg" && !it.name.contains("x") }
	val numThreads = 20

	val counter = AtomicInteger()
	val threads = Array(numThreads) { threadIndex -> Thread {

		val factory = SAXSVGDocumentFactory(parserClassName)
		for ((fileIndex, inputFile) in inputFiles.withIndex()) {
			if (fileIndex % numThreads != threadIndex) continue

			for (widthFactor in arrayOf(0.5, 0.75, 0.9, 1.25, 1.5, 2.0, 2.5, 3.0)) {
				val document = factory.createDocument(inputFile.path)
				val firstElement = document.firstChild
				if (firstElement is SVGOMSVGElement) {
					val marginForScale1 = 1
					val margin = 2 * marginForScale1
					val oldWidth = firstElement.width.baseVal.value
					val oldHeight = firstElement.height.baseVal.value
					val newWidth = oldWidth + 2 * margin
					val newHeight = oldHeight + 2 * margin
					firstElement.setAttribute("viewBox", "-$margin -$margin $newWidth $newHeight")
					firstElement.setAttribute("width", newWidth.toString())
					firstElement.setAttribute("height", newHeight.toString())
				}

				val elements = document.getElementsByTagNameNS("*", "path")
				for (index in 0 until elements.length) {
					val element = elements.item(index)
					if (element !is SVGOMPathElement) continue

					if (element.hasAttribute("stroke-width")) {
						val oldStrokeWidth = parseDouble(element.getAttribute("stroke-width"))
						if (oldStrokeWidth < widthFactor) {
							val newStrokeWidth = widthFactor
							element.setAttribute("stroke-width", newStrokeWidth.toString())
						}
					}
				}

				val svgOutput = SVGGraphics2D(document)

				val outputFile = File("$folder/${inputFile.nameWithoutExtension}x$widthFactor.svg")
				val outputWriter = OutputStreamWriter(Files.newOutputStream(outputFile.toPath()))
				svgOutput.stream(document.documentElement, outputWriter)
				outputWriter.close()
			}

			val nextCounter = counter.incrementAndGet()
			if (nextCounter % 100 == 0) {
				println("Converted $nextCounter files already")
			}
		}
	}}

	for (thread in threads) thread.start()
	for (thread in threads) thread.join()

	println("Converted $counter files in total")
}
