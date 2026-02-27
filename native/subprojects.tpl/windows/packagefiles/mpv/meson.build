project(
    'mpv', 'c',
    meson_version : '>=1.2.0',
)

cc = meson.get_compiler('c')

libmpv_dep = declare_dependency(
    version : '2',
    dependencies : cc.find_library('mpv', dirs : meson.current_source_dir()),
    include_directories : include_directories('include'),
)
meson.override_dependency('libmpv', libmpv_dep)
