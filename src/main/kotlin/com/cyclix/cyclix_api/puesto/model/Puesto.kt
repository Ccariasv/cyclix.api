package com.cyclix.cyclix_api.puesto.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidad que representa un Puesto (estación) de bicicletas.
 *
 * @Entity  → le dice a Spring que esta clase es una tabla en la BD
 * @Table   → especifica el nombre de la tabla
 */
@Entity
@Table(name = "puesto")
data class Puesto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Nombre legible del puesto, ej: "Puesto Centro Histórico" */
    @Column(nullable = false, length = 100)
    val nombre: String,

    /** Código único del puesto, ej: "PST-001" */
    @Column(nullable = false, unique = true, length = 50)
    val codigo: String,

    /** Dirección física del puesto */
    @Column(nullable = false, length = 255)
    val direccion: String,

    /** Coordenadas GPS para mostrar en el mapa de la app */
    @Column(nullable = false)
    val latitud: Double,

    @Column(nullable = false)
    val longitud: Double,

    /** Cuántas bicicletas caben en total en este puesto */
    @Column(name = "capacidad_total", nullable = false)
    val capacidadTotal: Int = 10,

    /** Cuántos espacios libres hay actualmente */
    @Column(name = "capacidad_disponible", nullable = false)
    val capacidadDisponible: Int = 10,

    /** Estado del puesto: ACTIVO, INACTIVO o MANTENIMIENTO */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val estado: EstadoPuesto = EstadoPuesto.ACTIVO,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/** Catálogo de estados posibles para un puesto */
enum class EstadoPuesto {
    ACTIVO,
    INACTIVO,
    MANTENIMIENTO
}