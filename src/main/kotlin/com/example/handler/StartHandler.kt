package com.example.handler

import com.example.util.DefaultEntity
import com.example.util.TelegramUtil.createMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update


class StartHandler: Handler {

    private val defaultEntity = DefaultEntity()
    override fun handle(update: Update, followingState: String?): List<SendMessage> {
        val chatId = getChatFromUpdate(update)
        val author = getUserEntityFromChatId(chatId)

        val welcomeMessage = createMessage(chatId,
            "Hello! Nice to meet you here. I am bot that will help you to measure your progress in gym.")
        val helpMessage: SendMessage = createMessage(chatId,
            "You can check your exercises with /showex. All exercises are split into groups. You can add a new group of exercises with /addgroup, and an exercise with /addex.")

        defaultEntity.saveInitialGroupsAndExercises(author.id.value)

        return listOf(welcomeMessage, helpMessage)
    }
}