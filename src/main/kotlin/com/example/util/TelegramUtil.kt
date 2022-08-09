package com.example.util

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

object TelegramUtil {
    fun createMessage(chatId: String, message: String): SendMessage {
        val messageTemplate = SendMessage(chatId, message)
        messageTemplate.enableMarkdown(true)
        return messageTemplate
    }

    fun createInlineKeyboardButton(chatId: String, message: String, buttonTexts: List<String>, showInOneColumn: Boolean = false): SendMessage {
        val messageToUser = SendMessage(chatId, message)
        var buttons = mutableListOf<MutableList<InlineKeyboardButton>>()

        when (showInOneColumn) {
            false -> {
                val innerList = mutableListOf<InlineKeyboardButton>()
                for (text in buttonTexts) {
                    innerList.add(createKeyboardButton(text))
                }
                // List with one list of many elements
                buttons = mutableListOf(innerList)
            }
            true -> {
                for (text in buttonTexts) {
                    // List of many lists with one element
                    buttons.add(mutableListOf(createKeyboardButton(text)))
                }
            }
        }

        val markupInline = InlineKeyboardMarkup(buttons)
        messageToUser.replyMarkup = markupInline
        return messageToUser
    }

    private fun createKeyboardButton(text: String): InlineKeyboardButton {
        val button = InlineKeyboardButton()
        button.text = text
        button.callbackData = text
        return button
    }
}