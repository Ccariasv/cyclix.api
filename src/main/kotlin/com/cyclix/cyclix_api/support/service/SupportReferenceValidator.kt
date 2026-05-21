package com.cyclix.cyclix_api.support.service

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ConcurrentHashMap

@Service
class SupportReferenceValidator(
    private val jdbcTemplate: JdbcTemplate
) {
    private val tableExistsCache = ConcurrentHashMap<String, Boolean>()

    fun validateReferences(bikeId: Long?, tripId: Long?, paymentId: Long?) {
        bikeId?.let { validatePositiveAndExists("bikeId", "bicicleta", it) }
        tripId?.let { validatePositiveAndExists("tripId", "trips", it) }
        paymentId?.let { validatePositiveAndExists("paymentId", "wallet_transactions", it) }
    }

    private fun validatePositiveAndExists(fieldName: String, tableName: String, id: Long) {
        if (id <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName debe ser mayor que 0")
        }

        if (!tableExists(tableName)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede validar $fieldName porque la tabla '$tableName' no existe"
            )
        }

        val exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM $tableName WHERE id = ?)",
            Boolean::class.java,
            id
        ) ?: false

        if (!exists) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName no existe: $id")
        }
    }

    private fun tableExists(tableName: String): Boolean =
        tableExistsCache.computeIfAbsent(tableName) {
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) > 0
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """.trimIndent(),
                Boolean::class.java,
                tableName
            ) ?: false
        }
}
