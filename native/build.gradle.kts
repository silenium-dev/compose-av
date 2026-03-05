plugins {
    id("av-natives")
}

val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true

natives {
    libName = rootProject.name
    libVersion = "0.1.0"
    nixFlake = file("flake.nix")
    sourceFiles.from("src", "meson.build", "subprojects.tpl")
}

publishing {
    publications {
        if (deployNative) {
            create<MavenPublication>("maven") {
                from(components["kotlin"])
            }
        }
    }
}
