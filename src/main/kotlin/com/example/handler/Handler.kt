package com.example.handler

import com.example.repo.entity.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

interface Handler {
    fun handle(update: Update, followingState: String?): List<SendMessage>

    fun getChatFromUpdate(update: Update): String {
        return if (!update.hasCallbackQuery() && update.hasMessage() && update.message.hasText()) {
            update.message.from.id.toString()
        } else {
            update.callbackQuery.from.id.toString()
        }
    }

    fun getUserEntityFromChatId(chatId: String): UserEntity {
        return transaction { UserEntity.find { UserTable.telegramId eq chatId.toLong() }.first() }
    }

    fun getUserGroups(userEntity: UserEntity): List<String> {
        return transaction { GroupEntity.find{ GroupTable.createdBy eq userEntity.id.value }.map { it.name } }
    }

    fun getGroupIdFromName(userEntity: UserEntity, name: String): Int {
        return transaction {
            GroupEntity.find{
                (GroupTable.createdBy eq userEntity.id.value) and (GroupTable.name eq name)
            }.first() }.id.value
    }

    fun getExerciseIdFromName(userEntity: UserEntity, name: String): Int {
        return transaction {
            ExerciseEntity.find{
                (ExerciseTable.createdBy eq userEntity.id.value) and (ExerciseTable.name eq name)
            }.first() }.id.value
    }

    fun getUserExercisesForGroup(userEntity: UserEntity, groupId: Int): List<String> {
        return transaction {
            ExerciseEntity.find{
                (ExerciseTable.createdBy eq userEntity.id.value) and (ExerciseTable.groupId eq groupId)
            }.map { it.name }
        }
    }
}