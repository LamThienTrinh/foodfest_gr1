package com.foodfest.app.plugins

import com.foodfest.app.features.auth.AuthRoutes
import com.foodfest.app.features.dish.DishService
import com.foodfest.app.features.dish.dishRoutes
import com.foodfest.app.features.favorite.FavoriteService
import com.foodfest.app.features.favorite.favoriteRoutes
import com.foodfest.app.features.follow.FollowService
import com.foodfest.app.features.follow.followRoutes
import com.foodfest.app.features.personaldish.PersonalDishService
import com.foodfest.app.features.personaldish.personalDishRoutes
import com.foodfest.app.features.post.PostService
import com.foodfest.app.features.post.postRoutes
import com.foodfest.app.features.tag.TagService
import com.foodfest.app.features.tag.tagRoutes
import com.foodfest.app.features.upload.uploadRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.context.GlobalContext

fun Application.configureRouting() {
    // Get services from Koin
    val koin = GlobalContext.get()
    val authService = koin.get<com.foodfest.app.features.auth.AuthService>()
    val tagService = koin.get<TagService>()
    val dishService = koin.get<DishService>()
    val favoriteService = koin.get<FavoriteService>()
    val personalDishService = koin.get<PersonalDishService>()
    val postService = koin.get<PostService>()
    val followService = koin.get<FollowService>()
    
    routing {
        get("/") {
            call.respondText("🍜 FoodFest API is running!")
        }
        
        get("/health") {
            call.respondText("OK")
        }
        
        // Feature routes
        AuthRoutes(authService)
        tagRoutes(tagService)
        dishRoutes(dishService)
        favoriteRoutes(favoriteService)
        personalDishRoutes(personalDishService)
        postRoutes(postService)
        followRoutes(followService)
        uploadRoutes()
    }
}
