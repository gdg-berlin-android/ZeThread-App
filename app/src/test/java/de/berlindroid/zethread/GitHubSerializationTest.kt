package de.berlindroid.zethread

import de.berlindroid.zethread.data.model.CommitAuthorIdent
import de.berlindroid.zethread.data.model.CreateFileRequest
import de.berlindroid.zethread.data.model.CreateFileResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testCreateFileRequestSerialization() {
        val request = CreateFileRequest(
            message = "feat(tapestry): crocheted during keynote [by @alex_dev]",
            content = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            author = CommitAuthorIdent(name = "Alex Dev", email = "alex@example.com"),
            committer = CommitAuthorIdent(name = "Alex Dev", email = "alex@example.com"),
            branch = "main"
        )

        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"message\":\"feat(tapestry): crocheted during keynote [by @alex_dev]\""))
        assertTrue(jsonString.contains("\"name\":\"Alex Dev\""))
        assertTrue(jsonString.contains("\"email\":\"alex@example.com\""))
        assertTrue(jsonString.contains("\"branch\":\"main\""))
    }

    @Test
    fun testCreateFileResponseDeserialization() {
        val rawResponse = """
            {
              "content": {
                "name": "patch_20260228_alex.png",
                "path": "patches/patch_20260228_alex.png",
                "sha": "3d2110d3873b40f6178a397b83e3e49ae27d4e30",
                "size": 1024,
                "download_url": "https://raw.githubusercontent.com/berlindroid/ZeThread/main/patches/patch.png"
              },
              "commit": {
                "sha": "76384fc99e6c6c33514c63713c54f20cac61b56d",
                "html_url": "https://github.com/berlindroid/ZeThread/commit/76384fc99e6c6c33514c63713c54f20cac61b56d"
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CreateFileResponse>(rawResponse)
        assertEquals("76384fc99e6c6c33514c63713c54f20cac61b56d", parsed.commit.sha)
        assertEquals("patches/patch_20260228_alex.png", parsed.content?.path)
    }

    @Test
    fun testPatchMetadataSerialization() {
        val metadata = de.berlindroid.zethread.data.model.PatchMetadata(
            id = "patch_20260228_120000_alex",
            author = de.berlindroid.zethread.data.model.PatchAuthor(
                handle = "alex",
                name = "Alex Contributor",
                email = "alex@droidcon.berlin"
            ),
            coordinates = de.berlindroid.zethread.data.model.PatchCoordinates(x = 3, y = 5),
            note = "Crocheted during opening keynote",
            timestamp = "2026-02-28T12:00:00Z",
            image = "patch.png",
            imageCommitSha = "76384fc99e6c6c33514c63713c54f20cac61b56d"
        )

        val jsonString = json.encodeToString(metadata)
        assertTrue(jsonString.contains("\"id\":\"patch_20260228_120000_alex\""))
        assertTrue(jsonString.contains("\"x\":3"))
        assertTrue(jsonString.contains("\"y\":5"))
        assertTrue(jsonString.contains("\"image_commit_sha\":\"76384fc99e6c6c33514c63713c54f20cac61b56d\""))

        val deserialized = json.decodeFromString<de.berlindroid.zethread.data.model.PatchMetadata>(jsonString)
        assertEquals(metadata.id, deserialized.id)
        assertEquals(3, deserialized.coordinates.x)
        assertEquals(5, deserialized.coordinates.y)
        assertEquals("patch.png", deserialized.image)
        assertEquals("76384fc99e6c6c33514c63713c54f20cac61b56d", deserialized.imageCommitSha)
    }
}
