package de.berlindroid.zethread

import de.berlindroid.zethread.data.model.CommitAuthorIdent
import de.berlindroid.zethread.data.model.CreateFileRequest
import de.berlindroid.zethread.data.repository.FakeGitHubRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeGitHubRepositoryTest {

    @Test
    fun testFakeGitHubRepositoryDispatch() = runBlocking {
        val fakeRepo = FakeGitHubRepository()
        val request = CreateFileRequest(
            message = "feat(tapestry): Added test patch [by @test_user]",
            content = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            author = CommitAuthorIdent(name = "test_user", email = "test@example.com"),
            committer = CommitAuthorIdent(name = "test_user", email = "test@example.com"),
            branch = "main"
        )

        val result = fakeRepo.createFile(
            owner = "louis993546",
            repo = "ZeThread-testing",
            path = "patches/patch_test.png",
            token = "",
            request = request
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(40, response.commit.sha.length)
        assertEquals("patches/patch_test.png", response.content?.path)

        val recorded = fakeRepo.lastCall.value
        assertNotNull(recorded)
        assertEquals("louis993546", recorded?.owner)
        assertEquals("ZeThread-testing", recorded?.repo)
        assertEquals("patches/patch_test.png", recorded?.path)
        assertEquals("test_user", recorded?.authorName)
        assertEquals("test@example.com", recorded?.authorEmail)
    }

    @Test
    fun testFakeGitHubRepositoryGetUserReturns404() = runBlocking {
        val fakeRepo = FakeGitHubRepository()
        val result = fakeRepo.getUser("torvalds")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("404") == true)
    }
}
