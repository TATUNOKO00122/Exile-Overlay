#version 150

uniform float FillPercent;
uniform int HalfMode;
uniform float OverlapWidth;
uniform int UseTexture;
uniform int EnableNoise;
uniform int EnableLiquidShadow;
uniform int EnableOrbInnerShadow;
uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 uv = texCoord0;
    
    float dist = length(uv - center);
    float radius = 0.48;
    
    if (dist > radius) {
        discard;
    }
    
    float liquidLevel = 1.0 - FillPercent;
    
    // 液面トップ境界線のアンチエイリアシング処理
    float surface_blur = 0.003;
    float surface_alpha = smoothstep(liquidLevel - surface_blur, liquidLevel + surface_blur, uv.y);
    if (surface_alpha <= 0.0) {
        discard;
    }
    
    float boundary_x_h3 = 0.0;
    if (HalfMode == 3) {
        float dy = uv.y - 0.5;
        float half_w = sqrt(max(0.0, radius * radius - dy * dy));
        boundary_x_h3 = (0.5 - half_w) + (1.0 - OverlapWidth) * (2.0 * half_w);
    }
    
    if (EnableNoise == 1) {
        // ノイズありの横方向境界事前破棄
        if (HalfMode == 1 && uv.x > 0.5 + 0.05) discard;
        if (HalfMode == 2 && uv.x < 0.5 - 0.05) discard;
        if (HalfMode == 3 && uv.x < boundary_x_h3 - 0.05) discard;
    } else {
        // 元の状態（ノイズなし）のハードな破棄
        if (HalfMode == 1 && uv.x > 0.5) discard;
        if (HalfMode == 2 && uv.x < 0.5) discard;
        if (HalfMode == 3 && uv.x < boundary_x_h3) discard;
    }
    
    float edge = 0.02;
    float alpha = (1.0 - smoothstep(radius - edge, radius, dist)) * surface_alpha;
    
    float check_x = uv.x;
    if (EnableNoise == 1) {
        float n = valueNoise(uv * 60.0);
        float noise_offset = (n - 0.5) * 0.04; 
        float edge_blur = 0.005;
        check_x = uv.x + noise_offset;
        
        if (HalfMode == 1) {
            alpha *= 1.0 - smoothstep(0.5 - edge_blur, 0.5 + edge_blur, check_x);
        } else if (HalfMode == 2) {
            alpha *= smoothstep(0.5 - edge_blur, 0.5 + edge_blur, check_x);
        } else if (HalfMode == 3) {
            alpha *= smoothstep(boundary_x_h3 - edge_blur, boundary_x_h3 + edge_blur, check_x);
        }
    } else {
        // 元の状態（ノイズなし）ではHalfMode 3のみアンチエイリアシング
        if (HalfMode == 3) {
            float transition = 0.005; // 少しだけ滑らかに
            alpha *= smoothstep(boundary_x_h3, boundary_x_h3 + transition, uv.x);
        }
    }
    
    vec3 final_rgb;
    float final_alpha;
    
    if (UseTexture == 1) {
        vec4 texColor = texture(Sampler0, uv);
        final_rgb = texColor.rgb;
        final_alpha = texColor.a * alpha;
    } else {
        final_rgb = vertexColor.rgb;
        final_alpha = vertexColor.a * alpha;
    }
    
    // 1. シャープで視認性のある液面トップ境界エフェクト（PoE/Diabloスタイルのくっきりした表面張りリム）
    if (EnableLiquidShadow == 1 && alpha > 0.0) {
        float dist_to_surface = uv.y - liquidLevel;
        if (dist_to_surface >= 0.0 && dist_to_surface < 0.025) {
            // (a) 液面トップの鮮やかな輝きリム（自色ベースの輝度+45% ＋ ほんのり光彩）
            float rim = pow(max(0.0, 1.0 - dist_to_surface / 0.008), 2.0);
            vec3 rim_glow = final_rgb * 1.45 + vec3(0.15);
            final_rgb = mix(final_rgb, rim_glow, rim * 0.85);
            
            // (b) リム直下のシャドウ（15%減衰）で境界線のコントラストと立体感をしっかり表現
            float shadow = smoothstep(0.003, 0.008, dist_to_surface) * (1.0 - smoothstep(0.008, 0.022, dist_to_surface));
            final_rgb *= (1.0 - shadow * 0.15);
        }
    }
    
    // 2. HalfMode == 3 (シールド等) 垂直境界線の発光エフェクト
    if (HalfMode == 3 && alpha > 0.0) {
        float dist_to_boundary = check_x - boundary_x_h3;
        if (dist_to_boundary > 0.0) {
            // 球体としての立体感（中央が明るく、縁が暗い）
            float sphere_z = sqrt(max(0.0, radius * radius - dist * dist));
            float lighting = 0.6 + 0.5 * (sphere_z / radius);
            final_rgb *= lighting;
            
            // 境界線のシールド発光（明るいシアン色）
            float glow_width = 0.04;
            float glow_intensity = pow(max(0.0, 1.0 - dist_to_boundary / glow_width), 2.0);
            vec3 glow_color = vec3(0.4, 0.9, 1.0);
            final_rgb = mix(final_rgb, glow_color, glow_intensity * 0.8);
            
            // 境界線ギリギリの白いハイライト（よりシャープな光）
            float core_intensity = pow(max(0.0, 1.0 - dist_to_boundary / 0.008), 2.0);
            final_rgb = mix(final_rgb, vec3(1.0, 1.0, 1.0), core_intensity * 0.7);
        }
    }
    
    // 3. オーブ外縁のインナーシャドウ（縁に向かって自然に暗くなるグラデーション立体感）
    if (EnableOrbInnerShadow == 1 && alpha > 0.0) {
        float normalized_dist = clamp(dist / radius, 0.0, 1.0); // 0.0 (中心) 〜 1.0 (最外縁)
        // 半径の約55%〜98%の範囲で縁の近くだけにうっすら影を適用
        float edge_vignette = smoothstep(0.55, 0.98, normalized_dist);
        // 最外縁で最大22%減衰（明るさ78%程度の控えめで自然な影）
        float shadow_factor = 1.0 - edge_vignette * 0.22;
        final_rgb *= shadow_factor;
    }

    
    fragColor = vec4(final_rgb, final_alpha);
}

