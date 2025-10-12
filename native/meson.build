project('compose-av',
        'cpp', 'java',
        default_options : {
            'cpp_std' : 'c++20',
        }
)
add_global_arguments('-fPIC', language : 'c')
add_global_arguments('-fPIC', language : 'cpp')
mpv = subproject('mpv',
                 default_options : {
                     'libmpv' : true,
                     'drm' : 'enabled',
                     'openal' : 'enabled',
                     'force_fallback_for' : ['expat', 'ffmpeg', 'libjpeg', 'libass', 'libplacebo', 'libpng', 'openal', 'zlib'],
                     'default_library' : 'static',
                     'prefer_static' : true,
                 }
)
subdir('src')
