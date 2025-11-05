package mardek.importer.converter

import mardek.importer.util.projectFolder
import org.apache.batik.transcoder.ErrorHandler
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

fun main() {
	/*
		Before running this code:
		- export all shapes in JPEX using 100% zoom PNG, and put them in project-directory/flash/all-shapes-x1
		- export all shapes in JPEX using 200% zoom SVG, and put them in project-directory/flash/all-shapes-svg2

		After completing the steps above, it is time to run this `main()` method, or alternatively run the command
		`./gradlew convertSVGs` This takes ~30 seconds on my gaming computer. It will probably take longer on (old)
		laptops. Changing numThreads from 20 to something else may help.

		Note that this job will probably spam the following error, which you can ignore:
		```
		org.w3c.dom.DOMException: file:/home/knokko/mardek/flash/all-shapes-svg/303.svg:
The attribute "style" represents an invalid CSS declaration ("image-rendering:optimizeSpeed; image-rendering:pixelated").
Original message:
The "pixelated" identifier is not a valid value for the "image-rendering" property.
	at org.apache.batik.css.engine.CSSEngine.getCascadedStyleMap(CSSEngine.java:824)
	at org.apache.batik.css.engine.CSSEngine.getComputedStyle(CSSEngine.java:867)
	at org.apache.batik.bridge.CSSUtilities.getComputedStyle(CSSUtilities.java:81)
	at org.apache.batik.bridge.CSSUtilities.convertDisplay(CSSUtilities.java:563)
	at org.apache.batik.bridge.AbstractGraphicsNodeBridge.getDisplay(AbstractGraphicsNodeBridge.java:158)
	at org.apache.batik.bridge.GVTBuilder.build(GVTBuilder.java:134)
	at org.apache.batik.bridge.SVGPatternElementBridge.extractLocalPatternContent(SVGPatternElementBridge.java:291)
	at org.apache.batik.bridge.SVGPatternElementBridge.extractPatternContent(SVGPatternElementBridge.java:246)
	at org.apache.batik.bridge.SVGPatternElementBridge.createPaint(SVGPatternElementBridge.java:85)
	at org.apache.batik.bridge.PaintServer.convertURIPaint(PaintServer.java:373)
	at org.apache.batik.bridge.PaintServer.convertPaint(PaintServer.java:273)
	at org.apache.batik.bridge.PaintServer.convertFillPaint(PaintServer.java:242)
	at org.apache.batik.bridge.PaintServer.convertFillAndStroke(PaintServer.java:160)
	at org.apache.batik.bridge.SVGShapeElementBridge.createShapePainter(SVGShapeElementBridge.java:117)
	at org.apache.batik.bridge.SVGDecoratedShapeElementBridge.createFillStrokePainter(SVGDecoratedShapeElementBridge.java:58)
	at org.apache.batik.bridge.SVGDecoratedShapeElementBridge.createShapePainter(SVGDecoratedShapeElementBridge.java:84)
	at org.apache.batik.bridge.SVGShapeElementBridge.buildGraphicsNode(SVGShapeElementBridge.java:91)
	at org.apache.batik.bridge.GVTBuilder.buildGraphicsNode(GVTBuilder.java:224)
	at org.apache.batik.bridge.GVTBuilder.buildComposite(GVTBuilder.java:171)
	at org.apache.batik.bridge.GVTBuilder.buildGraphicsNode(GVTBuilder.java:219)
	at org.apache.batik.bridge.GVTBuilder.buildComposite(GVTBuilder.java:171)
	at org.apache.batik.bridge.GVTBuilder.build(GVTBuilder.java:82)
	at org.apache.batik.transcoder.SVGAbstractTranscoder.transcode(SVGAbstractTranscoder.java:210)
	at org.apache.batik.transcoder.image.ImageTranscoder.transcode(ImageTranscoder.java:92)
	at org.apache.batik.transcoder.XMLAbstractTranscoder.transcode(XMLAbstractTranscoder.java:142)
	at org.apache.batik.transcoder.SVGAbstractTranscoder.transcode(SVGAbstractTranscoder.java:158)
	at mardek.importer.converter.SvgShapeConverterKt.main$lambda$0(SvgShapeConverter.kt:53)
	at java.base/java.lang.Thread.run(Thread.java:1583)
	```
	 */
	val pngInputFolder = File("$projectFolder/flash/all-shapes-x1")
	val svgInputFolder = File("$projectFolder/flash/all-shapes-svg2")
	val pngOutputFolder = File("$projectFolder/flash/all-shapes-x4")

	if (!pngInputFolder.isDirectory || !svgInputFolder.isDirectory) {
		throw Error("Please follow above instructions")
	}
	pngOutputFolder.mkdirs()

	val counter = AtomicInteger(0)
	val pngInputFiles = pngInputFolder.listFiles()!!
	val numThreads = 20

	val threads = Array(numThreads) { threadIndex -> Thread {
		for ((index, pngInputFile) in pngInputFiles.withIndex()) {
			if (index % numThreads != threadIndex) continue
			if (pngInputFile.extension != "png") {
				println("Skipping $pngInputFile")
				continue
			}

			val inputImage = ImageIO.read(pngInputFile)
			val svgFile = File("$svgInputFolder/${pngInputFile.nameWithoutExtension}.svg")

			val pngTranscoder = PNGTranscoder()
			pngTranscoder.errorHandler = SilentErrorHandler(pngInputFile.name)
			pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 4f * inputImage.width)
			pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 4f * inputImage.height)

			val transcoderInput = TranscoderInput(svgFile.absolutePath)
			val output = File("$pngOutputFolder/${pngInputFile.name}")

			val outputStream = Files.newOutputStream(output.toPath())
			val transcoderOutput = TranscoderOutput(outputStream)

			pngTranscoder.transcode(transcoderInput, transcoderOutput)

			outputStream.flush()
			outputStream.close()

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

private class SilentErrorHandler(private val fileName: String) : ErrorHandler {

	override fun error(exception: TranscoderException) {
		println("A non-fatal error occurred while processing $fileName")
	}

	override fun fatalError(failure: TranscoderException) {
		System.err.println("A fatal error occurred while processing $fileName")
	}

	override fun warning(warning: TranscoderException) {
		println("A warning occurred while processing $fileName")
	}
}