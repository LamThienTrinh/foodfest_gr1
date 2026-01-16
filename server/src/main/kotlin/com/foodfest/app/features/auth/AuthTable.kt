package com.foodfest.app.features.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// =============================================
// TABLE DEFINITION
// =============================================
object AuthTable : IntIdTable("users", "user_id") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 100)
    val avatarUrl = text("avatar_url").nullable()
    val followerCount = integer("follower_count").default(0)
    val followingCount = integer("following_count").default(0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// =============================================
// MODELS
// =============================================
@Serializable
data class User(
    val id: Int,
    val username: String,
    @Transient val passwordHash: String = "",
    val fullName: String,
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: String
)

// =============================================
// REPOSITORY (Database Operations)
// =============================================
class AuthRepository {
    
    suspend fun createUser(
        username: String,
        passwordHash: String,
        fullName: String,
        avatarUrl: String?
    ): User = newSuspendedTransaction(Dispatchers.IO) {
        val userId = AuthTable.insertAndGetId {
            it[AuthTable.username] = username
            it[AuthTable.passwordHash] = passwordHash
            it[AuthTable.fullName] = fullName
            it[AuthTable.avatarUrl] = avatarUrl
        }.value
        
        getUserByIdInternal(userId)!!
    }
    
    suspend fun getUserById(id: Int): User? = newSuspendedTransaction(Dispatchers.IO) {
        getUserByIdInternal(id)
    }
    
    private fun getUserByIdInternal(id: Int): User? {
        return AuthTable.select { AuthTable.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }
    
    suspend fun getUserByUsername(username: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        AuthTable.select { AuthTable.username eq username }
            .map { rowToUser(it) }
            .singleOrNull()
    }
    
    suspend fun userExists(username: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        AuthTable.select { AuthTable.username eq username }
            .count() > 0
    }
    
    suspend fun updateFullName(userId: Int, fullName: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        AuthTable.update({ AuthTable.id eq userId }) {
            it[AuthTable.fullName] = fullName
        } > 0
    }
    
    suspend fun updatePassword(userId: Int, passwordHash: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        AuthTable.update({ AuthTable.id eq userId }) {
            it[AuthTable.passwordHash] = passwordHash
        } > 0
    }
    
    suspend fun updateAvatar(userId: Int, avatarUrl: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        AuthTable.update({ AuthTable.id eq userId }) {
            it[AuthTable.avatarUrl] = avatarUrl
        } > 0
    }
    
    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[AuthTable.id].value,
            username = row[AuthTable.username],
            passwordHash = row[AuthTable.passwordHash],
            fullName = row[AuthTable.fullName],
            avatarUrl = row[AuthTable.avatarUrl],
            followerCount = row[AuthTable.followerCount],
            followingCount = row[AuthTable.followingCount],
            createdAt = row[AuthTable.createdAt].toString()
        )
    }
}
