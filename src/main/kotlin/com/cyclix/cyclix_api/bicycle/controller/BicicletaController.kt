
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
    //  Listar todas las bicicletas
    // --------------------------------------------------
    @GetMapping
    fun listarTodas(): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listarTodas()
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
        val resultado = when {
            estado != null -> bicicletaService.listarPorEstado(estado)
            tipo   != null -> bicicletaService.listarPorTipo(tipo)
            else           -> bicicletaService.listarTodas()
        }
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
    //  PATCH /api/v1/bicicletas/{id}/ubicacion
    //  Actualizar ubicación GPS de una bicicleta (ESP32/Pruebas)
    // --------------------------------------------------
    @PatchMapping("/{id}/ubicacion")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun actualizarUbicacion(
        @PathVariable id: Long,
        @Valid @RequestBody request: UbicacionRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizada = bicicletaService.actualizarUbicacion(id, request)
            ResponseEntity.ok(ApiResponse(true, "Ubicación actualizada correctamente", actualizada))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        }
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/{id}/ubicacion
    //  Obtener ubicación actual de una bicicleta específica
    // --------------------------------------------------
    @GetMapping("/{id}/ubicacion")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun obtenerUbicacion(@PathVariable id: Long): ResponseEntity<ApiResponse<Any>> {
        return try {
            val bici = bicicletaService.obtenerPorId(id)
            val ubicacion = mapOf("latitud" to bici.latitud, "longitud" to bici.longitud)
            ResponseEntity.ok(ApiResponse(true, "Ubicación obtenida", ubicacion))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(false, e.message ?: "No encontrada"))
        }
    }

    // --------------------------------------------------
    //  GET /api/v1/bicicletas/ubicaciones
    //  Obtener ubicaciones de todas las bicicletas
    // --------------------------------------------------
    @GetMapping("/ubicaciones")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun obtenerUbicaciones(): ResponseEntity<ApiResponse<Any>> {
        val bicis = bicicletaService.listarTodas()
        val ubicaciones = bicis.map {
            mapOf(
                "id" to it.id,
                "codigo" to it.codigo,
                "estado" to it.estado,
                "latitud" to it.latitud,
                "longitud" to it.longitud
            )
        }
        return ResponseEntity.ok(ApiResponse(true, "Ubicaciones obtenidas", ubicaciones))
    }
}