package com.aiapkbuilder.app.util

import org.junit.Assert.*
import org.junit.Test

class ErrorHandlingTest {

    @Test
    fun `ApiError has correct structure`() {
        val error = AppError.ApiError("Not found", code = 404)
        assertEquals("Not found", error.message)
        assertEquals(404, error.code)
        assertNull(error.cause)
    }

    @Test
    fun `DatabaseError preserves message`() {
        val cause = RuntimeException("DB failure")
        val error = AppError.DatabaseError("Query failed", cause)
        assertEquals("Query failed", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `AiProviderError includes provider name`() {
        val error = AppError.AiProviderError("Rate limited", provider = "OpenAI")
        assertEquals("Rate limited", error.message)
        assertEquals("OpenAI", error.provider)
    }

    @Test
    fun `BuildError includes job id and logs`() {
        val error = AppError.BuildError("Build failed", buildJobId = "j1", logs = "error log")
        assertEquals("Build failed", error.message)
        assertEquals("j1", error.buildJobId)
        assertEquals("error log", error.logs)
    }

    @Test
    fun `FileError includes file path`() {
        val error = AppError.FileError("File not found", filePath = "/tmp/test")
        assertEquals("File not found", error.message)
        assertEquals("/tmp/test", error.filePath)
    }

    @Test
    fun `ValidationError includes field name`() {
        val error = AppError.ValidationError("Required", field = "email")
        assertEquals("Required", error.message)
        assertEquals("email", error.field)
    }

    @Test
    fun `UnknownError handles generic exceptions`() {
        val cause = Exception("Something went wrong")
        val error = AppError.UnknownError("Unknown", cause)
        assertEquals("Unknown", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `toAppError wraps non-AppError exceptions`() {
        val exception = RuntimeException("test error")
        val error = exception.toAppError()
        assertTrue(error is AppError.UnknownError)
        assertEquals("test error", error.message)
    }

    @Test
    fun `toAppError preserves AppError type`() {
        val original = AppError.ApiError("API error")
        val error = original.toAppError()
        assertTrue(error is AppError.ApiError)
    }

    @Test
    fun `safeExecute returns success for valid blocks`() {
        val result = safeExecute { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `safeExecute returns failure for exceptions`() {
        val result = safeExecute { throw RuntimeException("fail") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError)
    }
}
