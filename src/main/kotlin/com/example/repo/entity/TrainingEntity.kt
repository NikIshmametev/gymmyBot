package com.example.repo.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TrainingTable : IntIdTable("trainings") {
    val userId = integer("user_id")
    val exerciseId = integer("exercise_id")
    val difficulty = float("difficulty")
    val repeats = integer("repeats")
    val grouped = bool("grouped")
    val createdAt = timestamp("created_at").default(Instant.now())
}

class TrainingEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, TrainingEntity>(TrainingTable)
    var userId by TrainingTable.userId
    var exerciseId by TrainingTable.exerciseId
    var difficulty by TrainingTable.difficulty
    var repeats by TrainingTable.repeats
    var grouped by TrainingTable.grouped
    var createdAt by TrainingTable.createdAt
}
