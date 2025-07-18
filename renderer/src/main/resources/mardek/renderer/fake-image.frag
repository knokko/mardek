#version 450

layout(set = 0, binding = 0) readonly buffer MiddleBuffer {
	uint sprites[];
};

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uvec2 spriteSize;
layout(location = 2) in flat int textureOffset;
layout(location = 3) in float opacity;
layout(location = 4) in flat int blinkColor;
layout(location = 5) in float blinkIntensity;

layout(location = 0) out vec4 outColor;

void main() {
    uvec2 intCoords = uvec2(textureCoordinates * spriteSize);
	uint color = sprites[textureOffset + spriteSize.x * intCoords.y + intCoords.x];
	uint ured = color & 255u;
    uint ugreen = (color >> 8) & 255u;
    uint ublue = (color >> 16) & 255u;
    uint ualpha = (color >> 24) & 255u;
	vec4 baseColor = vec4(ured / 255.0, ugreen / 255.0, ublue / 255.0, ualpha / 255.0);
	if (blinkIntensity > 0.0) {
		ured = blinkColor & 255u;
		ugreen = (blinkColor >> 8) & 255u;
		ublue = (blinkColor >> 16) & 255u;
		vec3 blink = vec3(ured / 255.0, ugreen / 255.0, ublue / 255.0);
		baseColor = vec4(blinkIntensity * blink + (1.0 - blinkIntensity) * baseColor.rgb, baseColor.a);
	}
	outColor = vec4(baseColor.rgb, opacity * baseColor.a);
}
