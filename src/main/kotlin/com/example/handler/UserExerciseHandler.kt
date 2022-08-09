package com.example.handler

import com.example.model.CacheKey
import com.example.model.State
import com.example.repo.entity.UserExerciseEntity
import com.example.util.TelegramUtil.createInlineKeyboardButton
import com.example.util.TelegramUtil.createMessage
import io.github.reactivecircus.cache4k.Cache
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Instant


class UserExerciseHandler(cache: Cache<CacheKey, String>): Handler {
    private val cacheVar = cache
    override fun handle(update: Update, followingState: String?): List<SendMessage> {

        val now = Instant.now()
        val chatId = getChatFromUpdate(update)
        val author = getUserEntityFromChatId(chatId)

        when (followingState ?: author.state) {
            null -> {
                val existedGroups = getUserGroups(author)
                return if (existedGroups.isNotEmpty()) {
                    val message = createInlineKeyboardButton(chatId, "Choose group of the exercise preferences:", existedGroups, true)
                    transaction { author.setState(State.CHOOSE_USER_EX_GROUP, now) }
                    return listOf(message)
                } else {
                    val message = createMessage(chatId, "You don't have any group of exercises. Add your first group with command /addgroup")
                    listOf(message)
                }
            }
            State.CHOOSE_USER_EX_GROUP.key -> {
                val groupName = update.callbackQuery.data
                val chosenGroupId = getGroupIdFromName(author, groupName)

                val existedExercises = getUserExercisesForGroup(author, chosenGroupId)
                return if (existedExercises.isNotEmpty()) {
                    cacheVar.put(CacheKey(chatId, State.CHOOSE_USER_EX_GROUP.key), chosenGroupId.toString())
                    val message = createInlineKeyboardButton(chatId, "Choose the exercise for preferences:", existedExercises, true)
                    transaction { author.setState(State.CHOOSE_USER_EX, now) }
                    return listOf(message)
                } else {
                    val message = createMessage(chatId, "There is no exercises for group $groupName \uD83D\uDD0D")
                    transaction { author.clearState(now) }
                    listOf(message)
                }
            }
            State.CHOOSE_USER_EX.key -> {
                val chosenExerciseId = getExerciseIdFromName(author, update.callbackQuery.data)
                cacheVar.put(CacheKey(chatId, State.CHOOSE_USER_EX.key), chosenExerciseId.toString())

                val cacheKeyGroup = CacheKey(chatId, State.CHOOSE_USER_EX_GROUP.key)
                cacheVar.invalidate(cacheKeyGroup)

                val message = createMessage(chatId, "Enter normal difficulty (weight/pace) for the exercise:")
                transaction { author.setState(State.ADD_USER_EX_NORMAL_DIFFICULTY, now) }
                return listOf(message)
            }
            State.ADD_USER_EX_NORMAL_DIFFICULTY.key -> {
                val cacheKeyExercise = CacheKey(chatId, State.CHOOSE_USER_EX.key)
                try {
                    val normalDifficulty = update.message.text.toFloat()
                    val exerciseId = cacheVar.get(cacheKeyExercise)?.toInt()
                    if (exerciseId == null) {
                        transaction { author.clearState(now) }
                        cacheVar.invalidate(cacheKeyExercise)
                        return listOf(createMessage(chatId, "Oops, something went wrong. Try again later \uD83D\uDE22"))
                    }
                    saveExercisePreference(exerciseId, author.id.value, normalDifficulty)
                }
                catch (e: NumberFormatException) {
                    val message = createMessage(chatId, "Couldn't understand your difficulty: ${update.message.text} \uD83D\uDE22. Try to enter number again.")
                    return listOf(message)
                }

                val message = createMessage(chatId, "Preference for exercise successfully added \uD83C\uDF89")
                transaction { author.clearState(now) }
                cacheVar.invalidate(cacheKeyExercise)
                return listOf(message)
            }
            else -> return emptyList()
        }
    }

    private fun saveExercisePreference(exId: Int, authorId: Int, difficulty: Float, saveTime: Instant = Instant.now()) {
        transaction {
            UserExerciseEntity.new {
                userId = authorId
                exerciseId = exId
                normalDifficulty = difficulty
                createdAt = saveTime
            }
        }
    }
}