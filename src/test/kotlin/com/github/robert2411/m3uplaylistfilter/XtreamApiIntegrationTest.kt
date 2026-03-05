package com.github.robert2411.m3uplaylistfilter

import io.github.saifullah.xtream.Xtream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XtreamApiIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private fun client(username: String = "user", password: String = "pass") = Xtream {
        auth {
            protocol = "http"
            host = "localhost"
            this.port = this@XtreamApiIntegrationTest.port
            this.username = username
            this.password = password
        }
        socketTimeoutMillis = 10_000
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 10_000
    }

    @Test
    fun `auth succeeds with correct credentials`() = runBlocking {
        val response = client().auth()
        assertEquals(1, response.userInfo.auth, "Expected auth=1 for valid credentials")
    }

    @Test
    fun `auth fails with wrong credentials`() = runBlocking {
        val response = client("wrong", "creds").auth()
        assertEquals(0, response.userInfo.auth, "Expected auth=0 for invalid credentials")
    }

    @Test
    fun `get movie categories returns a list`() = runBlocking {
        val categories = client().movie.getMovieCategories()
        assertNotNull(categories, "Movie categories should not be null")
    }

    @Test
    fun `get movies returns a list`() = runBlocking {
        val movies = client().movie.getMovies()
        assertNotNull(movies, "Movies list should not be null")
    }

    @Test
    fun `get series categories returns a list`() = runBlocking {
        val categories = client().tvSeries.getTvSeriesCategories()
        assertNotNull(categories, "Series categories should not be null")
    }

    @Test
    fun `get series returns a list`() = runBlocking {
        val series = client().tvSeries.getTvSeries()
        assertNotNull(series, "Series list should not be null")
    }
}
