# Vinilos - MISW4203

Aplicación Android desarrollada con **Jetpack Compose** y arquitectura **MVVM + Clean Architecture**.

---

## Estructura del proyecto

```
app/src/main/java/com/misw4203/vinilos/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   └── entities/
│   ├── remote/
│   │   ├── api/
│   │   └── dto/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── viewmodel/
│   └── ui/
│       ├── screens/
│       │   ├── album/
│       │   ├── artist/
│       │   └── collector/
│       ├── components/
│       └── theme/
└── di/
```

---

## Descripción de cada capa

### `data/` — Capa de datos

Responsable de obtener y persistir datos, ya sea desde la red o desde la base de datos local.

| Carpeta | Contenido |
|---|---|
| `data/local/dao/` | Interfaces DAO de Room. Definen las operaciones de lectura y escritura sobre la base de datos local (consultas SQL). |
| `data/local/entities/` | Clases de entidad de Room anotadas con `@Entity`. Representan las tablas de la base de datos local. |
| `data/remote/api/` | Interfaces de Retrofit anotadas con `@GET`, `@POST`, etc. Definen los endpoints del API REST. |
| `data/remote/dto/` | Data Transfer Objects (DTOs). Clases que modelan la respuesta JSON del API. Se mapean a modelos de dominio antes de llegar a la capa superior. |
| `data/repository/` | Implementaciones concretas de las interfaces de repositorio definidas en `domain/repository/`. Coordinan las fuentes de datos (remota y local) y devuelven modelos de dominio. |

---

### `domain/` — Capa de dominio

Es el núcleo de la aplicación. No depende de ninguna otra capa y contiene la lógica de negocio pura.

| Carpeta | Contenido |
|---|---|
| `domain/model/` | Modelos de dominio. Son las entidades principales del negocio (ej. `Album`, `Artist`, `Collector`). No tienen dependencias de Android ni de librerías externas. |
| `domain/repository/` | Interfaces de repositorio. Definen el contrato de acceso a datos que la capa de dominio necesita. Su implementación vive en `data/repository/`. |
| `domain/usecase/` | Casos de uso. Cada clase encapsula una acción específica del negocio (ej. `GetAlbumsUseCase`, `GetArtistDetailUseCase`). Son invocados desde los ViewModels. |

---

### `presentation/` — Capa de presentación

Contiene todo lo relacionado con la interfaz de usuario y el estado de la pantalla.

| Carpeta | Contenido |
|---|---|
| `presentation/viewmodel/` | ViewModels de Jetpack. Exponen el estado de la UI mediante `StateFlow` y llaman a los casos de uso. Sobreviven a los cambios de configuración. |
| `presentation/ui/screens/album/` | Composables de las pantallas relacionadas con álbumes (listado, detalle). |
| `presentation/ui/screens/artist/` | Composables de las pantallas relacionadas con artistas. |
| `presentation/ui/screens/collector/` | Composables de las pantallas relacionadas con coleccionistas. |
| `presentation/ui/components/` | Composables reutilizables y genéricos que pueden ser usados en cualquier pantalla (ej. `LoadingIndicator`, `ErrorMessage`, `AlbumCard`). |
| `presentation/ui/theme/` | Definición del tema de Material 3: colores, tipografía y formas. |

---

### `di/` — Inyección de dependencias

Módulos de Hilt que proveen las dependencias de la aplicación.

| Archivo | Contenido |
|---|---|
| `NetworkModule` | Configura el cliente Retrofit y OkHttp. Provee la instancia del API service. |
| `DatabaseModule` | Configura la base de datos Room y provee los DAOs. |
| `RepositoryModule` | Vincula las interfaces de repositorio del dominio con sus implementaciones en la capa de datos. |

---

## Stack tecnológico

| Tecnología | Uso |
|---|---|
| Jetpack Compose | Framework de UI declarativo |
| ViewModel + StateFlow | Gestión del estado de la UI |
| Hilt | Inyección de dependencias |
| Room | Persistencia local |
| Retrofit + OkHttp | Comunicación con el API REST |
| Navigation Compose | Navegación entre pantallas |
| Coroutines | Operaciones asíncronas |
| Coil | Carga de imágenes |
| Material 3 | Sistema de diseño |
