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

    suspend fun allTeamsAndTheirRepos(): Map<String, List<NaisApiRepository>> {
        val allTeams = mutableMapOf<String, List<NaisApiRepository>>()
        var teamsOffset = ""
        do {
            val gqlResponse = performGqlRequest(teamsOffset)
            gqlResponse.data.teams.nodes.forEach { team ->
                allTeams[team.slug] = team.repositories.nodes
            }
            teamsOffset = gqlResponse.data.teams.pageInfo.endCursor
        } while (gqlResponse.data.teams.pageInfo.hasNextPage)

        return allTeams
    }

    private suspend fun performGqlRequest(teamsOffset: String): PaginatedGqlResponse {
        val queryString = """query getTeamsAndRepos {
                                teams(first:100 after:"$teamsOffset") {
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
        val reqBody = RequestBody(queryString.replace("\n", " "))
        return http.post(baseUrl) {
            header(Authorization, "Bearer $authToken")
            header(UserAgent, "NAV IT McBotFace")
            header(ContentType, Json)
            setBody(reqBody)
        }.body<PaginatedGqlResponse>()
    }
}

@Serializable
data class RequestBody(val query: String)

@Serializable
data class PaginatedGqlResponse(val data: GqlResponseData)

@Serializable
data class GqlResponseData(val teams: GqlResponseTeams)

@Serializable
data class GqlResponseTeams(val nodes: List<Team>, val pageInfo: PageInfo)

@Serializable
data class Team(val slug: String, val slackChannel: String, val repositories: NaisApiRepositories)

@Serializable
data class NaisApiRepositories(val nodes: List<NaisApiRepository>, val pageInfo: PageInfo)

@Serializable
data class NaisApiRepository(val name: String)

@Serializable
data class PageInfo(val hasNextPage: Boolean, val endCursor: String)


