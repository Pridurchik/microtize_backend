package com.garun

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.Date

fun Application.configureRouting() {
    routing {

        // ── POST /api/leads — принять заявку ──────────────────────────────────
        post("/api/leads") {
            val req = call.receive<LeadRequest>()

            if (req.fullName.isBlank() || req.company.isBlank() ||
                req.email.isBlank() || req.phone.isBlank() || req.description.isBlank()
            ) {
                call.respond(HttpStatusCode.BadRequest, mapOf<String, String>("error" to "Все обязательные поля должны быть заполнены"))
                return@post
            }

            val newId = transaction {
                Leads.insert {
                    it[fullName] = req.fullName.trim()
                    it[company] = req.company.trim()
                    it[email] = req.email.trim()
                    it[phone] = req.phone.trim()
                    it[telegram] = req.telegram?.trim()?.takeIf { t -> t.isNotBlank() }
                    it[description] = req.description.trim()
                    it[createdAt] = System.currentTimeMillis()
                } get Leads.id
            }

            call.respond(HttpStatusCode.Created, LeadCreatedResponse(newId, "Заявка успешно отправлена"))
        }

        // ── Admin routes (basic auth) ─────────────────────────────────────────
        authenticate("admin-auth") {

            // HTML-панель
            get("/admin") {
                val leads = fetchAllLeads()
                call.respondText(buildAdminHtml(leads), ContentType.Text.Html)
            }

            // JSON-список (для API-клиентов)
            get("/admin/leads") {
                call.respond(fetchAllLeads())
            }

            delete("/admin/leads/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf<String, String>("error" to "Некорректный id"))

                val deleted = transaction { Leads.deleteWhere { Leads.id eq id } }

                if (deleted == 0) call.respond(HttpStatusCode.NotFound, mapOf<String, String>("error" to "Заявка не найдена"))
                else call.respond(mapOf<String, String>("message" to "Заявка #$id удалена"))
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fetchAllLeads(): List<LeadResponse> = transaction {
    Leads.selectAll()
        .orderBy(Leads.createdAt, SortOrder.DESC)
        .map {
            LeadResponse(
                id = it[Leads.id],
                fullName = it[Leads.fullName],
                company = it[Leads.company],
                email = it[Leads.email],
                phone = it[Leads.phone],
                telegram = it[Leads.telegram],
                description = it[Leads.description],
                createdAt = it[Leads.createdAt]
            )
        }
}

private fun buildAdminHtml(leads: List<LeadResponse>): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm")

    val rows = if (leads.isEmpty()) {
        """<tr><td colspan="8" class="empty">Заявок пока нет</td></tr>"""
    } else {
        leads.joinToString("\n") { l ->
            val tg = l.telegram?.h()?.let { """<span class="badge">$it</span>""" } ?: "—"
            """
            <tr>
              <td class="muted">${l.id}</td>
              <td><strong>${l.fullName.h()}</strong></td>
              <td>${l.company.h()}</td>
              <td><a href="mailto:${l.email}">${l.email.h()}</a></td>
              <td>${l.phone.h()}</td>
              <td>$tg</td>
              <td class="desc">${l.description.h()}</td>
              <td class="muted nowrap">${fmt.format(Date(l.createdAt))}</td>
              <td><button class="del-btn" onclick="deleteLead(${l.id}, this)">Удалить</button></td>
            </tr>
            """.trimIndent()
        }
    }

    return """
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Microtize — Заявки</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
           background: #f0f2f5; color: #1a1a1a; }

    .topbar { background: #0f172a; color: #fff; padding: 16px 32px;
              display: flex; align-items: center; gap: 16px; }
    .topbar h1 { font-size: 18px; font-weight: 600; letter-spacing: -.3px; }
    .topbar .pill { background: #3b82f6; color: #fff; font-size: 12px; font-weight: 600;
                    padding: 3px 10px; border-radius: 999px; }

    .wrap { max-width: 1440px; margin: 28px auto; padding: 0 24px; }

    .card { background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.08);
            overflow: hidden; }

    table { width: 100%; border-collapse: collapse; font-size: 14px; }
    thead th { background: #f8fafc; padding: 12px 16px; text-align: left;
               font-size: 11px; font-weight: 700; text-transform: uppercase;
               letter-spacing: .6px; color: #64748b; border-bottom: 1px solid #e2e8f0; }
    tbody td { padding: 13px 16px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover td { background: #fafbff; }

    a { color: #3b82f6; text-decoration: none; }
    a:hover { text-decoration: underline; }

    .badge { display: inline-block; background: #eff6ff; color: #2563eb;
             padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 500; }
    .desc { max-width: 300px; color: #475569; line-height: 1.5; }
    .muted { color: #94a3b8; font-size: 12px; }
    .nowrap { white-space: nowrap; }
    .empty { text-align: center; padding: 64px; color: #94a3b8; font-size: 15px; }
    .del-btn { background: #fee2e2; color: #dc2626; border: none; padding: 4px 10px;
               border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; white-space: nowrap; }
    .del-btn:hover { background: #fca5a5; }
  </style>
</head>
<body>
  <div class="topbar">
    <h1>Microtize — Панель заявок</h1>
    <span class="pill">${leads.size}</span>
  </div>
  <div class="wrap">
    <div class="card">
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>ФИО</th>
            <th>Компания</th>
            <th>Email</th>
            <th>Телефон</th>
            <th>Telegram</th>
            <th>Описание</th>
            <th>Дата</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
$rows
        </tbody>
      </table>
    </div>
  </div>
  <script>
    async function deleteLead(id, btn) {
      if (!confirm('Удалить заявку #' + id + '?')) return;
      btn.disabled = true;
      const res = await fetch('/admin/leads/' + id, { method: 'DELETE' });
      if (res.ok) btn.closest('tr').remove();
      else { alert('Ошибка удаления'); btn.disabled = false; }
    }
  </script>
</body>
</html>
    """.trimIndent()
}

/** HTML-экранирование */
private fun String.h() = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
