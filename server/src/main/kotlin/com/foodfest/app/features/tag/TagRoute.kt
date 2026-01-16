package com.foodfest.app.features.tag

import com.foodfest.app.core.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tagRoutes(tagService: TagService) {
    route("/api/tags") {
        get {
            val type = call.request.queryParameters["type"]
            val tags = tagService.getTags(type)
            call.respond(HttpStatusCode.OK, ApiResponse.success(tags))
        }
    }
}
