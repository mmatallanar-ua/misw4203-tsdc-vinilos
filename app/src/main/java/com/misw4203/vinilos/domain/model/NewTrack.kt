package com.misw4203.vinilos.domain.model

/**
 * Datos de entrada para crear un track. Modelo de dominio puro:
 * la capa de datos lo mapea a su DTO de red.
 */
data class NewTrack(
    val name: String,
    val duration: String,
)
