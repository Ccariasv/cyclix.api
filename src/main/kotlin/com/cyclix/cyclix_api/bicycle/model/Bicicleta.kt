package com.cyclix.cyclix_api.bicycle.model

import com.cyclix.cyclix_api.puesto.model.Puesto
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entidad Bicicleta.
 *
 * La relación con Puesto es OPCIONAL (nullable = true)
 * porque una bici puede estar en mantenimiento sin estar
 * asignada a ningún puesto físico.
 */
@Entity
@Table(name = "bicicleta")
data class Bicicleta(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Código/Serie único, ej: "BIC-001" */
    @Column(nullable = false, unique = true, length = 50)
    val codigo: String,

    @Column(nullable = false, length = 100)
    val marca: String,

    @Column(nullable = false, length = 100)
    val modelo: String,

    @Column(nullable = false, length = 50)
    val color: String,

    /** Tipo de bicicleta: URBANA, MONTANA o ELECTRICA */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val tipo: TipoBicicleta,

    /** Tamaño de llanta en pulgadas, ej: 26.0, 27.5, 29.0 */
    @Column(name = "tamano_llanta", nullable = false)
    val tamanoLlanta: Double,

    /** Precio de renta por hora */
    @Column(name = "precio_por_hora", nullable = false)
    val precioPorHora: BigDecimal,

    /** Estado actual de la bicicleta */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val estado: EstadoBicicleta = EstadoBicicleta.DISPONIBLE,

    /**
     * Código QR generado al registrar la bici.
     * El backend guarda aquí el texto/URL que el QR codifica.
     * La app móvil lee este QR para identificar la bici.
     */
    @Column(name = "codigo_qr", nullable = true, length = 255)
    val codigoQr: String? = null,

    /**
     * Relación con Puesto — es OPCIONAL.
     * @ManyToOne → muchas bicis pueden estar en el mismo puesto
     * fetch = LAZY → no carga el puesto de la BD hasta que lo necesitemos
     *                (mejor rendimiento)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = true)
    val puesto: Puesto? = null,

    @Column(name = "latitud", nullable = true)
    val latitud: Double? = null,

    @Column(name = "longitud", nullable = true)
    val longitud: Double? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/** Los tipos de bicicleta disponibles en el sistema */
enum class TipoBicicleta {
    URBANA,
    MONTAÑA,
    ELECTRICA
}

/** Los estados posibles de una bicicleta */
enum class EstadoBicicleta {
    DISPONIBLE,       // en el puesto, lista para rentar
    EN_USO,           // actualmente rentada por un usuario
    MANTENIMIENTO,    // en reparación, puede no tener puesto
    FUERA_DE_SERVICIO,// inhabilitada permanentemente
    RESERVADA         // apartada, pendiente de ser recogida
}