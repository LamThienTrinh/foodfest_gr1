package com.foodfest.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform