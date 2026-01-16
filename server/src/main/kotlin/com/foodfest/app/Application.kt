package com.foodfest.app

import com.foodfest.app.di.mainModule
import com.foodfest.app.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }
    
    val port = dotenv["SERVER_PORT"]?.toIntOrNull() ?: 8080
    val host = dotenv["SERVER_HOST"] ?: "0.0.0.0"
    
    embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Install Koin for Dependency Injection
    install(Koin) {
        slf4jLogger()
        modules(mainModule)
    }
    
    // Configure all plugins
    configureDatabases()    // Initialize DB and create tables
    configureSerialization() // JSON handling
    configureCORS()         // Cross-Origin Resource Sharing
    configureSecurity()     // JWT Authentication (TODO)
    configureErrorHandling() // Centralized exception -> ApiResponse
    configureMonitoring()   // Request logging
    configureRouting()      // All API routes
}