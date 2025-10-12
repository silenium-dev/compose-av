mpv_dep = mpv.get_variable('libmpv_dep')
jni = dependency('jni')
egl = dependency('egl')
glx = dependency('glx')
x11 = dependency('X11')

shared_library('compose-av',
        'util/Errors.cpp',
        'helper/errors.hpp',
        'helper/errors.cpp',
        'mpv/MPV.cpp',
        dependencies : [jni, egl, glx, x11, mpv_dep],
)
