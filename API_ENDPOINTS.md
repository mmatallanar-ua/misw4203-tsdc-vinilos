# API Endpoints — BackVynils

Referencia completa de la API REST del backend (NestJS + TypeORM). Pensada para
cruzarla contra la app móvil Android y detectar qué endpoints faltan por consumir.

## Información general

- **Base URL local:** `http://localhost:3000` (puerto 3000, **sin** prefijo global tipo `/api`)
- **Base URL desplegada (Heroku):** según `Entorno Colecciones Vynil` de `collections/`
- **CORS:** habilitado para todos los orígenes (`app.enableCors()`)
- **Autenticación:** ninguna (no hay guards, JWT ni API keys)
- **Content-Type:** `application/json`
- **Body de error:** `{ "statusCode": <n>, "message": "<texto>" }`
- **Mapeo de errores** (interceptor `BusinessErrorsInterceptor`):
  - `NOT_FOUND` → `404`
  - `PRECONDITION_FAILED` → `412`
  - `BAD_REQUEST` → `400` (incluye fallos de validación Joi)
- **Validación:** Joi dentro de los servicios. Los campos marcados como *req* son obligatorios.

### Convenciones de respuesta importantes para Android

1. **POST devuelve `200`, no `201 Created`.**
2. **DELETE devuelve `204 No Content`** (sin cuerpo).
3. **PUT de asociación** (`bandDTO[]`, `albumDTO[]`, etc.) **reemplaza toda la colección**, no agrega.
4. **Enums sensibles a mayúsculas:** `"rock"` ≠ `"Rock"` → produce `400`.
5. Algunas rutas de asociación terminan en `/` en el código fuente; NestJS tolera ambas formas.

## Enums

| Enum | Valores válidos (exactos) |
|---|---|
| `GENRE` | `Classical`, `Salsa`, `Rock`, `Folk` |
| `RECORD_LABEL` | `Sony Music`, `EMI`, `Discos Fuentes`, `Elektra`, `Fania Records` |
| `ALBUM_STATUS` | `Active`, `Inactive` |

## DTOs

| DTO | Campos (req = obligatorio) |
|---|---|
| `AlbumDTO` | `name` req, `cover` req, `releaseDate` date req, `description` req, `genre` GENRE req, `recordLabel` RECORD_LABEL req |
| `BandDTO` | `name` req, `description` req, `creationDate` date req, `image` uri opcional |
| `MusicianDTO` | `name` req, `description` req, `birthDate` date opcional, `image` uri opcional |
| `CollectorDTO` | `name` req, `telephone` req, `email` email req |
| `PrizeDTO` | `name` req, `description` req, `organization` req |
| `TrackDTO` | `name` req, `duration` string req (ej. `"3:45"`) |
| `CommentDTO` | `description` req, `rating` number 0–5 req, `collector` CollectorDTO opcional |
| `CollectorAlbumDTO` | `price` number req, `status` ALBUM_STATUS req, `album` opcional |
| `PerformerPrizeDTO` | `premiationDate` date opcional |

---

## 1. Albums — `/albums`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/albums` | — | 200 |
| GET | `/albums/:albumId` | — | 200 |
| POST | `/albums` | `AlbumDTO` | 200 |
| PUT | `/albums/:albumId` | `AlbumDTO` | 200 |
| DELETE | `/albums/:albumId` | — | 204 |

## 2. Bands — `/bands`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/bands` | — | 200 |
| GET | `/bands/:bandId` | — | 200 |
| POST | `/bands` | `BandDTO` | 200 |
| PUT | `/bands/:bandId` | `BandDTO` | 200 |
| DELETE | `/bands/:bandId` | — | 204 |

## 3. Musicians — `/musicians`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/musicians` | — | 200 |
| GET | `/musicians/:musicianId` | — | 200 |
| POST | `/musicians` | `MusicianDTO` | 200 |
| PUT | `/musicians/:musicianId` | `MusicianDTO` | 200 |
| DELETE | `/musicians/:musicianId` | — | 204 |

## 4. Collectors — `/collectors`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/collectors` | — | 200 |
| GET | `/collectors/:collectorId` | — | 200 |
| POST | `/collectors` | `CollectorDTO` | 200 |
| PUT | `/collectors/:collectorId` | `CollectorDTO` | 200 |
| DELETE | `/collectors/:collectorId` | — | 204 |

## 5. Prizes — `/prizes`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/prizes` | — | 200 |
| GET | `/prizes/:prizeId` | — | 200 |
| POST | `/prizes` | `PrizeDTO` | 200 |
| PUT | `/prizes/:prizeId` | `PrizeDTO` | 200 |
| DELETE | `/prizes/:prizeId` | — | 204 |

## 6. Tracks (anidado) — `/albums/:albumId/tracks`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/albums/:albumId/tracks` | — | 200 |
| GET | `/albums/:albumId/tracks/:trackId` | — | 200 |
| POST | `/albums/:albumId/tracks` | `TrackDTO` | 200 |
| PUT | `/albums/:albumId/tracks/:trackId` | `TrackDTO` | 200 |
| DELETE | `/albums/:albumId/tracks/:trackId` | — | 204 |

