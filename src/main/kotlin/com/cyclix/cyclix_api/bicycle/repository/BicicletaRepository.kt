package com.cyclix.cyclix_api.bicycle.repository

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * BicicletaRepository: habla directamente con la tabla `bicicleta`.
 *
 * Heredamos de JpaRepository y ya tenemos gratis:
 *   findAll(), findById(), save(), deleteById(), count()...
 *
 * Aquí solo declaramos las búsquedas especiales del negocio.
 */
@Repository
interface BicicletaRepository : JpaRepository<Bicicleta, Long> {

    /** ¿Ya existe una bici con ese código? (evitar duplicados) */
    fun existsByCodigo(codigo: String): Boolean

    /** Buscar bici por su código único */
    fun findByCodigo(codigo: String): Bicicleta?

    /** Buscar bici por su código QR (la app lo usa al escanear) */
    fun findByCodigoQr(codigoQr: String): Bicicleta?

    /** Todas las bicis de un estado determinado */
    fun findByEstado(estado: EstadoBicicleta): List<Bicicleta>

    /** Todas las bicis de un tipo (ej: solo ELECTRICAS) */
    fun findByTipo(tipo: TipoBicicleta): List<Bicicleta>

    /** Todas las bicis disponibles en un puesto específico */
    @Query("""
        SELECT b FROM Bicicleta b
        WHERE b.puesto.id = :puestoId
        AND b.estado = 'DISPONIBLE'
    """)
    fun findDisponiblesEnPuesto(puestoId: Long): List<Bicicleta>

    /** Todas las bicis (sin importar estado) de un puesto */
    @Query("SELECT b FROM Bicicleta b WHERE b.puesto.id = :puestoId")
    fun findByPuestoId(puestoId: Long): List<Bicicleta>

    /** Bicis sin puesto asignado (en tránsito o mantenimiento externo) */
    @Query("SELECT b FROM Bicicleta b WHERE b.puesto IS NULL")
    fun findSinPuesto(): List<Bicicleta>
}