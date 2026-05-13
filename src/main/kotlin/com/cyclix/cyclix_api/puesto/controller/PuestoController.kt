package com.cyclix.cyclix_api.puesto.controller

import com.cyclix.cyclix_api.puesto.dto.ApiResponse
import com.cyclix.cyclix_api.puesto.dto.PuestoRequest
import com.cyclix.cyclix_api.puesto.model.EstadoPuesto
import com.cyclix.cyclix_api.puesto.service.PuestoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * PuestoController: la "puerta de entrada" de la API para puestos.
 *
 * @RestController → esta clase maneja peticiones HTTP y devuelve JSON
 * @RequestMapping → todos los endpoints empiezan con /api/v1/puestos
 */
@RestController
@RequestMapping("/api/v1/puestos")
class PuestoController(
    private val puestoService: PuestoService
) {

    // --------------------------------------------------
    //  GET /api/v1/puestos
    //  Listar TODOS los puestos
    //  Requiere: JWT válido (cualquier rol)
    // --------------------------------------------------
    @GetMapping
    fun listarTodos(): ResponseEntity<ApiResponse<Any>> {
        val puestos = puestoService.listarTodos()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Puestos obtenidos", data = puestos)
        )
    }

    // --------------------------------------------------
    //  GET /api/v1/puestos/activos
    //  Solo los puestos que están ACTIVOS
    //  Útil para la app móvil: mostrar dónde rentar
    // --------------------------------------------------
    @GetMapping("/activos")
    fun listarActivos(): ResponseEntity<ApiResponse<Any>> {
        val puestos = puestoService.listarActivos()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Puestos activos obtenidos", data = puestos)
        )
    }

    // --------------------------------------------------
    //  GET /api/v1/puestos/disponibles
    //  Puestos con espacio para devolver una bicicleta
    //  Útil en la app cuando el usuario va a regresar la bici
    // --------------------------------------------------
    @GetMapping("/disponibles")
    fun listarConEspacio(): ResponseEntity<ApiResponse<Any>> {
        val puestos = puestoService.listarConEspacioDisponible()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Puestos con espacio disponible", data = puestos)
        )
    }

    // --------------------------------------------------
    //  GET /api/v1/puestos/{id}
    //  Detalle completo de un puesto específico
    //  El {id} en la URL es el número del puesto, ej: /puestos/3
    // --------------------------------------------------
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<ApiResponse<Any>> {
        return try {
            val puesto = puestoService.obtenerPorId(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Puesto encontrado", data = puesto))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = e.message ?: "No encontrado"))
        }
    }

    // --------------------------------------------------
    //  POST /api/v1/puestos
    //  Crear un puesto nuevo
    //  Requiere: JWT + Rol ADMIN
    //  @Valid → activa las validaciones del DTO automáticamente
    // --------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun crear(@Valid @RequestBody request: PuestoRequest): ResponseEntity<ApiResponse<Any>> {
        return try {
            val nuevo = puestoService.crear(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse(success = true, message = "Puesto creado exitosamente", data = nuevo))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse(success = false, message = e.message ?: "Conflicto"))
        }
    }

    // --------------------------------------------------
    //  PUT /api/v1/puestos/{id}
    //  Actualizar datos de un puesto
    //  Requiere: JWT + Rol ADMIN
    // --------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun actualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: PuestoRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizado = puestoService.actualizar(id, request)
            ResponseEntity.ok(ApiResponse(success = true, message = "Puesto actualizado", data = actualizado))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = e.message ?: "No encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse(success = false, message = e.message ?: "Conflicto"))
        }
    }

    // --------------------------------------------------
    //  PATCH /api/v1/puestos/{id}/estado?nuevoEstado=INACTIVO
    //  Cambiar solo el estado de un puesto
    //  Requiere: JWT + Rol ADMIN
    // --------------------------------------------------
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    fun cambiarEstado(
        @PathVariable id: Long,
        @RequestParam nuevoEstado: EstadoPuesto
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val actualizado = puestoService.cambiarEstado(id, nuevoEstado)
            ResponseEntity.ok(ApiResponse(success = true, message = "Estado actualizado", data = actualizado))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = e.message ?: "No encontrado"))
        }
    }
}