## 7. Comments (anidado) — `/albums/:albumId/comments`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/albums/:albumId/comments` | — | 200 |
| GET | `/albums/:albumId/comments/:commentId` | — | 200 |
| POST | `/albums/:albumId/comments` | `CommentDTO` | 200 |
| PUT | `/albums/:albumId/comments/:commentId` | `CommentDTO` | 200 |
| DELETE | `/albums/:albumId/comments/:commentId` | — | 204 |

## 8. CollectorAlbum — `/collectors/:collectorId/albums`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/collectors/:collectorId/albums` | — | 200 |
| GET | `/collectors/:collectorId/albums/:albumId` | — | 200 |
| POST | `/collectors/:collectorId/albums/:albumId` | `CollectorAlbumDTO` | 200 |
| PUT | `/collectors/:collectorId/albums/:albumId` | `CollectorAlbumDTO` | 200 |
| DELETE | `/collectors/:collectorId/albums/:albumId` | — | 204 |

## 9. Collector ↔ Performer (favoritos) — `/collectors/:collectorId/...`

| Método | Ruta | OK |
|---|---|---|
| GET | `/collectors/:collectorId/performers` | 200 |
| POST | `/collectors/:collectorId/bands/:bandId` | 200 |
| POST | `/collectors/:collectorId/musicians/:musicianId` | 200 |
| DELETE | `/collectors/:collectorId/bands/:bandId` | 204 |
| DELETE | `/collectors/:collectorId/musicians/:musicianId` | 204 |

## 10. Album ↔ Band — `/albums/:albumId/bands`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/albums/:albumId/bands` | — | 200 |
| GET | `/albums/:albumId/bands/:bandId` | — | 200 |
| POST | `/albums/:albumId/bands/:bandId/` | — | 200 |
| PUT | `/albums/:albumId/bands` | `BandDTO[]` (reemplaza todas) | 200 |
| DELETE | `/albums/:albumId/bands/:bandId` | — | 204 |

## 11. Album ↔ Musician — `/albums/:albumId/musicians`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/albums/:albumId/musicians` | — | 200 |
| GET | `/albums/:albumId/musicians/:musicianId` | — | 200 |
| POST | `/albums/:albumId/musicians/:musicianId/` | — | 200 |
| PUT | `/albums/:albumId/musicians` | `MusicianDTO[]` | 200 |
| DELETE | `/albums/:albumId/musicians/:musicianId` | — | 204 |

## 12. Band ↔ Album — `/bands/:bandId/albums`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/bands/:bandId/albums/` | — | 200 |
| GET | `/bands/:bandId/albums/:albumId` | — | 200 |
| POST | `/bands/:bandId/albums/:albumId` | — | 200 |
| PUT | `/bands/:bandId/albums/` | `AlbumDTO[]` | 200 |
| DELETE | `/bands/:bandId/albums/:albumId` | — | 204 |

## 13. Musician ↔ Album — `/musicians/:musicianId/albums`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/musicians/:musicianId/albums/` | — | 200 |
| GET | `/musicians/:musicianId/albums/:albumId` | — | 200 |
| POST | `/musicians/:musicianId/albums/:albumId` | — | 200 |
| PUT | `/musicians/:musicianId/albums/` | `AlbumDTO[]` | 200 |
| DELETE | `/musicians/:musicianId/albums/:albumId` | — | 204 |

## 14. Band ↔ Musician — `/bands/:bandId/musicians`

| Método | Ruta | OK |
|---|---|---|
| GET | `/bands/:bandId/musicians/` | 200 |
| GET | `/bands/:bandId/musicians/:musicianId` | 200 |
| POST | `/bands/:bandId/musicians/:musicianId` | 200 |
| DELETE | `/bands/:bandId/musicians/:musicianId` | 204 |

## 15. PerformerPrize — `/prizes/...` y `/performerprizes`

| Método | Ruta | Body | OK |
|---|---|---|---|
| GET | `/prizes/:prizeId/performers` | — | 200 |
| POST | `/prizes/:prizeId/musicians/:musicianId` | `PerformerPrizeDTO` | 200 |
| POST | `/prizes/:prizeId/bands/:bandId` | `PerformerPrizeDTO` | 200 |
| DELETE | `/prizes/:prizeId/musicians/:musicianId` | — | 204 |
| DELETE | `/prizes/:prizeId/bands/:bandId` | — | 204 |
| GET | `/performerprizes` | — | 200 |

---

**Total: 73 endpoints en 15 grupos funcionales.**

> Datos semilla: la migración `src/migration/1611260351811-DataSetup.ts` crea registros
> con IDs fijos `100–103` (albums, performers, collectors, tracks, prizes, comments),
> útiles para pruebas desde el cliente Android sin tener que crear datos primero.
