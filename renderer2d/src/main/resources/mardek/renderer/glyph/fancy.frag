#version 450

#include "info.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in float relativeY;
layout(location = 2) in flat GlyphInfo glyph;

layout(location = 0) out vec4 outColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/linear-gradients.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/intersection.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/color.glsl"

void main() {
	SelectedGradient selected = initialSelectedGradient(glyph.colorsAndSize.z, glyph.fillColors.x, glyph.fillDistances.x);
	selected = nextSelectedGradient(relativeY, selected, glyph.fillColors.x, glyph.fillColors.y, glyph.fillDistances.x, glyph.fillDistances.y);
	selected = nextSelectedGradient(relativeY, selected, glyph.fillColors.y, glyph.fillColors.z, glyph.fillDistances.y, glyph.fillDistances.z);
	selected = nextSelectedGradient(relativeY, selected, glyph.fillColors.z, glyph.fillColors.w, glyph.fillDistances.z, glyph.fillDistances.w);
	// TODO Code reuse for nextSelectedGradient with vectors
	vec4 fillColor = selectGradientColor(relativeY, selected);

	WaveIntersection intersection = closestIntersection(glyph);
	vec4 mainColor = determineMainColor(intersection.inside, intersection.distance, fillColor);
	float strokeIntensity = determineStrokeIntensity(intersection.distance, glyph.yInfoAndStrokeWidth.x);
	if (intersection.inside) strokeIntensity = 0.0;

	vec4 strokeColor;
	if (strokeIntensity > 0.0) {
		selected = initialSelectedGradient(glyph.colorsAndSize.w, glyph.borderColors.x, glyph.borderDistances.x);
		selected = nextSelectedGradient(intersection.distance, selected, glyph.borderColors.x, glyph.borderColors.y, glyph.borderDistances.x, glyph.borderDistances.y);
		strokeColor = selectGradientColor(intersection.distance, selected);
	} else strokeColor = decodeColor(glyph.colorsAndSize.w);

	outColor = mixStrokeColor(mainColor, strokeColor, strokeIntensity);
}
