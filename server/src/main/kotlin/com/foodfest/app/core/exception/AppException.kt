package com.foodfest.app.core.exception

import io.ktor.http.HttpStatusCode

sealed class AppException(
    override val message: String,
    val status: HttpStatusCode
) : RuntimeException(message) {
    class Validation(message: String) : AppException(message, HttpStatusCode.BadRequest)
    class NotFound(message: String) : AppException(message, HttpStatusCode.NotFound)
    class Unauthorized(message: String = "Unauthorized") : AppException(message, HttpStatusCode.Unauthorized)
    class Forbidden(message: String = "Forbidden") : AppException(message, HttpStatusCode.Forbidden)
    class Conflict(message: String) : AppException(message, HttpStatusCode.Conflict)
    class Internal(message: String = "Internal server error") : AppException(message, HttpStatusCode.InternalServerError)
}

fun Throwable.toAppStatus(): HttpStatusCode = when (this) {
    is AppException -> this.status
    is IllegalArgumentException -> HttpStatusCode.BadRequest
    else -> HttpStatusCode.InternalServerError
}
