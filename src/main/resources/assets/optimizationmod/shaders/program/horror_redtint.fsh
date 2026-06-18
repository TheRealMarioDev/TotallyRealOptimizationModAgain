#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform vec3 RedMatrix;
uniform vec3 GreenMatrix;
uniform vec3 BlueMatrix;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    float red   = dot(color.rgb, RedMatrix);
    float green = dot(color.rgb, GreenMatrix);
    float blue  = dot(color.rgb, BlueMatrix);
    vec3 tinted = vec3(red, green, blue);

    tinted = clamp((tinted - 0.5) * 1.4 + 0.5, 0.0, 1.0);
    fragColor = vec4(tinted, 1.0);
}
