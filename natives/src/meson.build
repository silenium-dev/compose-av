mpv_dep = mpv.get_variable('libmpv_dep')
jni = dependency('jni')

deps = [jni, mpv_dep]
link_args = []

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
  va = dependency('libva')
  deps += [egl, glx, x11, va]
  sources += ['mpv/mpv_linux.cpp']
  add_project_arguments('-DTARGET_LINUX', language: 'cpp')
elif os == 'windows'
  sources += ['mpv/mpv_windows.cpp']
  cc = meson.get_compiler('cpp')
#  gl = cc.find_library('opengl32')
#  deps += [gl]
  link_args += ['opengl32.lib']
  add_project_arguments('-DTARGET_WINDOWS', language: 'cpp')
endif

shared_library(
  sources,
  dependencies : deps,
  link_args: link_args,
  cpp_args : ['-Wimplicit-fallthrough'],
)
