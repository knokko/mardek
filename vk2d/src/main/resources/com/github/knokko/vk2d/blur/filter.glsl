// filterSize == 0: weights are [1.0] and totalWeight = 1.0
// filterSize == 1: weights are [0.5, 1.0, 0.5] and totalWeight = 2.0
// filterSize == 2: weights are [0.33, 0.67, 1.0, 0.67, 0.33] and totalWeight = 3.0

#define DEFAULT_UNROLL 3
#define MAX_FILTER_SIZE 19

#define initializeFilterWindow(base, filterSize, unroll) uint colors[2 * MAX_FILTER_SIZE + 1 + unroll];\
	for (int offset = filterSize; offset > 0; offset--) colors[filterSize - offset] = fetchColor(max(0, base - offset));\
	colors[filterSize] = fetchColor(base);\
	for (uint offset = 1; offset <= filterSize; offset++) colors[filterSize + offset] = fetchColor(min(getInputSize() - 1, base + offset));

#define applySingleFilter0(offset) decodeColor(colors[0 + offset])

#define applySingleFilter1(offset) 0.5 * (0.5 * decodeColor(colors[0 + offset]) + 1.0 * decodeColor(colors[1 + offset]) + 0.5 * decodeColor(colors[2 + offset]))

#define applySingleFilter3(offset) 0.25 * (\
			0.25 * decodeColor(colors[0 + offset]) + 0.5 * decodeColor(colors[1 + offset]) + 0.75 * decodeColor(colors[2 + offset]) +\
			1.0 * decodeColor(colors[3 + offset]) + 0.75 * decodeColor(colors[4 + offset]) + 0.5 * decodeColor(colors[5 + offset]) +\
			0.25 * decodeColor(colors[6 + offset])\
	)

#define applySingleFilter4(offset) 0.2 * (\
			0.2 * decodeColor(colors[0 + offset]) + 0.4 * decodeColor(colors[1 + offset]) + 0.6 * decodeColor(colors[2 + offset]) +\
			0.8 * decodeColor(colors[3 + offset]) + 1.0 * decodeColor(colors[4 + offset]) + 0.8 * decodeColor(colors[5 + offset]) +\
			0.6 * decodeColor(colors[6 + offset]) + 0.4 * decodeColor(colors[7 + offset]) + 0.2 * decodeColor(colors[8 + offset])\
	)

#define applySingleFilter9(offset) 0.1 * (\
			0.1 * decodeColor(colors[0 + offset]) + 0.2 * decodeColor(colors[1 + offset]) + 0.3 * decodeColor(colors[2 + offset]) +\
			0.4 * decodeColor(colors[3 + offset]) + 0.5 * decodeColor(colors[4 + offset]) + 0.6 * decodeColor(colors[5 + offset]) +\
			0.7 * decodeColor(colors[6 + offset]) + 0.8 * decodeColor(colors[7 + offset]) + 0.9 * decodeColor(colors[8 + offset]) +\
			1.0 * decodeColor(colors[9 + offset]) + 0.9 * decodeColor(colors[10 + offset]) + 0.8 * decodeColor(colors[11 + offset]) +\
			0.7 * decodeColor(colors[12 + offset]) + 0.6 * decodeColor(colors[13 + offset]) + 0.5 * decodeColor(colors[14 + offset]) +\
			0.4 * decodeColor(colors[15 + offset]) + 0.3 * decodeColor(colors[16 + offset]) + 0.2 * decodeColor(colors[17 + offset]) +\
			0.1 * decodeColor(colors[18 + offset])\
	)

#define applySingleFilter19(offset) 0.05 * (\
			0.05 * decodeColor(colors[0 + offset]) + 0.1 * decodeColor(colors[1 + offset]) + 0.15 * decodeColor(colors[2 + offset]) +\
			0.2 * decodeColor(colors[3 + offset]) + 0.25 * decodeColor(colors[4 + offset]) + 0.3 * decodeColor(colors[5 + offset]) +\
			0.35 * decodeColor(colors[6 + offset]) + 0.4 * decodeColor(colors[7 + offset]) + 0.45 * decodeColor(colors[8 + offset]) +\
			0.5 * decodeColor(colors[9 + offset]) + 0.55 * decodeColor(colors[10 + offset]) + 0.6 * decodeColor(colors[11 + offset]) +\
			0.65 * decodeColor(colors[12 + offset]) + 0.7 * decodeColor(colors[13 + offset]) + 0.75 * decodeColor(colors[14 + offset]) +\
			0.8 * decodeColor(colors[15 + offset]) + 0.85 * decodeColor(colors[16 + offset]) + 0.9 * decodeColor(colors[17 + offset]) +\
			0.95 * decodeColor(colors[18 + offset]) + 1.0 * decodeColor(colors[19 + offset]) + 0.95 * decodeColor(colors[20 + offset]) +\
			0.9 * decodeColor(colors[21 + offset]) + 0.85 * decodeColor(colors[22 + offset]) + 0.8 * decodeColor(colors[23 + offset]) +\
			0.75 * decodeColor(colors[24 + offset]) + 0.7 * decodeColor(colors[25 + offset]) + 0.65 * decodeColor(colors[26 + offset]) +\
			0.6 * decodeColor(colors[27 + offset]) + 0.55 * decodeColor(colors[28 + offset]) + 0.5 * decodeColor(colors[29 + offset]) +\
			0.45 * decodeColor(colors[30 + offset]) + 0.4 * decodeColor(colors[31 + offset]) + 0.35 * decodeColor(colors[32 + offset]) +\
			0.3 * decodeColor(colors[33 + offset]) + 0.25 * decodeColor(colors[34 + offset]) + 0.2 * decodeColor(colors[35 + offset]) +\
			0.15 * decodeColor(colors[36 + offset]) + 0.1 * decodeColor(colors[37 + offset]) + 0.05 * decodeColor(colors[38 + offset])\
	)

#define applyFilter(v, stride, filterSize, applySingleFilter, unroll) for (uint unrollIndex = 1; unrollIndex <= unroll; unrollIndex++) {\
		colors[2 * filterSize + unrollIndex] = fetchColor(min(getInputSize() - 1, v + filterSize + unrollIndex));\
	}\
	for (uint unrollIndex = 0; unrollIndex < unroll && v + unrollIndex < limit; unrollIndex++) {\
		vec4 textureColor = applySingleFilter(unrollIndex);\
		outputBuffer[y * inputSize.x + x + stride * unrollIndex] = encodeColor(textureColor);\
	}\
	for (uint index = 0; index <= 2 * filterSize; index++) colors[index] = colors[index + unroll]
