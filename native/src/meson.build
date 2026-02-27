mpv_dep = mpv.get_variable('libmpv_dep')
jni = dependency('jni')

deps = [jni, mpv_dep]

sources = [
  'compose-av',
  'util/Errors.cpp',
  'helper/errors.hpp',
  'helper/errors.cpp',
  'mpv/MPV.cpp',
  'mpv/mpv_platform.hpp',
]

os = host_machine.system()
if os == 'linux'
  egl = dependency('egl')
  glx = dependency('glx')
  x11 = dependency('x11')
  deps += [egl, glx, x11]
  sources += ['mpv/mpv_linux.cpp']
elif os == 'windows'
  sources += ['mpv/mpv_windows.cpp']
  cc = meson.get_compiler('cpp')
  gl = cc.find_library('opengl32')
  deps += [gl]
endif

shared_library(
  sources,
  dependencies : deps,
  cpp_args : ['-Wimplicit-fallthrough'],
)
