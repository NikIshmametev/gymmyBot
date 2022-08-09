package com.example.repo.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object UserExerciseTable : IntIdTable("user_exercises") {
    val userId = integer("user_id")
    val exerciseId = integer("exercise_id")
    val normalDifficulty = float("normal_difficulty")
    val createdAt = timestamp("created_at").default(Instant.now())
}

class UserExerciseEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, UserExerciseEntity>(UserExerciseTable)
    var userId by UserExerciseTable.userId
    var exerciseId by UserExerciseTable.exerciseId
    var normalDifficulty by UserExerciseTable.normalDifficulty
    var createdAt by UserExerciseTable.createdAt
}
