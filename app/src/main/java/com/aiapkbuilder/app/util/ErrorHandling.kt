package com.aiapkbuilder.app.util

/**
 * Sealed class for representing application errors.
 * Provides type-safe error handling across the app.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Network/API related errors
     */
    data class ApiError(
        override val message: String,
        val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Database/Local storage errors
     */
    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * AI Provider/Model errors
     */
    data class AiProviderError(
        override val message: String,
        val provider: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Build system errors
     */
    data class BuildError(
        override val message: String,
        val buildJobId: String? = null,
        val logs: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * File system/IO errors
     */
    data class FileError(
        override val message: String,
        val filePath: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Configuration/Validation errors
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * General/Unknown errors
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}

/**
 * Extension function to convert exceptions to AppError
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    else -> AppError.UnknownError(
        message = this.message ?: "Unknown error occurred",
        cause = this
    )
}

/**
 * Result extension for safe error handling
 */
inline fun <T> safeExecute(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.failure(e.toAppError())
}

/**
 * Async result wrapper for coroutines
 */
inline suspend fun <T> safeExecuteAsync(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.failure(e.toAppError())
}
