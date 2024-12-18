#version 450

layout(set = 0, binding = 1) readonly buffer MiddleBuffer {
	uint sprites[];
};

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat int textureOffset;
layout(location = 2) in float opacity;

layout(location = 0) out vec4 outColor;

void main() {
    uvec2 intCoords = uvec2(textureCoordinates * 15 + vec2(0.5));
	uint color = sprites[16 * 16 * textureOffset + 16 * intCoords.y + intCoords.x];
	uint ured = color & 255u;
    uint ugreen = (color >> 8) & 255u;
    uint ublue = (color >> 16) & 255u;
    uint ualpha = (color >> 24) & 255u;
	vec4 baseColor = vec4(ured / 255.0, ugreen / 255.0, ublue / 255.0, ualpha / 255.0);
	outColor = vec4(baseColor.rgb, opacity * baseColor.a);
}
