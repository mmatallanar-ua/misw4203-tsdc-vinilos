package com.misw4203.vinilos.presentation.common

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Resultado clasificado de una operación de datos, sin filtrar tipos de red a los VMs. */
sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>
    data object Network : DomainResult<Nothing>
    data object NotFound : DomainResult<Nothing>
    data object Server : DomainResult<Nothing>
}

/**
 * Ejecuta [block] y clasifica el fallo en el orden canónico único:
 * Cancellation (relanzada) → HttpException (404 ⇒ NotFound, resto ⇒ Server)
 * → IOException (Network) → Exception (Server).
 * Centraliza el try/catch que antes estaba duplicado y con deriva en los VMs.
 */
suspend inline fun <T> runCatchingDomain(block: () -> T): DomainResult<T> =
    try {
        DomainResult.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() == 404) DomainResult.NotFound else DomainResult.Server
    } catch (e: IOException) {
        DomainResult.Network
    } catch (e: Exception) {
        DomainResult.Server
    }
