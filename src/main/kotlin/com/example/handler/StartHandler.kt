package com.example.handler

import com.example.util.TelegramUtil.createMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update


class StartHandler: Handler {
    override fun handle(update: Update, followingState: String?): List<SendMessage> {
        val chatId = getChatFromUpdate(update)
        val welcomeMessage = createMessage(chatId,
            "Hello! Nice to meet you here. I am bot that will help you to measure your progress in gym.")
        val helpMessage: SendMessage = createMessage(chatId,
            "I have already added some exercises for you. You always can add (remove) the ones with commands /add (/remove).")
        return listOf(welcomeMessage, helpMessage)
    }
}