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
	SelectedGradient selected = initialSelectedGradient(glyph.fillColor, glyph.color0, glyph.distance0);
	selected = nextSelectedGradient(relativeY, selected, glyph.color0, glyph.color1, glyph.distance0, glyph.distance1);
	selected = nextSelectedGradient(relativeY, selected, glyph.color1, glyph.color2, glyph.distance1, glyph.distance2);
	selected = nextSelectedGradient(relativeY, selected, glyph.color2, glyph.color3, glyph.distance2, glyph.distance3);
	vec4 fillColor = selectGradientColor(relativeY, selected);

	WaveIntersection intersection = closestIntersection(glyph);
	vec4 mainColor = determineMainColor(
		intersection.inside, intersection.distance, fillColor, decodeColor(glyph.backgroundColor)
	);
	float strokeIntensity = determineStrokeIntensity(intersection.distance, glyph.strokeWidth);
	if (intersection.inside) strokeIntensity = 0.0;

	vec4 strokeColor;
	if (strokeIntensity > 0.0) {
		selected = initialSelectedGradient(glyph.strokeColor, glyph.borderColor0, glyph.borderDistance0);
		selected = nextSelectedGradient(intersection.distance, selected, glyph.borderColor0, glyph.borderColor1, glyph.borderDistance0, glyph.borderDistance1);
		strokeColor = selectGradientColor(intersection.distance, selected);
	} else strokeColor = decodeColor(glyph.strokeColor);

	outColor = mixStrokeColor(mainColor, strokeColor, strokeIntensity);
}
