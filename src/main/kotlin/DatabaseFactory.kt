package com.garun

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:sqlite:leads.db",
            driver = "org.sqlite.JDBC"
        )
        transaction {
            SchemaUtils.create(Leads)
        }
    }
}
