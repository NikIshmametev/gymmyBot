package com.example.app

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exposedLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


@Component
class Bot(
    private val updateReceiver: UpdateReceiver
) : TelegramLongPollingBot() {
    @Value("\${bot.username}")
    private val botUsername: String? = null

    @Value("\${bot.token}")
    private val botToken: String? = null

    @Value("\${db.user}")
    private val dbUser: String? = null

    @Value("\${db.password}")
    private val dbPassword: String? = null

    override fun onUpdateReceived(update: Update) {
        Database.connect("jdbc:postgresql://postgres:5432/", user=dbUser!!, password=dbPassword!!)

        val messagesToSend = updateReceiver.handle(update)

        if (messagesToSend.isNotEmpty()) {
            messagesToSend.forEach {
                when (it) {
                    is SendMessage -> executeWithExceptionCheck(it)
                    else -> println("Error")
                }
            }
        }
    }

    fun executeWithExceptionCheck(sendMessage: SendMessage?) {
        try {
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            exposedLogger.info(e.toString())
        }
    }

    override fun getBotUsername(): String {
        return botUsername!!
    }

    override fun getBotToken(): String {
        return botToken!!
    }
}