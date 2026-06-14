package com.garun

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

object Leads : Table("leads") {
    val id = integer("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val company = varchar("company", 255)
    val email = varchar("email", 255)
    val phone = varchar("phone", 50)
    val telegram = varchar("telegram", 100).nullable()
    val description = text("description")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class LeadRequest(
    val fullName: String,
    val company: String,
    val email: String,
    val phone: String,
    val telegram: String? = null,
    val description: String
)

@Serializable
data class LeadCreatedResponse(val id: Int, val message: String)

@Serializable
data class LeadResponse(
    val id: Int,
    val fullName: String,
    val company: String,
    val email: String,
    val phone: String,
    val telegram: String?,
    val description: String,
    val createdAt: Long
)
