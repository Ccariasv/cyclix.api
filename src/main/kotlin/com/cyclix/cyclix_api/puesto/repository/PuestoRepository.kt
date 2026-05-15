package com.cyclix.cyclix_api.puesto.repository

import com.cyclix.cyclix_api.puesto.model.EstadoPuesto
import com.cyclix.cyclix_api.puesto.model.Puesto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * PuestoRepository: la capa que habla directamente con la base de datos.
 *
 * Al extender JpaRepository ya tienes gratis métodos como:
 *   - findAll()       → traer todos los puestos
 *   - findById(id)    → buscar por id
 *   - save(puesto)    → guardar o actualizar
 *   - deleteById(id)  → eliminar
 *
 * Aquí solo agregamos las búsquedas especiales que necesitamos.
 */
@Repository
interface PuestoRepository : JpaRepository<Puesto, Long> {

    /** Verificar si ya existe un puesto con ese código (para no duplicar) */
    fun existsByCodigo(codigo: String): Boolean

    /** Buscar un puesto por su código único */
    fun findByCodigo(codigo: String): Puesto?

    /** Listar solo los puestos activos (para la app móvil) */
    fun findByEstado(estado: EstadoPuesto): List<Puesto>

    /** Listar puestos que aún tienen espacio disponible */
    @Query("SELECT p FROM Puesto p WHERE p.capacidadDisponible > 0 AND p.estado = 'ACTIVO'")
    fun findPuestosConEspacioDisponible(): List<Puesto>
}