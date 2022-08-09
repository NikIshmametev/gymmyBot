package com.example.repo.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ExerciseTable : IntIdTable("exercises") {
    val name = text("name")
    val groupId = integer("group_id")
    val createdAt = timestamp("created_at").default(Instant.now())
    val createdBy = integer("created_by")
}

class ExerciseEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, ExerciseEntity>(ExerciseTable)
    var name by ExerciseTable.name
    var groupId by ExerciseTable.groupId
    var createdAt by ExerciseTable.createdAt
    var createdBy by ExerciseTable.createdBy
}
