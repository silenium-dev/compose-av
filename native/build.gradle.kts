plugins {
    id("av-natives")
}

natives {
    libName = rootProject.name
    libVersion = "0.1.0"
    nixFlake = file("flake.nix")
    sourceFiles.from("src", "meson.build", "subprojects.tpl")
    showLogs = providers.environmentVariable("CI").orElse("false").map { it != "false" }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
