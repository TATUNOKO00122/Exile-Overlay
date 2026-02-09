#version 150

uniform float FillPercent;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 uv = texCoord0;
    
    // 円形の距離を計算
    float dist = length(uv - center);
    float radius = 0.48;
    
    // 円の外側は discard
    if (dist > radius) {
        discard;
    }
    
    // 液面の高さを計算（下から上へ）
    // V座標は0が上、1が下なので、1 - FillPercentが液面の位置
    float liquidLevel = 1.0 - FillPercent;
    
    // 液面より上は描画しない
    if (uv.y < liquidLevel) {
        discard;
    }
    
    // 縁を滑らかにするアンチエイリアス
    float edge = 0.02;
    float alpha = 1.0 - smoothstep(radius - edge, radius, dist);
    
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
