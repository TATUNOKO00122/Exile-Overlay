#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FillLevel;
uniform float Time;

in vec2 texCoord0;
in vec4 vertexColor;
in float fillLevel;

out vec4 fragColor;

// 円形マスク計算
float circleMask(vec2 uv, vec2 center, float radius) {
    float dist = length(uv - center);
    float edge = 0.02;
    return 1.0 - smoothstep(radius - edge, radius, dist);
}

// 液面の波アニメーション
float wave(vec2 uv, float time) {
    float wave1 = sin(uv.x * 8.0 + time * 2.0) * 0.02;
    float wave2 = sin(uv.x * 12.0 - time * 1.5) * 0.015;
    return wave1 + wave2;
}

void main() {
    vec2 uv = texCoord0;
    vec2 center = vec2(0.5, 0.5);
    float radius = 0.48;
    
    // 円形マスクを計算
    float mask = circleMask(uv, center, radius);
    
    if (mask < 0.01) {
        discard;
    }
    
    // 液面の高さを計算（波アニメーション込み）
    float liquidLevel = fillLevel;
    if (uv.y > (1.0 - liquidLevel)) {
        // 液面部分
        float waveOffset = wave(uv, Time);
        float surfaceY = 1.0 - liquidLevel + waveOffset;
        
        if (uv.y < surfaceY) {
            // 液体内部
            vec4 color = vertexColor * ColorModulator;
            
            // 深さによるグラデーション
            float depth = (surfaceY - uv.y) * 2.0;
            vec3 darkColor = color.rgb * 0.7;
            vec3 lightColor = color.rgb * 1.2;
            vec3 finalColor = mix(darkColor, lightColor, clamp(depth, 0.0, 1.0));
            
            // 液面のハイライト
            float highlight = smoothstep(0.0, 0.05, surfaceY - uv.y) * 0.3;
            finalColor += vec3(highlight);
            
            fragColor = vec4(finalColor, color.a * mask);
        } else {
            // 液面の表面
            vec4 color = vertexColor * ColorModulator;
            float surfaceIntensity = 1.2;
            fragColor = vec4(color.rgb * surfaceIntensity, color.a * mask * 0.9);
        }
    } else {
        discard;
    }
}
