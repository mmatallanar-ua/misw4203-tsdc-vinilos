# ADR 0001 — Sin concepto de sesión / "collector actual"

- **Estado:** Aceptada (deuda técnica aceptada)
- **Fecha:** 2026-05-17
- **Contexto del curso:** MISW4203 (TSDC) — Vinilos

## Contexto

La HU09 ("agregar comentario a un álbum") exige que el `POST /albums/{id}/comments`
incluya un campo `collector` con el id del coleccionista que comenta. Sin embargo,
el alcance del curso **no incluye autenticación ni gestión de usuarios**: la app
no tiene login, ni token, ni un "usuario actual" persistido.

El resto de acciones que requieren un `collectorId`
(`addAlbumToCollector`, `addFavoritePerformer`, eliminar favoritos/álbumes)
**sí** parten de una navegación real: el usuario abre la lista de coleccionistas,
entra a un coleccionista concreto (`collector/{collectorId}`) y desde su detalle
dispara la acción. En esos flujos `collectorId` es el id legítimamente
seleccionado por el usuario y viaja como argumento de navegación
(`CollectorDetailScreen` → `Destinations.addAlbumToCollector(collectorId)`, etc.).

El único caso sin un coleccionista de origen es **agregar comentario a un álbum**:
se llega desde el detalle del álbum, donde no hay coleccionista en contexto.

## Decisión

1. **No introducir** un `SessionRepository` / `CurrentCollectorProvider` ni
   ninguna capa de sesión/autenticación. Estaría fuera del alcance del curso y
   añadiría complejidad (almacenamiento de credenciales, ciclo de vida de sesión,
   manejo de expiración) sin requisito que lo justifique.
2. Para el flujo de comentario, usar una **referencia por defecto**:
   `Destinations.DefaultCollectorId = 100`. Se propaga vía ruta
   (`album/{albumId}/comment/add/{collectorId}`) y `SavedStateHandle` hasta
   `AddCommentViewModel`, manteniendo el VM agnóstico de cómo se obtuvo el id
   (mismo patrón que el resto de VMs de formulario).
3. Los demás `collectorId` **se mantienen** provenientes de navegación real;
   no se sustituyen por la constante.

## Consecuencias

**Positivas**
- Cero superficie de auth: nada de credenciales que proteger ni sesión que
  mantener (coherente con la nota de seguridad de cleartext sólo en hosts dev).
- El acoplamiento del default queda **localizado**: un único `const val` con
  KDoc en `Destinations.kt`; ningún VM ni repositorio conoce el valor mágico.
- `AddCommentViewModel` es testeable igual que el resto: recibe `collectorId`
  por `SavedStateHandle`, sin dependencia oculta a una sesión.

**Negativas / deuda asumida**
- Todos los comentarios creados desde la app se atribuyen al coleccionista
  `100`, independientemente de quién use la app. Es incorrecto desde el punto
  de vista de dominio, pero aceptable porque no hay identidad de usuario que
  representar.
- Si el backend no tiene un coleccionista con id `100`, el `POST` fallará;
  depende del seed del entorno.
- Reabrir esta decisión es obligatorio si en algún momento el alcance incorpora
  autenticación: en ese caso, sustituir el default por el id del usuario
  autenticado e idealmente encapsular el "collector actual" en una abstracción
  inyectable.

## Alternativas consideradas

- **`SessionRepository` / `CurrentCollectorProvider` inyectable:** la solución
  correcta si hubiera auth. Descartada: sin login no hay nada que proveer y
  añade complejidad sin requisito (esfuerzo L, valor nulo en el alcance actual).
- **Selector de coleccionista antes de comentar:** UX intrusiva para un curso
  sin concepto de propiedad de la cuenta; tampoco resuelve la ausencia de
  identidad real.

## Referencias

- `presentation/navigation/Destinations.kt` → `DefaultCollectorId` (KDoc),
  ruta `AddComment` y `fun addComment(albumId, collectorId = DefaultCollectorId)`.
- `presentation/viewmodel/AddCommentViewModel.kt` → `collectorId` vía
  `SavedStateHandle[Destinations.AddCommentCollectorArg]`.
- Backlog interno: ítem **B6** ("Concepto de collector/sesión actual inexistente").
