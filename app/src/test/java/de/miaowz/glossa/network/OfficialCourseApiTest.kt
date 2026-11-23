package de.miaowz.glossa.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OfficialCourseApiTest {

    private val requestedPaths = mutableListOf<String>()

    /** Serves [routes] by path; like `res.end(JSON.stringify(...))` there is no JSON content type. */
    private fun client(
        routes: Map<String, String>,
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: ContentType? = null,
    ) = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        requestedPaths += path
        val headers = contentType?.let { headersOf(HttpHeaders.ContentType, it.toString()) } ?: headersOf()
        val body = routes[path]
        if (body == null) respond("", HttpStatusCode.NotFound) else respond(body, status, headers)
    }) { glossaDefaults() }

    @Test
    fun coursesParseWithoutJsonContentType() = runBlocking {
        val api = OfficialCourseApi(client(mapOf("/api/course" to BackendSamples.COURSES)))

        val courses = api.getCourses()

        assertEquals(2, courses.size)
        assertEquals(listOf("/api/course"), requestedPaths)
    }

    @Test
    fun coursesParseWithHtmlContentType() = runBlocking {
        val api = OfficialCourseApi(
            client(mapOf("/api/course" to BackendSamples.COURSES), contentType = ContentType.Text.Html),
        )

        assertEquals(2, api.getCourses().size)
    }

    @Test
    fun singleCourseToleratesArrayResponse() = runBlocking {
        val api = OfficialCourseApi(client(mapOf("/api/course/2" to BackendSamples.COURSE_AS_ARRAY)))

        assertEquals("N5", api.getCourse(2)?.courseLevel)
    }

    @Test
    fun singleCourseIsNullForEmptyArray() = runBlocking {
        val api = OfficialCourseApi(client(mapOf("/api/course/9" to "[]")))

        assertNull(api.getCourse(9))
    }

    @Test
    fun lecturesUseCourseEndpoint() = runBlocking {
        val api = OfficialCourseLectureApi(client(mapOf("/api/course/2/lectures" to BackendSamples.LECTURES)))

        val lectures = api.getLecturesForCourse(2)

        assertEquals(listOf(5L, 6L), lectures.map { it.id })
        assertEquals(listOf("/api/course/2/lectures"), requestedPaths)
    }

    @Test
    fun httpErrorsThrowResponseException() {
        val api = OfficialCourseApi(
            client(mapOf("/api/course" to "Internal Server Error"), status = HttpStatusCode.InternalServerError),
        )

        assertThrows(ResponseException::class.java) { runBlocking { api.getCourses() } }
    }
}
