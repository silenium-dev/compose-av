jni_headers_dep = jni_headers.get_variable('jni_headers_dep')
deps = [jni_headers_dep]
link_args = []

if get_option('mpv_subproject')
  mpv_dep = mpv.get_variable('libmpv_dep')
  deps += [mpv_dep]
else
  mpv = dependency('mpv')
  deps += [mpv]
endif

sources = [
  'compose-av',
  'util/Errors.cpp',
  'helper/JniCallRef.hpp',
  'helper/JniMpvCallback.cpp',
  'helper/JniMpvCallback.hpp',
  'helper/MpvCallback.hpp',
  'helper/results.cpp',
  'helper/results.hpp',
  'mpv/commands.cpp',
  'mpv/instance.cpp',
  'mpv/instance.hpp',
  'mpv/lifecycle.cpp',
  'mpv/mpv_platform.hpp',
  'mpv/MPVException.cpp',
  'mpv/MPVException.hpp',
  'mpv/observe.cpp',
  'mpv/properties_async.cpp',
  'mpv/properties_sync.cpp',
  'mpv/render.cpp',
  'mpv/renderer.hpp',
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
  link_args += ['opengl32.lib']
  add_project_arguments('-DTARGET_WINDOWS', language: 'cpp')
endif

shared_library(
  sources,
  dependencies : deps,
  link_args: link_args,
  cpp_args : ['-Wimplicit-fallthrough'],
)
