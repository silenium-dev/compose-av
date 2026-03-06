plugins {
    id("av-natives")
}

val deployKotlin = (findProperty("deploy.kotlin") as String?)?.toBoolean() ?: true

natives {
    libName = rootProject.name
    libVersion = "0.1.0"
    nixFlake = file("flake.nix")
    sourceFiles.from("src", "meson.build", "subprojects.tpl")
}

publishing {
    publications {
        if (deployKotlin) {
            create<MavenPublication>("maven") {
                from(components["kotlin"])
            }
        }
    }
}
