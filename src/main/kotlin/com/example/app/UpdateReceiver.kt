package com.example.app

import com.example.handler.*
import com.example.model.CacheKey
import com.example.model.State
import com.example.repo.entity.UserEntity
import com.example.repo.entity.UserTable.telegramId
import com.example.util.TelegramUtil
import io.github.reactivecircus.cache4k.Cache
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable
import java.time.Instant
import kotlin.system.exitProcess

@Component
class UpdateReceiver {
    private val cache = Cache.Builder().build<CacheKey, String>()

    private val startHandler = StartHandler()
    private val groupHandler = GroupHandler()
    private val exerciseHandler = ExerciseHandler(cache)
    private val userExerciseHandler = UserExerciseHandler(cache)
    private val trainingHandler = TrainingHandler(cache)

    fun handle(update: Update): List<PartialBotApiMethod<out Serializable?>> {
        return try {
            // Проверяем, если Update - сообщение с текстом
            if (isMessageWithText(update)) {
                val userId = update.message.from.id

                val user = transaction {
                    var userCheck = UserEntity.find { telegramId eq userId }.firstOrNull()
                    if (userCheck == null) {
                        userCheck = UserEntity.new { telegramId=userId }
                        exposedLogger.info("New user $userCheck saved in user table")
                    }
                    userCheck
                }

                return handleMessage(user, update)
            } else if (update.hasCallbackQuery()) {
                val callbackQuery = update.callbackQuery
                val user = transaction { UserEntity.find { telegramId eq callbackQuery.from.id }.first() }
                return handleByCallBackQuery(user, update)
            }

            throw UnsupportedOperationException()
        } catch (e: UnsupportedOperationException) {
            throw UnsupportedOperationException()
        }
    }

    fun handleMessage(user: UserEntity, update: Update): List<SendMessage> {
        exposedLogger.info("Received message: ${update.message.text}")
        exposedLogger.info("User state: ${user.state}")
        try {
            return when (user.state) {
                null -> handleByText(user, update)
                State.ADD_GROUP_NAME.key -> groupHandler.handle(update, null)
                State.ADD_EX_NAME.key -> exerciseHandler.handle(update, null)
                State.ADD_USER_EX_NORMAL_DIFFICULTY.key -> userExerciseHandler.handle(update, null)
                State.SET_TRAINING_DIFFICULTY.key -> trainingHandler.handle(update, null)
                State.SET_TRAINING_REPEATS.key -> trainingHandler.handle(update, null)
                else -> throw NotImplementedError()
            }
        }
        catch (e: NotImplementedError) {
            exposedLogger.error(e.stackTraceToString())
            exposedLogger.warn("Clear state for: $user")
            transaction { user.clearState(Instant.now()) }

            return listOf(
                TelegramUtil.createMessage(
                    update.message.from.id.toString(),
                    "Sorry, don't understand you \uD83D\uDE22"
                )
            )
        }
    }

    fun handleByText(user: UserEntity, update: Update): List<SendMessage> {
        return when (update.message.text) {
            "/start" -> startHandler.handle(update, null)
            "/addgroup" -> groupHandler.handle(update, null)
            "/addex" -> exerciseHandler.handle(update, null)
            "/setex" -> userExerciseHandler.handle(update, null)
            "/tr" -> trainingHandler.handle(update, null)
            "/showex" -> exerciseHandler.handle(update, State.SHOW_GROUPS.key)
            else -> throw NotImplementedError()
        }
    }

    fun handleByCallBackQuery(user: UserEntity, update: Update): List<SendMessage> {
        exposedLogger.info("User state during callback: ${user.state}")
        return try {
            when (user.state) {
                State.ADD_EX_GROUP.key -> exerciseHandler.handle(update, null)
                State.CHOOSE_USER_EX.key -> userExerciseHandler.handle(update, null)
                State.CHOOSE_USER_EX_GROUP.key -> userExerciseHandler.handle(update, null)
                State.CHOOSE_TRAINING_GROUP.key -> trainingHandler.handle(update, null)
                State.CHOOSE_TRAINING_EX.key -> trainingHandler.handle(update, null)
                State.SET_TRAINING_GROUPED.key -> trainingHandler.handle(update, null)
                State.SHOW_EXERCISES.key -> exerciseHandler.handle(update, null)
                State.SHOW_DIFFICULTY.key -> exerciseHandler.handle(update, null)
                else -> throw NotImplementedError()
            }
        } catch (e: NotImplementedError) {
            exposedLogger.error(e.stackTraceToString())
            listOf(
                TelegramUtil.createMessage(
                    update.message.from.id.toString(),
                    "Sorry, don't understand you \uD83D\uDE22"
                )
            )
            exitProcess(0)
        }
    }

    private fun isMessageWithText(update: Update): Boolean {
        return !update.hasCallbackQuery() && update.hasMessage() && update.message.hasText()
    }
}