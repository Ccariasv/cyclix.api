
package com.cyclix.cyclix_api.bicycle.controller

import com.cyclix.cyclix_api.bicycle.dto.ApiResponse
import com.cyclix.cyclix_api.bicycle.dto.BicicletaRequest
import com.cyclix.cyclix_api.bicycle.dto.CambiarEstadoRequest
import com.cyclix.cyclix_api.bicycle.dto.UbicacionRequest
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.service.BicicletaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * BicicletaController: todos los endpoints del módulo de bicicletas.
 * Base URL: /api/v1/bicicletas
 */
@RestController
@RequestMapping("/api/v1/bicicletas")
class BicicletaController(
    private val bicicletaService: BicicletaService
) {

    // --------------------------------------------------
    //  GET /api/v1/bicicletas
    //  Listar bicicletas con filtros opcionales
    // --------------------------------------------------
    @GetMapping
    fun listarTodas(
        @RequestParam(required = false) estado: EstadoBicicleta?,
        @RequestParam(required = false) tipo: TipoBicicleta?,
        @RequestParam(required = false) puestoId: Long?,
        @RequestParam(defaultValue = "false") soloDisponibles: Boolean
    ): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listar(estado, tipo, puestoId, soloDisponibles)
        return ResponseEntity.ok(ApiResponse(true, "Bicicletas obtenidas", bicis))
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas?estado=DISPONIBLE
    //  Filtrar por estado (parámetro opcional en la URL)
    //  Ejemplo: /bicicletas?estado=DISPONIBLE
    // --------------------------------------------------
    @GetMapping("/filtrar")
    fun filtrarPorEstado(
        @RequestParam(required = false) estado: EstadoBicicleta?,
        @RequestParam(required = false) tipo: TipoBicicleta?
    ): ResponseEntity<ApiResponse<Any>> {
        val resultado = bicicletaService.listar(estado, tipo)
        return ResponseEntity.ok(ApiResponse(true, "Bicicletas filtradas", resultado))
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/sin-puesto
    //  Bicis en mantenimiento sin puesto asignado
    // --------------------------------------------------
    @GetMapping("/sin-puesto")
    @PreAuthorize("hasRole('ADMIN')")
    fun listarSinPuesto(): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listarSinPuesto()
        return ResponseEntity.ok(ApiResponse(true, "Bicicletas sin puesto", bicis))
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/puesto/{puestoId}
    //  Todas las bicis de un puesto específico
    // --------------------------------------------------
    @GetMapping("/puesto/{puestoId}")
    fun listarPorPuesto(@PathVariable puestoId: Long): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listarPorPuesto(puestoId)
        return ResponseEntity.ok(ApiResponse(true, "Bicicletas del puesto $puestoId", bicis))
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/puesto/{puestoId}/disponibles
    //  Solo las DISPONIBLES en ese puesto (para la app al rentar)
    // --------------------------------------------------
    @GetMapping("/puesto/{puestoId}/disponibles")
    fun listarDisponiblesEnPuesto(@PathVariable puestoId: Long): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listarDisponiblesEnPuesto(puestoId)
        return ResponseEntity.ok(ApiResponse(true, "Bicicletas disponibles en puesto $puestoId", bicis))
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/{id}
    //  Detalle completo de una bicicleta
    // --------------------------------------------------
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<ApiResponse<Any>> {
        return try {
            val bici = bicicletaService.obtenerPorId(id)
            ResponseEntity.ok(ApiResponse(true, "Bicicleta encontrada", bici))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        }
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/qr/{codigoQr}
    //  La app móvil llama a este endpoint al escanear el QR
    //  y obtiene todos los datos de la bici
    // --------------------------------------------------
    @GetMapping("/qr/{codigoQr}")
    fun obtenerPorQr(@PathVariable codigoQr: String): ResponseEntity<ApiResponse<Any>> {
        return try {
            val bici = bicicletaService.obtenerPorQr(codigoQr)
            ResponseEntity.ok(ApiResponse(true, "Bicicleta encontrada por QR", bici))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        }
    }

    // --------------------------------------------------
    //  POST /api/v1/bicicletas
    //  Registrar una bicicleta nueva (solo ADMIN)
    // --------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun crear(@Valid @RequestBody request: BicicletaRequest): ResponseEntity<ApiResponse<Any>> {
        return try {
            val nueva = bicicletaService.crear(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(true, "Bicicleta registrada exitosamente", nueva))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse(false, e.message ?: "Conflicto"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "Puesto no encontrado"))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(false, e.message ?: "Error de estado"))
        }
    }

    // --------------------------------------------------
    //  PUT /api/v1/bicicletas/{id}
    //  Actualizar datos de una bicicleta (solo ADMIN)
    // --------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun actualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: BicicletaRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizada = bicicletaService.actualizar(id, request)
            ResponseEntity.ok(ApiResponse(true, "Bicicleta actualizada", actualizada))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse(false, e.message ?: "Conflicto"))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(false, e.message ?: "Error de estado"))
        }
    }

    // --------------------------------------------------
    //  PATCH /api/v1/bicicletas/{id}/estado
    //  Cambiar estado de una bici (rentar, devolver, mantenimiento)
    //  Esta es la operación más usada en el día a día
    // --------------------------------------------------
    @PatchMapping("/{id}/estado")
    fun cambiarEstado(
        @PathVariable id: Long,
        @Valid @RequestBody request: CambiarEstadoRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizada = bicicletaService.cambiarEstado(id, request)
            ResponseEntity.ok(ApiResponse(true, "Estado actualizado a ${request.nuevoEstado}", actualizada))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(false, e.message ?: "Cambio de estado no permitido"))
        }
    }

    // --------------------------------------------------
    //  DELETE /api/v1/bicicletas/{id}
    //  Baja lógica de una bicicleta (solo ADMIN)
    // --------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun darDeBaja(@PathVariable id: Long): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizada = bicicletaService.darDeBaja(id)
            ResponseEntity.ok(ApiResponse(true, "Bicicleta dada de baja", actualizada))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        }
    }
}
