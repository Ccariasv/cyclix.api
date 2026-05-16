package com.cyclix.cyclix_api.common.error

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val field = (error as? FieldError)?.field ?: error.objectName
            field to (error.defaultMessage ?: "Valor inválido")
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación", errors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Solicitud inválida")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Map<String, Any>> =
        buildResponse(
            HttpStatus.valueOf(ex.statusCode.value()),
            ex.reason ?: "Error en la solicitud"
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Solicitud inválida")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.BAD_REQUEST, "Formato de request inválido")

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Map<String, Any>> {
        log.error("Unhandled exception in request processing", ex)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor")
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        errors: Map<String, String>? = null
    ): ResponseEntity<Map<String, Any>> {
        val body = linkedMapOf<String, Any>(
            "timestamp" to LocalDateTime.now(),
            "status" to status.value(),
            "error" to status.reasonPhrase,
            "message" to message
        )
        if (!errors.isNullOrEmpty()) {
            body["errors"] = errors
        }
        return ResponseEntity.status(status).body(body)
    }
}
