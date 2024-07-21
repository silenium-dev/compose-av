#version 300 es
#ifdef GL_ES
// Set default precision to medium
precision mediump int;
precision mediump float;
#endif

uniform sampler2D tex_y;
uniform sampler2D tex_u;
uniform sampler2D tex_v;
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

    if (tex_format == 1) { // rgb(a)
        rgba.rgb = texture(tex_y, texCoords).rgb;
    } else if (tex_format == 2) { // nv12 | p010le
        yuv.r = (texture(tex_y, texCoords).r) - (limitedRange > 0 ? 0.0625 : 0.0);
        yuv.g = texture(tex_u, texCoords).g - 0.5;// TODO: find out, if r, g need to be swapped
        yuv.b = texture(tex_u, texCoords).r - 0.5;
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
}
