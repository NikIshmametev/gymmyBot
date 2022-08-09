package com.example.handler

import com.example.model.State
import com.example.repo.entity.GroupEntity
import com.example.repo.entity.GroupTable
import com.example.repo.entity.UserEntity
import com.example.util.TelegramUtil.createMessage
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Instant


class GroupHandler: Handler {
    override fun handle(update: Update, followingState: String?): List<SendMessage> {
        val now = Instant.now()
        val chatId = getChatFromUpdate(update)
        val author = getUserEntityFromChatId(chatId)

        when (followingState ?: author.state) {
            null -> {
                val message = createMessage(chatId, "Enter name of the new group:")
                transaction { author.setState(State.ADD_GROUP_NAME, now) }
                return listOf(message)
            }
            State.ADD_GROUP_NAME.key -> {
                val groupName = update.message.text.lowercase().replaceFirstChar(Char::titlecaseChar)
                val message = when (groupAlreadyExist(author, groupName)) {
                    true -> createMessage(chatId, "Group: '$groupName' already exist ❌")
                    false -> {
                        saveGroup(groupName, author.id.value, now)
                        createMessage(chatId, "Group: '$groupName' was successfully added \uD83C\uDF89")
                    }
                }
                transaction { author.clearState(now) }
                return listOf(message)
            }
            else -> return emptyList()
        }
    }

    private fun groupAlreadyExist(userEntity: UserEntity, groupName: String): Boolean {
        val group = transaction { GroupEntity.find {
            (GroupTable.createdBy eq userEntity.id.value) and (GroupTable.name eq groupName)
        }.firstOrNull() }
        return group != null
    }

    private fun saveGroup(groupName: String, groupAuthorId: Int, saveTime: Instant = Instant.now()) {
        transaction {
            GroupEntity.new {
                name = groupName.replaceFirstChar(Char::titlecaseChar)
                createdBy = groupAuthorId
                createdAt = saveTime
            }
        }
    }
}