#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in int scale;
layout(location = 2) in int spriteOffset;
layout(location = 3) in float opacity;

layout(set = 0, binding = 0) readonly buffer MapBuffer {
	uint sprites[];
};

#include "kim1.glsl"

layout(push_constant) uniform PushConstants {
	ivec2 screenSize;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out int propagateSpriteOffset;
layout(location = 2) out float propagateOpacity;

vec2 textureCoordinateMapping[] = {
		vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
		vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	//uvec2 spriteSize = getKimImageSize(sprites[spriteOffset]);
	uvec2 spriteSize = uvec2(16, 16); // TODO Stop hardcoding this
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];

	ivec2 extraOffset = ivec2(0, 0);
	if (spriteSize.x >= 32) extraOffset.x -= 16 * scale;
	if (spriteSize.y >= 32) extraOffset.y -= 16 * scale;
	vec2 relative = vec2(offset + extraOffset + scale * spriteSize * textureCoordinates) / vec2(screenSize);
	gl_Position = vec4(2.0 * relative - vec2(1.0), 0.0, 1.0);
	propagateSpriteOffset = spriteOffset;
	propagateOpacity = opacity;
}
