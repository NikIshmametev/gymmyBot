package com.example.handler

import com.example.model.CacheKey
import com.example.model.State
import com.example.repo.entity.ExerciseEntity
import com.example.repo.entity.ExerciseTable
import com.example.repo.entity.UserExerciseEntity
import com.example.repo.entity.UserExerciseTable
import com.example.util.TelegramUtil.createInlineKeyboardButton
import com.example.util.TelegramUtil.createMessage
import io.github.reactivecircus.cache4k.Cache
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Instant


class ExerciseHandler(cache: Cache<CacheKey, String>): Handler {
    private val cacheVar = cache
    override fun handle(update: Update, followingState: String?): List<SendMessage> {

        val now = Instant.now()
        val chatId = getChatFromUpdate(update)
        val author = getUserEntityFromChatId(chatId)

        when (followingState ?: author.state) {
            null -> {
                val existedGroups = getUserGroups(author)
                return if (existedGroups.isNotEmpty()) {
                    val message = createInlineKeyboardButton(chatId, "Choose group of the new exercise:", existedGroups, true)
                    transaction { author.setState(State.ADD_EX_GROUP, now) }
                    listOf(message)
                } else {
                    val message = createMessage(chatId, "You don't have any group of exercises. Add your first group with command /addgroup")
                    listOf(message)
                }
            }
            State.ADD_EX_GROUP.key -> {
                val chosenGroupId = getGroupIdFromName(author, update.callbackQuery.data)
                cacheVar.put(CacheKey(chatId, State.ADD_EX_GROUP.key), chosenGroupId.toString())

                val message = createMessage(chatId, "Enter name of the new exercise:")
                transaction { author.setState(State.ADD_EX_NAME, now) }
                return listOf(message)
            }
            State.ADD_EX_NAME.key -> {
                val cacheKey = CacheKey(chatId, State.ADD_EX_GROUP.key)
                val exerciseName = update.message.text.lowercase().replaceFirstChar(Char::titlecaseChar)
                val exerciseGroupId = cacheVar.get(cacheKey)?.toInt()
                exposedLogger.info("Loaded data from cache: $exerciseGroupId, key=$cacheKey")
                exposedLogger.info("Exercise name: $exerciseName")
                val groupsWithExerciseWithSameName = transaction { ExerciseEntity.find { (ExerciseTable.name eq exerciseName) }.map { it.groupId } }

                if (exerciseGroupId == null) {
                    return listOf(createMessage(chatId, "Oops, something went wrong. Try again later \uD83D\uDE22"))
                }

                exposedLogger.info("Current exercise group id: $exerciseGroupId, existed groups with the same name exercises: $groupsWithExerciseWithSameName")
                val message = if (!groupsWithExerciseWithSameName.contains(exerciseGroupId)) {
                    saveExercise(exerciseName, exerciseGroupId, author.id.value, now)
                    createMessage(chatId, "Exercise: '$exerciseName' was successfully added \uD83C\uDF89")
                } else {
                    createMessage(chatId, "Exercise: '$exerciseName' already exist ❌")
                }
                transaction { author.clearState(now) }
                cacheVar.invalidate(cacheKey)
                return listOf(message)
            }

            State.SHOW_GROUPS.key -> {
                val existedGroups = getUserGroups(author)
                return if (existedGroups.isNotEmpty()) {
                    val message = createInlineKeyboardButton(chatId, "Existed groups:", existedGroups, true)
                    transaction { author.setState(State.SHOW_EXERCISES, now) }
                    return listOf(message)
                } else {
                    val message = createMessage(chatId, "You don't have any group of exercises. Add your first group with command /addgroup")
                    listOf(message)
                }
            }
            State.SHOW_EXERCISES.key -> {
                val groupName = update.callbackQuery.data
                val chosenGroupId = getGroupIdFromName(author, groupName)
                val existedExercises = getUserExercisesForGroup(author, chosenGroupId)
                return if (existedExercises.isNotEmpty()) {
                    val message = createInlineKeyboardButton(chatId, "Existed exercises for '$groupName':", existedExercises, true)
                    transaction { author.setState(State.SHOW_DIFFICULTY, now) }
                    listOf(message)
                } else {
                    val message = createMessage(chatId, "There is no exercises for group '$groupName''\uD83D\uDDD1️")
                    transaction { author.clearState(now) }
                    listOf(message)
                }
            }
            State.SHOW_DIFFICULTY.key -> {
                val exerciseName = update.callbackQuery.data
                val chosenExerciseId = getExerciseIdFromName(author, exerciseName)
                val userExercise = transaction {
                    UserExerciseEntity.find{
                        (UserExerciseTable.userId eq author.id.value) and (UserExerciseTable.exerciseId eq chosenExerciseId)
                    }.firstOrNull()
                }
                val message = if (userExercise == null) {
                    createMessage(chatId, "Difficulty for the '$exerciseName' was not set ⚙️")
                } else
                    createMessage(chatId, "Difficulty for the '$exerciseName': ${userExercise.normalDifficulty}")
                transaction { author.clearState(now) }
                return listOf(message)
            }
            else -> return emptyList()
        }
    }

    private fun saveExercise(entityName: String, grpId: Int, authorId: Int, saveTime: Instant = Instant.now()) {
        transaction {
            ExerciseEntity.new {
                name = entityName.replaceFirstChar(Char::titlecaseChar)
                groupId = grpId
                createdBy = authorId
                createdAt = saveTime
            }
        }
    }
}