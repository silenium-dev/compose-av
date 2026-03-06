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
        vcs(trigger)
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
        text("deploy.repo-url", publishRepository, display = ParameterDisplay.HIDDEN, readOnly = true)
        text("deploy.username", publishUsername, display = ParameterDisplay.HIDDEN, readOnly = true)
        password("deploy.password", publishPassword, display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    steps {
        gradle {
            tasks = """
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pdeploy.version=${publishVersion}
                |-Pdeploy.enabled=true
                |-Pdeploy.repo-url=%deploy.repo-url%
                |-Pdeploy.username=%deploy.username%
                |-Pdeploy.password=%deploy.password%
                |--scan
                |--info
            """.trimMargin().replace("\n", " ")
            incremental = true
        }
    }
})

object BuildSnapshot : Build(
    buildTypeName = "Build Snapshot",
    publishRepository = "https://repoflow.silenium.dev/api/maven/public/maven-snapshots",
    publishUsername = "teamcitypublic",
    publishPassword = "credentialsJSON:4f61365b-e397-46e3-8c84-d48c2c91b687",
    publishVersion = "dev-%build.vcs.number%",
    trigger = {
        branchFilter = "+:refs/heads/*"
    }
)

object BuildRelease : Build(
    buildTypeName = "Build Release",
    publishRepository = "https://repoflow.silenium.dev/api/maven/public/maven-releases",
    publishUsername = "teamcitypublic",
    publishPassword = "credentialsJSON:4f61365b-e397-46e3-8c84-d48c2c91b687",
    publishVersion = "%build.vcs.number%",
    trigger = {
        branchFilter = "+:refs/tags/*"
    }
)
