package com.foodfest.app.core.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private val dotenv = dotenv {
        directory = "./server"
        ignoreIfMissing = true
    }
    
    fun init() {
            val url = dotenv["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/foodfest"
            val user = dotenv["DATABASE_USER"] ?: "postgres"
            val password = dotenv["DATABASE_PASSWORD"] ?: "postgres"
            val maxPoolSize = dotenv["DATABASE_MAX_POOL_SIZE"]?.toIntOrNull() ?: 10

        Database.connect(hikari(url, user, password, maxPoolSize))
    }
    
    private fun hikari(
        url: String,
        user: String,
        password: String,
        maxPoolSize: Int
    ): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
    
    fun createTables(vararg tables: Table) {
        transaction {
            SchemaUtils.create(*tables)
        }
    }
}
