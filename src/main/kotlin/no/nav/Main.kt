package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("secretsnotifier")

fun main() = runBlocking {
//    val gitHub = GitHub(httpClient, envOrDie("GITHUB_TOKEN"))
    val naisAPI = NaisAPI(httpClient, envOrDie("TEAMS_TOKEN"))
//    val slack = Slack(httpClient, envOrDie("SLACK_TOKEN"))

//    val reposWithSecretAlerts = gitHub.reposWithSecretAlerts("navikt")
//    if (reposWithSecretAlerts.isEmpty()) {
//        log.info("No repos with secret alerts found, exiting.")
//        exitProcess(0)
//    }
//
//    log.info("Found ${reposWithSecretAlerts.size} repos with secret alerts, now let's find their owners")

    val allTeamsAndTheirRepos = naisAPI.allTeams()
    val repoCount = allTeamsAndTheirRepos.sumOf { it.repositories?.nodes?.size ?: 0 }
    log.info("Found ${allTeamsAndTheirRepos.size} teams with a total of $repoCount repos")

//    reposWithSecretAlerts.forEach { repo ->
//        val owner = ownerFor(repo, allTeamsAndTheirRepos)
//        owner?.let {
//            slack.sendAlert(repo, owner)
//        } ?: log.warn("Unable to find an owner for ${repo.fullName}")
//    }

    log.info("Done!")
}

private fun envOrDie(name: String) = System.getProperty(name)
    ?: System.getenv(name)
    ?: throw RuntimeException("Unable to find env var $name, I'm useless without it")

internal fun ownerFor(repo: RepoWithSecret, allTeamsAndTheirRepos: List<Team>) =
    allTeamsAndTheirRepos.firstOrNull {
        it.repositories?.nodes?.contains(NaisApiRepository(repo.fullName)) ?: false
    }


private val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}
