#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in float scale;
layout(location = 2) in int spriteOffset;
layout(location = 3) in float opacity;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(set = 0, binding = 0) readonly buffer ImageBuffer {
	uint kimBuffer[];
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out int propagateSpriteOffset;
layout(location = 2) out float propagateOpacity;

#include "kim2.glsl"

vec2 textureCoordinateMapping[] = {
		vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
		vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];
	uvec2 spriteSize = getKim2ImageSize(kimBuffer[spriteOffset]);
	vec2 relative = vec2(offset + scale * spriteSize * textureCoordinates) / vec2(viewportSize);
	gl_Position = vec4(2.0 * relative - vec2(1.0), 0.0, 1.0);
	propagateSpriteOffset = spriteOffset;
	propagateOpacity = opacity;
}
