package com.fongmi.android.tv.player.effect.video;

import androidx.media3.common.VideoFrameProcessingException;

final class ToneAdjustShaderProgram extends VideoAdjustShaderProgram {

    private static final String FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform float uGamma;
            uniform float uHue;
            varying vec2 vTexSamplingCoord;
            vec3 rotateHue(vec3 color, float hue) {
              float angle = radians(hue);
              float s = sin(angle);
              float c = cos(angle);
              float y = dot(color, vec3(0.299, 0.587, 0.114));
              float i = dot(color, vec3(0.596, -0.274, -0.322));
              float q = dot(color, vec3(0.211, -0.523, 0.312));
              float ii = i * c - q * s;
              float qq = i * s + q * c;
              return clamp(vec3(y + 0.956 * ii + 0.621 * qq, y - 0.272 * ii - 0.647 * qq, y - 1.106 * ii + 1.703 * qq), 0.0, 1.0);
            }
            void main() {
              vec4 sample = texture2D(uTexSampler, vTexSamplingCoord);
              vec3 color = pow(clamp(sample.rgb, 0.0, 1.0), vec3(1.0 / max(uGamma, 0.0001)));
              if (abs(uHue) > 0.0001) color = rotateHue(color, uHue);
              gl_FragColor = vec4(color, sample.a);
            }
            """;

    private final ToneAdjustEffect effect;

    ToneAdjustShaderProgram(boolean useHighPrecisionColorComponents, ToneAdjustEffect effect) throws VideoFrameProcessingException {
        super(useHighPrecisionColorComponents, FRAGMENT_SHADER);
        this.effect = effect;
    }

    @Override
    protected void bindUniforms() {
        glProgram.setFloatUniform("uGamma", effect.getGamma());
        glProgram.setFloatUniform("uHue", effect.getHue());
    }
}
