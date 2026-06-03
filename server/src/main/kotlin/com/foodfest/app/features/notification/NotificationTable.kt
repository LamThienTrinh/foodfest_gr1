package com.foodfest.app.features.notification

import com.foodfest.app.core.exception.AppException
import com.foodfest.app.features.auth.AuthTable
import com.foodfest.app.features.family.FamilyGroupTable
import com.foodfest.app.features.family.FamilyMemberTable
import com.foodfest.app.features.family.FamilyPantryItemTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDate

object NotificationTable : IntIdTable("notifications", "notification_id") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 50)
    val title = varchar("title", 160)
    val message = text("message")
    val relatedEntityType = varchar("related_entity_type", 50).nullable()
    val relatedEntityId = integer("related_entity_id").nullable()
    val actionUrl = varchar("action_url", 255).nullable()
    val isRead = bool("is_read").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object PushDeviceTokenTable : IntIdTable("push_device_tokens", "push_device_token_id") {
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val platform = varchar("platform", 20)
    val token = text("token")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

object NotificationJobRunTable : IntIdTable("notification_job_runs", "notification_job_run_id") {
    val jobName = varchar("job_name", 80)
    val startedAt = timestamp("started_at").defaultExpression(CurrentTimestamp())
    val finishedAt = timestamp("finished_at").nullable()
    val status = varchar("status", 20)
    val insertedCount = integer("inserted_count").default(0)
    val pushAttemptedCount = integer("push_attempted_count").default(0)
    val pushSentCount = integer("push_sent_count").default(0)
    val errorMessage = text("error_message").nullable()
}

object NotificationDeliveryLogTable :
    IntIdTable("notification_delivery_logs", "notification_delivery_log_id") {
    val notificationId = reference("notification_id", NotificationTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", AuthTable, onDelete = ReferenceOption.CASCADE)
    val pushDeviceTokenId = optReference(
        "push_device_token_id",
        PushDeviceTokenTable,
        onDelete = ReferenceOption.SET_NULL
    )
    val provider = varchar("provider", 40)
    val status = varchar("status", 20)
    val responseMessage = text("response_message").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

@Serializable
data class AppNotification(
    val id: Int,
    val userId: Int,
    val type: String,
    val title: String,
    val message: String,
    val relatedEntityType: String? = null,
    val relatedEntityId: Int? = null,
    val actionUrl: String? = null,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class NotificationUnreadCount(
    val unreadCount: Int
)

@Serializable
data class PushDeviceToken(
    val id: Int,
    val userId: Int,
    val platform: String,
    val token: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class PushDeliveryTarget(
    val id: Int,
    val userId: Int,
    val platform: String,
    val token: String
)

data class PantryExpirySyncResult(
    val insertedCount: Int,
    val insertedNotifications: List<AppNotification>
)

data class NotificationJobResult(
    val insertedCount: Int,
    val pushAttemptedCount: Int,
    val pushSentCount: Int
)

class NotificationRepository {
    private fun rowToNotification(row: ResultRow): AppNotification {
        return AppNotification(
            id = row[NotificationTable.id].value,
            userId = row[NotificationTable.userId].value,
            type = row[NotificationTable.type],
            title = row[NotificationTable.title],
            message = row[NotificationTable.message],
            relatedEntityType = row[NotificationTable.relatedEntityType],
            relatedEntityId = row[NotificationTable.relatedEntityId],
            actionUrl = row[NotificationTable.actionUrl],
            isRead = row[NotificationTable.isRead],
            createdAt = row[NotificationTable.createdAt].toString()
        )
    }

    private fun rowToPushDeviceToken(row: ResultRow): PushDeviceToken {
        return PushDeviceToken(
            id = row[PushDeviceTokenTable.id].value,
            userId = row[PushDeviceTokenTable.userId].value,
            platform = row[PushDeviceTokenTable.platform],
            token = row[PushDeviceTokenTable.token],
            isActive = row[PushDeviceTokenTable.isActive],
            createdAt = row[PushDeviceTokenTable.createdAt].toString(),
            updatedAt = row[PushDeviceTokenTable.updatedAt].toString()
        )
    }

    /**
     * Inserts one pantry expiry notification using the DB unique guard as the idempotency boundary.
     */
    private fun insertPantryExpiryNotification(
        userId: Int,
        type: String,
        title: String,
        message: String,
        pantryItemId: Int,
        familyId: Int
    ): AppNotification? {
        val inserted = NotificationTable.insertIgnore {
            it[NotificationTable.userId] = userId
            it[NotificationTable.type] = type
            it[NotificationTable.title] = title
            it[NotificationTable.message] = message
            it[NotificationTable.relatedEntityType] = "family_pantry_item"
            it[NotificationTable.relatedEntityId] = pantryItemId
            it[NotificationTable.actionUrl] = "foodfest://family/pantry/$familyId"
            it[NotificationTable.isRead] = false
        }.insertedCount

        if (inserted == 0) return null

        return NotificationTable.select {
            (NotificationTable.userId eq userId) and
                (NotificationTable.type eq type) and
                (NotificationTable.relatedEntityType eq "family_pantry_item") and
                (NotificationTable.relatedEntityId eq pantryItemId)
        }.singleOrNull()?.let(::rowToNotification)
    }

    /**
     * Marks stale "sắp hết hạn" notifications as read after the same pantry item becomes expired.
     */
    private fun markExpiringNotificationsRead(userIds: List<Int>?, expiredItemIds: List<Int>) {
        if (expiredItemIds.isEmpty()) return

        NotificationTable.update({
            val base = (NotificationTable.type eq "pantry_expiring") and
                (NotificationTable.relatedEntityType eq "family_pantry_item") and
                (NotificationTable.relatedEntityId inList expiredItemIds) and
                (NotificationTable.isRead eq false)

            if (userIds.isNullOrEmpty()) {
                base
            } else {
                base and (NotificationTable.userId inList userIds)
            }
        }) {
            it[NotificationTable.isRead] = true
        }
    }

    suspend fun listForUser(userId: Int, limit: Int = 50): List<AppNotification> =
        newSuspendedTransaction(Dispatchers.IO) {
            NotificationTable
                .select { NotificationTable.userId eq userId }
                .orderBy(NotificationTable.createdAt to SortOrder.DESC)
                .limit(limit.coerceIn(1, 100))
                .map(::rowToNotification)
        }

    suspend fun unreadCount(userId: Int): Int = newSuspendedTransaction(Dispatchers.IO) {
        NotificationTable.select {
            (NotificationTable.userId eq userId) and (NotificationTable.isRead eq false)
        }.count().toInt()
    }

    /**
     * Scans Pantry expiry state for the current user and inserts idempotent inbox notifications.
     */
    suspend fun syncPantryExpiryNotifications(userId: Int): Int = newSuspendedTransaction(Dispatchers.IO) {
        val today = LocalDate.now()
        val warningEnd = today.plusDays(3)
        val familyIds = FamilyMemberTable
            .select { FamilyMemberTable.userId eq userId }
            .map { row -> row[FamilyMemberTable.familyId].value }

        if (familyIds.isEmpty()) {
            return@newSuspendedTransaction 0
        }

        val insertedNotifications = mutableListOf<AppNotification>()

        val pantryRows = (FamilyPantryItemTable innerJoin FamilyGroupTable)
            .select {
                (FamilyPantryItemTable.familyId inList familyIds) and
                    (FamilyPantryItemTable.expiryDate lessEq warningEnd)
            }

        pantryRows.forEach { row ->
            val expiryDate = row[FamilyPantryItemTable.expiryDate] ?: return@forEach
            val pantryItemId = row[FamilyPantryItemTable.id].value
            val ingredientName = row[FamilyPantryItemTable.ingredientName]
            val familyName = row[FamilyGroupTable.name]
            val type = if (expiryDate <= today) "pantry_expired" else "pantry_expiring"
            val title = if (type == "pantry_expired") "Nguyên liệu đã hết hạn" else "Nguyên liệu sắp hết hạn"
            val message = if (type == "pantry_expired") {
                "$ingredientName trong $familyName đã hết hạn ngày $expiryDate"
            } else {
                "$ingredientName trong $familyName sẽ hết hạn ngày $expiryDate"
            }

            insertPantryExpiryNotification(
                userId = userId,
                type = type,
                title = title,
                message = message,
                pantryItemId = pantryItemId,
                familyId = row[FamilyPantryItemTable.familyId].value
            )?.let(insertedNotifications::add)
        }

        // If an item was previously "sắp hết hạn", create one final "đã hết hạn" event when date passes.
        val expiredItemIds = FamilyPantryItemTable.select {
            (FamilyPantryItemTable.familyId inList familyIds) and
                (FamilyPantryItemTable.expiryDate lessEq today)
        }.map { row -> row[FamilyPantryItemTable.id].value }

        markExpiringNotificationsRead(userIds = listOf(userId), expiredItemIds = expiredItemIds)

        insertedNotifications.size
    }

    /**
     * Phase 6 scheduler scan: scans every pantry item and creates notifications for every family member.
     */
    suspend fun syncPantryExpiryNotificationsForAllUsers(): PantryExpirySyncResult =
        newSuspendedTransaction(Dispatchers.IO) {
            val today = LocalDate.now()
            val warningEnd = today.plusDays(3)
            val pantryRows = (FamilyPantryItemTable innerJoin FamilyGroupTable)
                .select { FamilyPantryItemTable.expiryDate lessEq warningEnd }
                .toList()

            if (pantryRows.isEmpty()) {
                return@newSuspendedTransaction PantryExpirySyncResult(0, emptyList())
            }

            val familyIds = pantryRows.map { it[FamilyPantryItemTable.familyId].value }.distinct()
            val membersByFamily = FamilyMemberTable
                .select { FamilyMemberTable.familyId inList familyIds }
                .map { row ->
                    row[FamilyMemberTable.familyId].value to row[FamilyMemberTable.userId].value
                }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })

            val insertedNotifications = mutableListOf<AppNotification>()
            pantryRows.forEach { row ->
                val expiryDate = row[FamilyPantryItemTable.expiryDate] ?: return@forEach
                val familyId = row[FamilyPantryItemTable.familyId].value
                val pantryItemId = row[FamilyPantryItemTable.id].value
                val ingredientName = row[FamilyPantryItemTable.ingredientName]
                val familyName = row[FamilyGroupTable.name]
                val type = if (expiryDate <= today) "pantry_expired" else "pantry_expiring"
                val title = if (type == "pantry_expired") "Nguyên liệu đã hết hạn" else "Nguyên liệu sắp hết hạn"
                val message = if (type == "pantry_expired") {
                    "$ingredientName trong $familyName đã hết hạn ngày $expiryDate"
                } else {
                    "$ingredientName trong $familyName sẽ hết hạn ngày $expiryDate"
                }

                membersByFamily[familyId].orEmpty().forEach { memberUserId ->
                    insertPantryExpiryNotification(
                        userId = memberUserId,
                        type = type,
                        title = title,
                        message = message,
                        pantryItemId = pantryItemId,
                        familyId = familyId
                    )?.let(insertedNotifications::add)
                }
            }

            val expiredItemIds = pantryRows
                .filter { row -> row[FamilyPantryItemTable.expiryDate]?.let { it <= today } == true }
                .map { row -> row[FamilyPantryItemTable.id].value }
                .distinct()
            markExpiringNotificationsRead(userIds = null, expiredItemIds = expiredItemIds)

            PantryExpirySyncResult(
                insertedCount = insertedNotifications.size,
                insertedNotifications = insertedNotifications
            )
        }

    /**
     * Registers or reactivates one push token for a user/device.
     */
    suspend fun registerDeviceToken(userId: Int, platform: String, token: String): PushDeviceToken =
        newSuspendedTransaction(Dispatchers.IO) {
            val existing = PushDeviceTokenTable.select {
                (PushDeviceTokenTable.userId eq userId) and
                    (PushDeviceTokenTable.platform eq platform) and
                    (PushDeviceTokenTable.token eq token)
            }.singleOrNull()

            if (existing == null) {
                val tokenId = PushDeviceTokenTable.insertAndGetId {
                    it[PushDeviceTokenTable.userId] = userId
                    it[PushDeviceTokenTable.platform] = platform
                    it[PushDeviceTokenTable.token] = token
                    it[PushDeviceTokenTable.isActive] = true
                }
                PushDeviceTokenTable.select { PushDeviceTokenTable.id eq tokenId }
                    .single()
                    .let(::rowToPushDeviceToken)
            } else {
                PushDeviceTokenTable.update({ PushDeviceTokenTable.id eq existing[PushDeviceTokenTable.id] }) {
                    it[PushDeviceTokenTable.isActive] = true
                    it[PushDeviceTokenTable.updatedAt] = CurrentTimestamp()
                }
                PushDeviceTokenTable.select { PushDeviceTokenTable.id eq existing[PushDeviceTokenTable.id] }
                    .single()
                    .let(::rowToPushDeviceToken)
            }
        }

    /**
     * Deactivates a push token when the app logs out or the native token is revoked.
     */
    suspend fun deactivateDeviceToken(userId: Int, platform: String, token: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            PushDeviceTokenTable.update({
                (PushDeviceTokenTable.userId eq userId) and
                    (PushDeviceTokenTable.platform eq platform) and
                    (PushDeviceTokenTable.token eq token)
            }) {
                it[PushDeviceTokenTable.isActive] = false
                it[PushDeviceTokenTable.updatedAt] = CurrentTimestamp()
            } > 0
        }

    /**
     * Returns active token targets for push delivery; delivery failures are logged separately.
     */
    suspend fun listActiveDeviceTokens(userId: Int): List<PushDeliveryTarget> =
        newSuspendedTransaction(Dispatchers.IO) {
            PushDeviceTokenTable.select {
                (PushDeviceTokenTable.userId eq userId) and
                    (PushDeviceTokenTable.isActive eq true)
            }.map { row ->
                PushDeliveryTarget(
                    id = row[PushDeviceTokenTable.id].value,
                    userId = row[PushDeviceTokenTable.userId].value,
                    platform = row[PushDeviceTokenTable.platform],
                    token = row[PushDeviceTokenTable.token]
                )
            }
        }

    /**
     * Starts a durable job-run log before running a scheduler task.
     */
    suspend fun startJobRun(jobName: String): Int = newSuspendedTransaction(Dispatchers.IO) {
        NotificationJobRunTable.insertAndGetId {
            it[NotificationJobRunTable.jobName] = jobName
            it[NotificationJobRunTable.status] = "running"
        }.value
    }

    /**
     * Finishes a scheduler job-run log with counts and optional error details.
     */
    suspend fun finishJobRun(
        jobRunId: Int,
        status: String,
        insertedCount: Int,
        pushAttemptedCount: Int,
        pushSentCount: Int,
        errorMessage: String? = null
    ) {
        newSuspendedTransaction(Dispatchers.IO) {
            NotificationJobRunTable.update({ NotificationJobRunTable.id eq jobRunId }) {
                it[NotificationJobRunTable.finishedAt] = CurrentTimestamp()
                it[NotificationJobRunTable.status] = status
                it[NotificationJobRunTable.insertedCount] = insertedCount
                it[NotificationJobRunTable.pushAttemptedCount] = pushAttemptedCount
                it[NotificationJobRunTable.pushSentCount] = pushSentCount
                it[NotificationJobRunTable.errorMessage] = errorMessage?.take(2000)
            }
        }
    }

    /**
     * Logs one push delivery attempt without changing the inbox notification state.
     */
    suspend fun logPushDelivery(
        notificationId: Int,
        userId: Int,
        deviceTokenId: Int?,
        provider: String,
        status: String,
        responseMessage: String?
    ) {
        newSuspendedTransaction(Dispatchers.IO) {
            NotificationDeliveryLogTable.insert {
                it[NotificationDeliveryLogTable.notificationId] = notificationId
                it[NotificationDeliveryLogTable.userId] = userId
                it[NotificationDeliveryLogTable.pushDeviceTokenId] = deviceTokenId
                it[NotificationDeliveryLogTable.provider] = provider
                it[NotificationDeliveryLogTable.status] = status
                it[NotificationDeliveryLogTable.responseMessage] = responseMessage?.take(2000)
            }
        }
    }

    suspend fun markRead(userId: Int, notificationId: Int): AppNotification =
        newSuspendedTransaction(Dispatchers.IO) {
            val updated = NotificationTable.update({
                (NotificationTable.id eq notificationId) and (NotificationTable.userId eq userId)
            }) {
                it[isRead] = true
            }
            if (updated == 0) {
                throw AppException.NotFound("Notification not found")
            }

            NotificationTable.select {
                (NotificationTable.id eq notificationId) and (NotificationTable.userId eq userId)
            }.singleOrNull()?.let(::rowToNotification)
                ?: throw AppException.NotFound("Notification not found")
        }

    suspend fun markAllRead(userId: Int): Int = newSuspendedTransaction(Dispatchers.IO) {
        NotificationTable.update({
            (NotificationTable.userId eq userId) and (NotificationTable.isRead eq false)
        }) {
            it[isRead] = true
        }
    }

    companion object {
        fun insertEventNotification(
            userId: Int,
            type: String,
            title: String,
            message: String,
            relatedEntityType: String? = null,
            relatedEntityId: Int? = null,
            actionUrl: String? = null
        ) {
            NotificationTable.insertIgnore {
                it[NotificationTable.userId] = userId
                it[NotificationTable.type] = type
                it[NotificationTable.title] = title
                it[NotificationTable.message] = message
                it[NotificationTable.relatedEntityType] = relatedEntityType
                it[NotificationTable.relatedEntityId] = relatedEntityId
                it[NotificationTable.actionUrl] = actionUrl
                it[NotificationTable.isRead] = false
            }
        }

        fun markRelatedEventRead(
            userId: Int,
            type: String,
            relatedEntityType: String,
            relatedEntityId: Int
        ) {
            NotificationTable.update({
                (NotificationTable.userId eq userId) and
                    (NotificationTable.type eq type) and
                    (NotificationTable.relatedEntityType eq relatedEntityType) and
                    (NotificationTable.relatedEntityId eq relatedEntityId)
            }) {
                it[isRead] = true
            }
        }
    }
}
