package com.foodfest.app.di

import com.foodfest.app.features.auth.AuthRepository
import com.foodfest.app.features.auth.AuthService
import com.foodfest.app.features.dish.DishRepository
import com.foodfest.app.features.dish.DishService
import com.foodfest.app.features.favorite.FavoriteRepository
import com.foodfest.app.features.favorite.FavoriteService
import com.foodfest.app.features.follow.FollowRepository
import com.foodfest.app.features.follow.FollowService
import com.foodfest.app.features.personaldish.PersonalDishRepository
import com.foodfest.app.features.personaldish.PersonalDishService
import com.foodfest.app.features.post.PostRepository
import com.foodfest.app.features.post.PostService
import com.foodfest.app.features.tag.TagRepository
import com.foodfest.app.features.tag.TagService
import org.koin.dsl.module

val mainModule = module {
    // Auth feature
    single { AuthRepository() }
    single { AuthService(get()) }
    
    // Tag feature
    single { TagRepository() }
    single { TagService(get()) }
    
    // Dish feature
    single { DishRepository() }
    single { DishService(get(), get()) }
    
    // Favorite feature
    single { FavoriteRepository() }
    single { FavoriteService(get()) }
    
    // Personal Dish feature
    single { PersonalDishRepository() }
    single { PersonalDishService(get()) }
    
    // Post feature
    single { PostRepository() }
    single { PostService(get()) }
    
    // Follow feature
    single { FollowRepository() }
    single { FollowService(get()) }
}
