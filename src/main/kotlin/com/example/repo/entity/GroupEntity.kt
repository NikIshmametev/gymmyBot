package com.example.repo.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object GroupTable : IntIdTable("groups") {
    val name = text("name")
    val createdAt = timestamp("created_at").default(Instant.now())
    val createdBy = integer("created_by")
}

class GroupEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, GroupEntity>(GroupTable)
    var name by GroupTable.name
    var createdAt by GroupTable.createdAt
    var createdBy by GroupTable.createdBy
}
