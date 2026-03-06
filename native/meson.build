project('compose-av',
        'cpp', 'java',
        default_options : {
          'cpp_std' : 'c++20',
          'wrap_mode' : 'forcefallback',
        },
        meson_version : '>=1.2.0',
)

add_global_arguments('-fPIC', language : 'c')
add_global_arguments('-fPIC', language : 'cpp')
add_global_link_arguments(
  language : 'cpp',
)

mpv = subproject('mpv',
                 default_options : {
                   'libmpv' : true,
                   'drm' : 'enabled',
                   'openal' : 'enabled',
                   'wrap_mode' : 'forcefallback',
                   'default_library' : 'static',
                   'prefer_static' : true,
                 }
)
subdir('src')
