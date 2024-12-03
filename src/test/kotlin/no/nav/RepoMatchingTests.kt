package no.nav

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RepoMatchingTests {
    @Test
    fun `first team with supplied repo is designated as owner`() {
        val teams = listOf(
            Team("team1", "#yolo1", Repositories(mutableListOf(NaisApiRepository("repo1")), PageInfo(false, ""))),
            Team("team2", "#yolo2", Repositories(mutableListOf(NaisApiRepository("repo2")), PageInfo(false, ""))),
            Team("team3", "#yolo3", Repositories(mutableListOf(NaisApiRepository("repo2")), PageInfo(false, "")))
        )

        val expected = "team2"
        val actual = ownerFor(RepoWithSecret("repo2", "yolo-token"), teams)?.slug
        assertEquals(expected, actual)
    }
}