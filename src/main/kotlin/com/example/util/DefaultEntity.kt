package com.example.util

import com.example.repo.entity.ExerciseEntity
import com.example.repo.entity.GroupEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class DefaultEntity {
  private fun saveGroup(groupName: String, groupAuthorId: Int, saveTime: Instant = Instant.now()): GroupEntity {
      return transaction {
        GroupEntity.new {
          name = groupName.replaceFirstChar(Char::titlecaseChar)
          createdBy = groupAuthorId
          createdAt = saveTime
        }
      }
  }

  private fun saveExercise(entityName: String, grpId: Int, authorId: Int, saveTime: Instant = Instant.now()): ExerciseEntity {
    return transaction {
      ExerciseEntity.new {
        name = entityName.replaceFirstChar(Char::titlecaseChar)
        groupId = grpId
        createdBy = authorId
        createdAt = saveTime
      }
    }
  }

  fun saveInitialGroupsAndExercises(authorId: Int) {
    defaultExercises.forEach { (groupName, exerciseSet) -> {
        val savedGroup = saveGroup(groupName, authorId)
        exerciseSet.forEach {
          saveExercise(it, savedGroup.id.value, authorId)
        }
    }
    }
  }

  companion object DefaultEntities {
    val defaultExercises = mapOf(
      "Anaerobic🏃‍️" to setOf("Running, 100m"),
      "Legs🏋" to setOf("Barbell back squat"),
      "Back⏮‍️" to setOf("Pull-ups"),
      "Abs🗞‍️" to setOf("Sit-ups"),
      "Arms💪‍️" to setOf("Biceps: barbell curl", "Triceps: dips on bar"),
      "Chest🔱‍️" to setOf("Barbell bench press", "Push-ups"),
      "Shoulders🤷‍️" to setOf("Seated dumbbell press")
    )
  }
}