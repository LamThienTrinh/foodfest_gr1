package com.foodfest.app.plugins

import com.foodfest.app.core.database.DatabaseFactory
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.dish.DishTable
import com.foodfest.app.features.dish.DishTagTable
import com.foodfest.app.features.follow.FollowTable
import com.foodfest.app.features.family.FamilyGroupTable
import com.foodfest.app.features.family.FamilyMemberTable
import com.foodfest.app.features.family.FamilyMenuTable
import com.foodfest.app.features.family.FamilyMenuItemTable
import com.foodfest.app.features.family.FamilyMenuVoteTable
import com.foodfest.app.features.family.FamilyInviteTable
import com.foodfest.app.features.family.FamilySavedMealTable
import com.foodfest.app.features.family.FamilySavedMealItemTable
import com.foodfest.app.features.family.FamilyPantryItemTable
import com.foodfest.app.features.family.FamilyShoppingListActivityTable
import com.foodfest.app.features.family.FamilyShoppingListItemTable
import com.foodfest.app.features.family.FamilyShoppingListTable
import com.foodfest.app.features.family.FamilyNoteTable
import com.foodfest.app.features.notification.NotificationDeliveryLogTable
import com.foodfest.app.features.notification.NotificationJobRunTable
import com.foodfest.app.features.notification.NotificationTable
import com.foodfest.app.features.notification.PushDeviceTokenTable
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
        CommentTable,
        FamilyGroupTable,
        FamilyMemberTable,
        FamilyInviteTable,
        FamilyMenuTable,
        FamilyMenuItemTable,
        FamilyMenuVoteTable,
        FamilySavedMealTable,
        FamilySavedMealItemTable,
        FamilyPantryItemTable,
        FamilyShoppingListTable,
        FamilyShoppingListItemTable,
        FamilyShoppingListActivityTable,
        FamilyNoteTable,
        NotificationTable,
        PushDeviceTokenTable,
        NotificationJobRunTable,
        NotificationDeliveryLogTable
    )
}
