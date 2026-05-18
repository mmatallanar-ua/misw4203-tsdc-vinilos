package com.misw4203.vinilos.domain.model

data class AlbumDetail(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val releaseDate: String,
    val genre: String,
    val recordLabel: String,
    val description: String,
    val tracks: List<Track>,
    val performers: List<Performer>,
    val comments: List<Comment>,
)

data class Track(
    val id: Long,
    val name: String,
    val duration: String,
)

/**
 * El backend no etiqueta el tipo de un performer; se infiere por los campos
 * propios de cada entidad: `creationDate` ⇒ banda, `birthDate` (o ausencia de
 * `creationDate`) ⇒ músico. Necesario para elegir el endpoint correcto al
 * quitar un favorito (`/musicians/{id}` vs `/bands/{id}`).
 */
enum class PerformerKind { MUSICIAN, BAND, UNKNOWN }

data class Performer(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val kind: PerformerKind = PerformerKind.UNKNOWN,
)

data class Comment(
    val id: Long,
    val description: String,
    val rating: Int,
    val commenter: CollectorSummary? = null,
)
