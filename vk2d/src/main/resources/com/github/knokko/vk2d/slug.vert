/*
 * Copyright (C) 2026  Behdad Esfahbod
 * Copyright (C) 2017  Eric Lengyel
 *
 * Based on the Slug algorithm by Eric Lengyel:
 * https://github.com/EricLengyel/Slug
 *
 *  This is part of HarfBuzz, a text shaping library.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE COPYRIGHT HOLDER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE COPYRIGHT HOLDER HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

#version 450

/* Dilate a glyph vertex by half a pixel on screen.
 *
 * position:  object-space vertex position (modified in place)
 * texcoord:  em-space sample coordinates (modified in place)
 * normal:    object-space outward normal at this vertex
 * jac:       inverse of the 2x2 linear part of the em-to-object transform,
 *            stored row-major as (j00, j01, j10, j11).  Maps object-space
 *            displacements back to em-space for texcoord adjustment.
 *            For simple scaling with y-flip (the common case):
 *              em-to-object = [[s, 0], [0, -s]]
 *              jac = (1/s, 0, 0, -1/s)
 * m:         model-view-projection matrix
 * viewport:  viewport size in pixels
 */
void hb_gpu_dilate (inout vec2 position, inout vec2 texcoord,
		    vec2 normal, vec4 jac,
		    mat4 m, vec2 viewport)
{
  vec2 n = normalize (normal);

  vec4 clipPos = m * vec4 (position, 0.0, 1.0);
  vec4 clipN   = m * vec4 (n, 0.0, 0.0);

  float s = clipPos.w;
  float t = clipN.w;

  float u = (s * clipN.x - t * clipPos.x) * viewport.x;
  float v = (s * clipN.y - t * clipPos.y) * viewport.y;

  float s2 = s * s;
  float st = s * t;
  float uv = u * u + v * v;

  float denom = uv - st * st;
  float d = abs (denom) > 1.0 / 16777216.0
	  ? s2 * (st + sqrt (uv)) / denom
	  : 0.0;

  vec2 dPos = d * normal;
  position += dPos;
  texcoord += vec2 (dot (dPos, jac.xy), dot (dPos, jac.zw));
}

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec2 inTextureCoordinates;
layout(location = 2) in vec2 inNormal;
layout(location = 3) in float inEmPerPos;
layout(location = 4) in uint inGlyphLoc;

layout(push_constant) uniform PushConstants {
	mat4 viewProjectionMatrix;
	vec2 viewportSize;
} pushConstants;

layout(location = 0) out vec2 outTextureCoordinates;
layout(location = 1) out uint outGlyphLoc;

void main() {
	vec2 pos = inPosition;
	vec2 tex = inTextureCoordinates;
	vec4 jac = vec4(inEmPerPos, 0.0, 0.0, -inEmPerPos);
	hb_gpu_dilate(pos, tex, inNormal, jac, pushConstants.viewProjectionMatrix, pushConstants.viewportSize);
	gl_Position = pushConstants.viewProjectionMatrix * vec4(pos, 0.0, 1.0);
	outTextureCoordinates = tex;
	outGlyphLoc = inGlyphLoc;
}
