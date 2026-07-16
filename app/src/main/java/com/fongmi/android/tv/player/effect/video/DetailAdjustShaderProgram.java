package com.fongmi.android.tv.player.effect.video;

import androidx.annotation.NonNull;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.Size;

final class DetailAdjustShaderProgram extends VideoAdjustShaderProgram {

    private static final String FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexelSize;
            uniform float uSharpness;
            uniform float uThreshold;
            uniform float uShadowLift;
            uniform float uShadowStart;
            uniform float uShadowEnd;
            varying vec2 vTexSamplingCoord;
            const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
            void main() {
              vec4 center = texture2D(uTexSampler, vTexSamplingCoord);
              vec3 color = center.rgb;
              if (uSharpness > 0.0) {
                vec3 left = texture2D(uTexSampler, vTexSamplingCoord + vec2(-uTexelSize.x, 0.0)).rgb;
                vec3 right = texture2D(uTexSampler, vTexSamplingCoord + vec2(uTexelSize.x, 0.0)).rgb;
                vec3 up = texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, -uTexelSize.y)).rgb;
                vec3 down = texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, uTexelSize.y)).rgb;
                vec3 edge = color * 4.0 - left - right - up - down;
                float edgeStrength = max(max(abs(edge.r), abs(edge.g)), abs(edge.b));
                float mask = smoothstep(uThreshold, uThreshold * 2.0 + 0.0001, edgeStrength);
                color = clamp(color + edge * uSharpness * mask, 0.0, 1.0);
              }
              if (uShadowLift > 0.0) {
                float luma = dot(color, LUMA);
                float shadow = 1.0 - smoothstep(uShadowStart, uShadowEnd, luma);
                color = clamp(color + (1.0 - color) * uShadowLift * shadow, 0.0, 1.0);
              }
              gl_FragColor = vec4(color, center.a);
            }
            """;

    private final DetailAdjustEffect effect;

    DetailAdjustShaderProgram(boolean useHighPrecisionColorComponents, DetailAdjustEffect effect) throws VideoFrameProcessingException {
        super(useHighPrecisionColorComponents, FRAGMENT_SHADER);
        this.effect = effect;
    }

    @NonNull
    @Override
    public Size configure(int inputWidth, int inputHeight) {
        glProgram.setFloatsUniform("uTexelSize", new float[]{1.0f / inputWidth, 1.0f / inputHeight});
        return super.configure(inputWidth, inputHeight);
    }

    @Override
    protected void bindUniforms() {
        glProgram.setFloatUniform("uSharpness", effect.getSharpness());
        glProgram.setFloatUniform("uThreshold", effect.getThreshold());
        glProgram.setFloatUniform("uShadowLift", effect.getShadowLift());
        glProgram.setFloatUniform("uShadowStart", effect.getShadowStart());
        glProgram.setFloatUniform("uShadowEnd", effect.getShadowEnd());
    }
}
