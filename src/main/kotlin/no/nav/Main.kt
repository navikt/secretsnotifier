package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    println("Looking for repos with secrets alerts...")
    //val gitHub = GitHub(httpClient, envOrDie("GITHUB_TOKEN"))
    val naisAPI = NaisAPI(httpClient, envOrDie("TEAMS_TOKEN"))
    //val slack = Slack(httpClient, envOrDie("SLACK_TOKEN"))

    val allTeamsAndTheirRepos = naisAPI.allTeamsAndTheirRepos()
    val repoCount = allTeamsAndTheirRepos.values.sumOf { it.size }
    println("Found ${allTeamsAndTheirRepos.size} teams with a total of $repoCount repos")

    println("Done!")
}

private fun envOrDie(name: String) = System.getProperty(name)
    ?: System.getenv(name)
    ?: throw RuntimeException("Unable to find env var $name, I'm useless without it")

private val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}

private fun linksTo(repos: List<RepoWithSecret>) =
    repos.joinToString(separator = "\n• ", prefix = "• ") { repo ->
        "<https://github.com/${repo.fullName}/security/secret-scanning|${repo.name()} (${repo.secretType})>"
    }