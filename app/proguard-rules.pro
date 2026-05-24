# ============================================================================
# Vinilos — reglas R8/ProGuard dirigidas (NO -keep class ** { *; }).
#
# Cubren la superficie reflexiva propia que las consumer rules embarcadas
# NO cubren. Verificado contra el código, las versiones y un smoke del APK
# release shrinkado vs. el control debug sin R8 (Fase 7 A4):
#   - Gson 2.11.0 solo conserva los CAMPOS @SerializedName, NO la clase ni
#     su constructor → los DTOs SÍ necesitan regla (ver abajo): bajo R8
#     full mode su única referencia es el tipo genérico reflexivo del
#     servicio Retrofit, así que la clase se elimina/inutiliza y la
#     deserialización de la respuesta produce vacío (lista en error).
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

# Las subclases ANÓNIMAS de TypeToken declaradas en Converters
# (`object : TypeToken<List<...>>() {}`, izadas como constantes estáticas)
# deben conservar su firma genérica. Las consumer rules de Gson NO cubren
# las subclases anónimas de la app: bajo R8 *full mode* el supertipo
# genérico se borra y `TypeToken.getType()` lanza
# `IllegalStateException: TypeToken must be created with a type argument`
# en el `<clinit>` del companion de Converters; Room (AlbumDao_Impl.<clinit>)
# lo propaga como ExceptionInInitializerError → crash al abrir la app.
# (Detectado en el smoke manual del APK release shrinkado — Fase 7 A4.)
#
# Par EXACTO documentado por Gson para R8 >= 3.0 (examples/android-proguard
# -example): el modificador contraintuitivo `allowobfuscation,allowshrinking`
# es lo que hace que R8 retenga la firma genérica de TypeToken y subclases
# (un `-keep` sin `allowshrinking` NO la preserva en full mode).
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# DTOs de red: Retrofit+Gson los instancian y pueblan por reflexión desde
# las respuestas; su única referencia concreta es el tipo genérico del
# servicio (reflexivo), por lo que en R8 full mode la CLASE se elimina o
# pierde el constructor que Gson usa para instanciarla. Las consumer rules
# de Gson solo conservan los campos @SerializedName, NO la clase. Sin esto
# la deserialización de toda respuesta de red produce objetos vacíos y la
# lista entra en estado de error (aislado: el control debug sin R8 carga
# OK contra el mismo backend; el release shrinkado no — Fase 7 A4).
-keep,allowobfuscation class com.misw4203.vinilos.data.remote.dto.** { *; }

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
