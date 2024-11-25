#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in int spriteOffset;
layout(location = 2) in int opacity;

#include "base.glsl"
#include "kim1.glsl"

layout(push_constant) uniform PushConstants {
	ivec2 screenSize;
	ivec2 cameraOffset;
	int scale;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out int propagateSpriteOffset;
layout(location = 2) out float propagateOpacity;

vec2 textureCoordinateMapping[] = {
		vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
		vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	uvec2 spriteSize = getKimImageSize(mapAndSprites[spriteOffset]);
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];

	ivec2 extraOffset = ivec2(0, 0);
	if (spriteSize.y >= 32) extraOffset.y -= 16 * scale;
	vec2 relative = 2.0 * vec2(offset + extraOffset - cameraOffset) / vec2(screenSize);
	gl_Position = vec4(relative + 2.0 * scale * spriteSize * textureCoordinates / vec2(screenSize), 0.0, 1.0);
	propagateSpriteOffset = spriteOffset;
	propagateOpacity = opacity / 255.0;
}
