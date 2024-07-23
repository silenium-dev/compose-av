dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(":os-utils")
project(":os-utils").projectDir = file("../os-utils")
