package com.foodfest.app.plugins

import com.foodfest.app.core.database.DatabaseFactory
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import com.foodfest.app.features.dish.DishTagTable
import com.foodfest.app.features.follow.FollowTable
import com.foodfest.app.features.post.PostTable
import com.foodfest.app.features.post.SavedPostTable
import com.foodfest.app.features.post.PostLikeTable
import com.foodfest.app.features.post.CommentTable
import com.foodfest.app.features.tag.TagTable
import io.ktor.server.application.*

fun Application.configureDatabases() {
    DatabaseFactory.init()
    DatabaseFactory.createTables(
        AuthTable,
        TagTable,
        DishTable,
        DishTagTable,
        FollowTable,
        PostTable,
        SavedPostTable,
        PostLikeTable,
        CommentTable
    )
}
