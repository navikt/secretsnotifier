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
            val apiResponse: AllTeamsResponse = run(allTeamsQuery(teamsCursor))
            allTeams += apiResponse.data.teams.nodes
            teamsCursor = apiResponse.data.teams.pageInfo.endCursor ?: ""
        } while (apiResponse.data.teams.pageInfo.hasNextPage)

        allTeams.forEach { team ->
            team.repositories.nodes += additionalReposFor(team)
        }

        return allTeams
    }

    private suspend fun additionalReposFor(team: Team): List<NaisApiRepository> {
        var hasMore = team.repositories.pageInfo.hasNextPage
        if (!hasMore) return emptyList()

        val additionalRepos = emptyList<NaisApiRepository>().toMutableList()
        var cursor = team.repositories.pageInfo.endCursor ?: ""
        while (hasMore) {
            val apiResponse: SingleTeamResponse = run(singleTeamQuery(team.slug, cursor))
            additionalRepos += apiResponse.data.team.repositories.nodes
            hasMore = apiResponse.data.team.repositories.pageInfo.hasNextPage
            cursor = apiResponse.data.team.repositories.pageInfo.endCursor ?: ""
        }
        return additionalRepos
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
                repositories(first:100 after:"") {
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
data class Team(val slug: String, val slackChannel: String?, var repositories: Repositories = Repositories(nodes = emptyList<NaisApiRepository>().toMutableList(), PageInfo(false,"")))

@Serializable
data class Repositories(var nodes: MutableList<NaisApiRepository>, val pageInfo: PageInfo)

@Serializable
data class NaisApiRepository(val name: String)

@Serializable
data class PageInfo(val hasNextPage: Boolean, val endCursor: String?)


