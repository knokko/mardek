package mardek.importer.area

fun parseAreaMap(rawMap: String): Triple<Int, Int, IntArray> {
	parseAssert(rawMap.startsWith("[["), "Expected $rawMap to start with [[")
	parseAssert(rawMap.endsWith("]]"), "Expected $rawMap to end with ]]")

	val rows = rawMap.substring(2, rawMap.length - 2).split("],[")
	val columns = rows.map { row -> row.split(",").map { Integer.parseInt(it) } }
	val height = rows.size
	val width = columns[0].size
	for (colum in columns) {
		if (colum.size != width) throw IllegalArgumentException("Not all columns have the same size")
	}
	val tileIDs = IntArray(width * height) { index -> columns[index / width][index % width] }

	return Triple(width, height, tileIDs)
}
