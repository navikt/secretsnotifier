package no.nav

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpHeaders.UserAgent
import kotlinx.serialization.Serializable

class NaisAPI(private val http: HttpClient, private val authToken: String) {
    private val baseUrl = "https://console.nav.cloud.nais.io/graphql"

    suspend fun allTeams(): List<Team> {
        val allTeams = mutableListOf<Team>()
        var teamsCursor = ""
        do {
            log.info("Querying for teams at offset '$teamsCursor'")
            val apiResponse: AllTeamsResponse = run(allTeamsQuery(teamsCursor))
            allTeams += apiResponse.data.teams.nodes
            teamsCursor = apiResponse.data.teams.pageInfo.endCursor ?: ""
        } while (apiResponse.data.teams.pageInfo.hasNextPage)

        allTeams.forEach { team ->
            team.repositories.nodes = reposBelongingTo(team)
        }

        return allTeams
    }

    private suspend fun reposBelongingTo(team: Team): MutableList<NaisApiRepository> {
        val allRepos = team.repositories.nodes
        var keepGoing = team.repositories.pageInfo.hasNextPage
        var cursor = team.repositories.pageInfo.endCursor ?: ""
        while (keepGoing) {
            val apiResponse: SingleTeamResponse = run(singleTeamQuery(team.slug, cursor))
            allRepos += apiResponse.data.team.repositories.nodes
            keepGoing = apiResponse.data.team.repositories.pageInfo.hasNextPage
            cursor = apiResponse.data.team.repositories.pageInfo.endCursor ?: ""
        }
        return allRepos
    }

    private suspend inline fun <reified T> run(queryString: String): T {
        val reqBody = RequestBody(queryString.replace("\n", " "))
        return http.post(baseUrl) {
            header(Authorization, "Bearer $authToken")
            header(UserAgent, "NAV IT McBotFace")
            header(ContentType, Json)
            setBody(reqBody)
        }.body<T>()
    }

    private fun allTeamsQuery(teamsCursor: String) =
        """query allTeams {
          teams(first:100 after:"$teamsCursor") {
             pageInfo {
                totalCount
                hasNextPage
                endCursor
             }
             nodes {
                slug
                slackChannel
             }
          }
      } """

    private fun singleTeamQuery(slug: String, repoCursor: String) = """
    query singleTeam {
       team(slug:"$slug") {
          slug
          repositories(first:100 after:"$repoCursor") {
             pageInfo {
                totalCount
                hasNextPage
                endCursor
             }
             nodes {
                name
             }
          }
       }
    }
"""
}

@Serializable
data class RequestBody(val query: String)

@Serializable
data class AllTeamsResponse(val data: AllTeamsResponseData)

@Serializable
data class SingleTeamResponse(val data: SingleTeamResponseData)

@Serializable
data class AllTeamsResponseData(val teams: AllTeams)

@Serializable
data class SingleTeamResponseData(val team: Team)

@Serializable
data class AllTeams(val nodes: List<Team>, val pageInfo: PageInfo)

@Serializable
data class Team(val slug: String, val slackChannel: String, val repositories: Repositories)

@Serializable
data class Repositories(var nodes: MutableList<NaisApiRepository>, val pageInfo: PageInfo)

@Serializable
data class NaisApiRepository(val name: String)

@Serializable
data class PageInfo(val hasNextPage: Boolean, val endCursor: String?)


