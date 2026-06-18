#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float PixelSize;

out vec4 fragColor;

void main() {
    vec2 mosaicInSize = InSize / PixelSize;
    vec2 fractPix = fract(texCoord * mosaicInSize) / mosaicInSize;

    vec4 baseTexel = texture(DiffuseSampler, texCoord - fractPix);
    baseTexel.a = 1.0;
    fragColor = baseTexel;
}
