project(
  'mpv', 'c',
  meson_version : '>=1.2.0',
)

target_cpu = host_machine.cpu_family()
if target_cpu == 'x86_64'
  machine_arch = 'X64'
elif target_cpu == 'aarch64'
  machine_arch = 'ARM64'
elif target_cpu == 'x86'
  machine_arch = 'X86'
elif target_cpu == 'arm'
  machine_arch = 'ARM'
else
  error('Unsupported architecture: ' + target_cpu)
endif

gendef = find_program('gendef')
llvm_lib = find_program('llvm-lib')

mpv_dll = files('libmpv-2.dll')
mpv_def = custom_target('gen-mpv-def',
                        input : [mpv_dll],
                        output : ['libmpv-2.def'],
                        capture : true,
                        command : [gendef, '-', '@INPUT@'],
)
mpv_lib = custom_target('gen-mpv-lib',
                        input : [mpv_dll, mpv_def],
                        output : ['mpv.lib'],
                        command : [llvm_lib, '/def:@INPUT1@', '/name:@INPUT0@', '/out:@OUTPUT0@', '/MACHINE:' + machine_arch],
)


libmpv_dep = declare_dependency(
  version : '2',
  link_whole : mpv_lib,
  include_directories : include_directories('include'),
)

meson.override_dependency('libmpv', libmpv_dep)
