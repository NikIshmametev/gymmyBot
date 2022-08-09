package com.example.handler

import com.example.model.CacheKey
import com.example.model.State
import com.example.repo.entity.TrainingEntity
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


class TrainingHandler(cache: Cache<CacheKey, String>): Handler {
    private val cacheVar = cache
    override fun handle(update: Update, followingState: String?): List<SendMessage> {

        val now = Instant.now()
        val chatId = getChatFromUpdate(update)
        val author = getUserEntityFromChatId(chatId)

        when (followingState ?: author.state) {
            null -> {
                val existedGroups = getUserGroups(author)
                return if (existedGroups.isNotEmpty()) {
                    val message = createInlineKeyboardButton(chatId, "Choose group of the exercises:", existedGroups, true)
                    transaction { author.setState(State.CHOOSE_TRAINING_GROUP, now) }
                    return listOf(message)
                } else {
                    val message = createMessage(chatId, "You don't have any group of exercises. Add your first group with command /addgroup")
                    listOf(message)
                }
            }
            State.CHOOSE_TRAINING_GROUP.key -> {
                val groupName = update.callbackQuery.data
                val chosenGroupId = getGroupIdFromName(author, groupName)

                val existedExercises = getUserExercisesForGroup(author, chosenGroupId)
                return if (existedExercises.isNotEmpty()) {
                    cacheVar.put(CacheKey(chatId, State.CHOOSE_TRAINING_GROUP.key), chosenGroupId.toString())
                    val message = createInlineKeyboardButton(chatId, "Choose the exercise:", existedExercises, true)
                    transaction { author.setState(State.CHOOSE_TRAINING_EX, now) }
                    return listOf(message)
                } else {
                    val message = createMessage(chatId, "There is no exercises for group '$groupName''\uD83D\uDDD1️")
                    transaction { author.clearState(now) }
                    listOf(message)
                }
            }
            State.CHOOSE_TRAINING_EX.key -> {
                val chosenExerciseId = getExerciseIdFromName(author, update.callbackQuery.data)
                cacheVar.put(CacheKey(chatId, State.CHOOSE_TRAINING_EX.key), chosenExerciseId.toString())

                val cacheKeyOld = CacheKey(chatId, State.CHOOSE_TRAINING_GROUP.key)
                cacheVar.invalidate(cacheKeyOld)

                val normalDifficulty = transaction {
                    UserExerciseEntity.find{
                        (UserExerciseTable.userId eq author.id.value) and (UserExerciseTable.exerciseId eq chosenExerciseId)
                    }.first() }.normalDifficulty

                val message = createMessage(chatId, "Enter difficulty (weight/pace) of the exercise (normal: $normalDifficulty):")
                transaction { author.setState(State.SET_TRAINING_DIFFICULTY, now) }
                return listOf(message)
            }
            State.SET_TRAINING_DIFFICULTY.key -> {
                return try {
                    val difficulty = update.message.text.toFloat()
                    cacheVar.put(CacheKey(chatId, State.SET_TRAINING_DIFFICULTY.key), difficulty.toString())

                    val message = createMessage(chatId, "Enter number of exercise repeats:")
                    transaction { author.setState(State.SET_TRAINING_REPEATS, now) }
                    listOf(message)
                } catch (e: NumberFormatException) {
                    val message = createMessage(chatId, "Couldn't understand your difficulty: ${update.message.text} \uD83D\uDE22. Try to enter number again.")
                    listOf(message)
                }
            }
            State.SET_TRAINING_REPEATS.key -> {
                return try {
                    val repeats = update.message.text.toFloat()
                    cacheVar.put(CacheKey(chatId, State.SET_TRAINING_REPEATS.key), repeats.toString())

                    val message = createInlineKeyboardButton(chatId, "Is it part of the complex:", listOf(true.toString(), false.toString()), false)
                    transaction { author.setState(State.SET_TRAINING_GROUPED, now) }
                    listOf(message)
                } catch (e: NumberFormatException) {
                    val message = createMessage(chatId, "Couldn't understand number of repeats: ${update.message.text} \uD83D\uDE22. Try to enter number again.")
                    listOf(message)
                }
            }
            State.SET_TRAINING_GROUPED.key -> {
                val wasGrouped = update.callbackQuery.data.toBoolean()
                val cacheKeyExercise = CacheKey(chatId, State.CHOOSE_TRAINING_EX.key)
                val exerciseId = cacheVar.get(cacheKeyExercise)?.toInt()

                val cacheKeyDifficulty = CacheKey(chatId, State.SET_TRAINING_DIFFICULTY.key)
                val difficulty = cacheVar.get(cacheKeyDifficulty)?.toFloat()

                val cacheKeyRepeats = CacheKey(chatId, State.SET_TRAINING_REPEATS.key)
                val repeats = cacheVar.get(cacheKeyRepeats)?.toInt()

                var message = createMessage(chatId, "Preference for exercise successfully added \uD83C\uDF89")

                if (exerciseId == null || difficulty == null || repeats == null) {
                    exposedLogger.error("One of the training values is null. exerciseId: $exerciseId, difficulty: $difficulty, repeats: $repeats")
                    message = createMessage(chatId, "Oops, something went wrong. Try again later \uD83D\uDE22")
                }
                else {
                    saveTraining(author.id.value, exerciseId, difficulty, repeats, wasGrouped)
                }

                transaction { author.clearState(now) }
                cacheVar.invalidate(cacheKeyExercise)
                cacheVar.invalidate(cacheKeyDifficulty)
                cacheVar.invalidate(cacheKeyRepeats)
                return listOf(message)
            }
            else -> return emptyList()
        }
    }

    private fun saveTraining(authorId: Int, exId: Int, diff: Float, repeatsNumber: Int, wasGrouped: Boolean, saveTime: Instant = Instant.now()) {
        transaction {
            TrainingEntity.new {
                userId = authorId
                exerciseId = exId
                difficulty = diff
                repeats = repeatsNumber
                grouped = wasGrouped
                createdAt = saveTime
            }
        }
    }
}