# ============================================================================
# Vinilos — reglas R8/ProGuard dirigidas (NO -keep class ** { *; }).
#
# Solo cubren la ÚNICA superficie reflexiva propia que las consumer rules
# embarcadas NO cubren. Verificado contra el código y las versiones:
#   - Gson 2.11.0 embarca reglas que conservan los campos @SerializedName
#     → los DTOs (data.remote.dto, TODOS con @SerializedName) NO necesitan regla.
#   - Retrofit 2.11.0 embarca consumer rules desde 2.9.0 (interfaces @GET/@POST,
#     firmas, Response) → VinilosApiService / okhttp / okio NO necesitan regla.
#   - kotlinx-coroutines 1.11.0 embarca sus reglas (>=1.7.0) → sin -dontwarn.
#   - proguard-android-optimize.txt ya conserva values()/valueOf de los enums.
#   - Room / Hilt / Compose / Coil embarcan sus propias consumer rules (AAR).
# ============================================================================

# Gson resuelve genéricos vía la firma (object : TypeToken<List<...>>() {} en
# Converters). Es metadata a nivel de la app que las reglas de Gson asumen
# presente; sin esto la (de)serialización de listas pierde el tipo elemento.
-keepattributes Signature

# Modelos de DOMINIO serializados por reflexión en
# data/local/converter/Converters.kt (Room cache: List<Track/Performer/Comment/
# Album/MusicianPrize/CollectorAlbum/CollectorComment/MusicianSummary> vía
# TypeToken). A diferencia de los DTOs, NINGÚN modelo de dominio usa
# @SerializedName (data class Kotlin planas), así que las reglas embarcadas de
# Gson NO los protegen: sin esto R8 renombra los campos y la caché Room se
# (de)serializa con datos vacíos/erróneos en runtime. Regla mínima: solo los
# campos, permitiendo ofuscar el nombre de clase.
-keepclassmembers,allowobfuscation class com.misw4203.vinilos.domain.model.** {
    <fields>;
}

# PerformerKind viaja serializado dentro de List<Performer> por Gson en
# Converters. Gson (de)serializa enums por el nombre de la constante; se
# conservan las constantes de ESTE enum concreto (regla acotada a una sola
# clase, no -keepclassmembers enum * { *; }) para que la caché no se corrompa.
-keepclassmembers enum com.misw4203.vinilos.domain.model.PerformerKind {
    <fields>;
}
