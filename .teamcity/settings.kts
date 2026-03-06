import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubConnection
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2025.11"

project {
    buildType(BuildSnapshot)
    buildType(BuildRelease)

    features {
        githubConnection {
            id = "PROJECT_EXT_10"
            displayName = "GitHub.com"
            clientId = "Ov23liT97vPVEYmQSgyV"
            clientSecret = "credentialsJSON:caa167f1-c169-4bab-add1-445d1770fe1d"
        }

        untrustedBuildsSettings {
            enableLog = true
            manualRunsApproved = true
            defaultAction = UntrustedBuildsSettings.DefaultAction.APPROVE
            approvalRules = """
                (groups:MAINTAINERS):1
            """.trimIndent()
        }
    }
}

abstract class Build(
    val buildTypeName: String,
    val publishRepository: String,
    val publishUsername: String,
    val publishPassword: String,
    val publishVersion: String,
    val trigger: VcsTrigger.() -> Unit,
) : BuildType({
    name = buildTypeName

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            branchFilter = "+:refs/heads/*"
        }
    }

    features {
        perfmon {
        }

        pullRequests {
            provider = github {
                authType = vcsRoot()
                ignoreDrafts = true
            }
        }
    }

    requirements {
        exists("system.nix.store")
    }

    params {
        text("env.MAVEN_REPO_URL", publishRepository, display = ParameterDisplay.HIDDEN, readOnly = true)
        text("env.MAVEN_REPO_USERNAME", publishUsername, display = ParameterDisplay.HIDDEN, readOnly = true)
        password("env.MAVEN_REPO_PASSWORD", publishPassword, display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    steps {
        gradle {
            tasks = "build publish"
            gradleParams = "-Pdeploy.version=${publishVersion} -Pdeploy.kotlin=true"
            incremental = true
        }
    }
})

object BuildSnapshot : Build(
    buildTypeName = "Build Snapshot",
    publishRepository = "https://repoflow.silenium.dev/api/maven/public/maven-snapshots",
    publishUsername = "teamcitypublic",
    publishPassword = "credentialsJSON:c8524851-3a17-4ea4-ac26-49a99c5c387c",
    publishVersion = "%build.vcs.number%",
    trigger = {
        branchFilter = "+:refs/heads/*"
    }
)

object BuildRelease : Build(
    buildTypeName = "Build Release",
    publishRepository = "https://repoflow.silenium.dev/api/maven/public/maven-releases",
    publishUsername = "teamcitypublic",
    publishPassword = "credentialsJSON:c8524851-3a17-4ea4-ac26-49a99c5c387c",
    publishVersion = "%build.vcs.number%",
    trigger = {
        branchFilter = "+:refs/tags/*"
    }
)
