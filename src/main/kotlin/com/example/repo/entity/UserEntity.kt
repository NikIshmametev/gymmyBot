package com.example.repo.entity

import com.example.model.State
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object UserTable : IntIdTable("users") {
    val telegramId: Column<Long> = long("telegram_id")
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val lastActionAt: Column<Instant> = timestamp("last_action_at").default(Instant.now())
    val state: Column<String?> = text("state").nullable()
}

class UserEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, UserEntity>(UserTable)
    var telegramId by UserTable.telegramId
    var createdAt by UserTable.createdAt
    var lastActionAt by UserTable.lastActionAt
    var state by UserTable.state

    fun clearState(updateTime: Instant = Instant.now()) {
        state = null
        lastActionAt = updateTime
    }

    fun setState(newState: State, updateTime: Instant = Instant.now()) {
        state = newState.key
        lastActionAt = updateTime
    }
}
