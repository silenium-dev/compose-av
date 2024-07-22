#version 300 es
#ifdef GL_ES
// Set default precision to medium
precision mediump int;
precision mediump float;
#endif

uniform sampler2D tex_y;
uniform sampler2D tex_u;
uniform sampler2D tex_v;
uniform ivec4 swizzle_0;
uniform ivec4 swizzle_1;
uniform ivec4 swizzle_2;
uniform int tex_format;
uniform int enableHDR;
uniform int limitedRange;

in vec2 texCoords;
out vec4 color;

float gamma = 2.2;
vec3 toLinear(in vec3 colour) { return pow(colour, vec3(gamma)); }
vec3 toHDR(in vec3 colour, in float range) { return toLinear(colour) * range; }

void main(){
    vec3 yuv;
    vec4 rgba;

    vec4 yIn = texture(tex_y, texCoords);
    vec4 uIn = texture(tex_u, texCoords);
    vec4 vIn = texture(tex_v, texCoords);

    if (tex_format == 1) { // rgb(a)
        rgba.rgb = yIn.rgb;
    } else if (tex_format == 2) { // nv12 | p010le
        yuv.r = yIn[swizzle_0.r] - (limitedRange > 0 ? 0.0625 : 0.0);
        yuv.g = uIn[swizzle_1.r] - 0.5;
        yuv.b = uIn[swizzle_1.g] - 0.5;
    }

    if (tex_format == 2 || tex_format == 3) {
        rgba.rgb = mat3(1.0, 1.0, 1.0,
        0.0, -0.39465, 2.03211,
        1.13983, -0.58060, 0.0) * yuv;
    }

    if (enableHDR > 0){
        rgba.rgb = toHDR(rgba.rgb, 1.0);
    }

    rgba.a = 1.0;
    color = rgba;
    //    float rOut = 0.0;
    //    float gOut = 0.0;
    //    if (swizzle_u.r == 1) {
    //        rOut = 1.0f;
    //    } else {
    //        rOut = 0.0f;
    //    }
    //    if (swizzle_u.g == 1) {
    //        gOut = 1.0f;
    //    } else {
    //        gOut = 0.0f;
    //    }
    //    color = vec4(rOut, gOut, 0.0, 1.0);
}
