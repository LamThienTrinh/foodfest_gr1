package com.foodfest.app.navigation

import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.blindbox
import foodfest.composeapp.generated.resources.home
import foodfest.composeapp.generated.resources.mon_an
import foodfest.composeapp.generated.resources.profile
import org.jetbrains.compose.resources.DrawableResource

enum class MainTab { Home, Dish, BlindBox, Profile }

data class NavItem(
    val tab: MainTab,
    val title: String,
    val iconRes: DrawableResource
)

val mainNavItems = listOf(
    NavItem(MainTab.Home, "Trang chủ", Res.drawable.home),
    NavItem(MainTab.Dish, "Món ăn", Res.drawable.mon_an),
    NavItem(MainTab.BlindBox, "BlindBox", Res.drawable.blindbox),
    NavItem(MainTab.Profile, "Hồ sơ", Res.drawable.profile)
)
