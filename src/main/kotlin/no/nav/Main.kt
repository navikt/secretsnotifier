package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val gitHub = GitHub(httpClient, envOrDie("GITHUB_TOKEN"))
    val naisAPI = NaisAPI(httpClient, envOrDie("TEAMS_TOKEN"))
    val slack = Slack(httpClient, envOrDie("SLACK_TOKEN"))

    val reposWithSecretAlerts = gitHub.reposWithSecretAlerts("navikt")
    if (reposWithSecretAlerts.isEmpty()) {
        println("No repos with secret alerts found, exiting.")
        exitProcess(0)
    }

    println("Found ${reposWithSecretAlerts.size} repos with secret alerts, now let's find their owners")

    val allTeamsAndTheirRepos = naisAPI.allTeamsAndTheirRepos()
    val repoCount = allTeamsAndTheirRepos.values.sumOf { it.size }
    println("Found ${allTeamsAndTheirRepos.size} teams with a total of $repoCount repos")

    reposWithSecretAlerts.forEach { repo ->
        val owner = ownerFor(repo, allTeamsAndTheirRepos)
        owner?.let {
            val heading = ":wave: *Hei, ${owner.slug}* :github2:"
            val msg =
                "GitHub har oppdaget hemmeligheter i repo som dere eier:\n\n ${linkTo(repo)}\n\n Dersom hemmelighetene er aktive må de *roteres* så fort som mulig, og videre varsling og steg for å avdekke evt. misbruk må iverksettes. \n\n :warning: Husk at Git aldri glemmer, så kun fjerning fra koden er IKKE tilstrekkelig.\n\nNår dette er gjort (eller dersom dette er falske positiver) lukkes varselet ved å velge i nedtrekksmenyen `Close as`.\n\nDu kan også lese mer om håndtering av hemmeligheter i vår <https://sikkerhet.nav.no/docs/sikker-utvikling/hemmeligheter|Security Playbook>"
            slack.send("#jk-tullekanal", heading, msg)
        } ?: println("Unable to find an owner for ${repo.fullName}")
    }

    println("Done!")
}

private fun envOrDie(name: String) = System.getProperty(name)
    ?: System.getenv(name)
    ?: throw RuntimeException("Unable to find env var $name, I'm useless without it")

private fun ownerFor(repo: RepoWithSecret, allTeamsAndTheirRepos: Map<Team, List<NaisApiRepository>>) =
    allTeamsAndTheirRepos
        .filter { (k, v) -> v.contains(NaisApiRepository(repo.fullName)) }
        .map { it.key }
        .firstOrNull()


private val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}

private fun linkTo(repo: RepoWithSecret) =
        "<https://github.com/${repo.fullName}/security/secret-scanning|${repo.name()} (${repo.secretType})>"