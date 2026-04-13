#version 150

uniform float FillPercent;
uniform int HalfMode;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 uv = texCoord0;
    
    float dist = length(uv - center);
    float radius = 0.48;
    
    if (dist > radius) {
        discard;
    }
    
    if (HalfMode == 1) {
        if (uv.x > 0.5) {
            discard;
        }
    } else if (HalfMode == 2) {
        if (uv.x < 0.5) {
            discard;
        }
    }
    
    float liquidLevel = 1.0 - FillPercent;
    
    if (uv.y < liquidLevel) {
        discard;
    }
    
    float edge = 0.02;
    float alpha = 1.0 - smoothstep(radius - edge, radius, dist);
    
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
