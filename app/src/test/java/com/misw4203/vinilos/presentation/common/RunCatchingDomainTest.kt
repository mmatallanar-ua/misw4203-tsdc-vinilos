package com.misw4203.vinilos.presentation.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RunCatchingDomainTest {
    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test fun `ok wraps value`() = runTest {
        assertEquals(DomainResult.Ok(42), runCatchingDomain { 42 })
    }

    @Test fun `404 maps to NotFound`() = runTest {
        assertEquals(DomainResult.NotFound, runCatchingDomain { throw http(404) })
    }

    @Test fun `non-404 http maps to Server`() = runTest {
        assertEquals(DomainResult.Server, runCatchingDomain { throw http(500) })
    }

    @Test fun `IOException maps to Network`() = runTest {
        assertEquals(DomainResult.Network, runCatchingDomain { throw IOException("offline") })
    }

    @Test fun `generic Exception maps to Server`() = runTest {
        assertEquals(DomainResult.Server, runCatchingDomain { throw IllegalStateException("x") })
    }

    @Test fun `CancellationException is rethrown not swallowed`() = runTest {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { runCatchingDomain { throw CancellationException() } }
        }
    }

    @Test fun `failureOrNull maps each DomainResult`() {
        assertNull(DomainResult.Ok(1).failureOrNull())
        assertEquals(DomainFailure.NETWORK, DomainResult.Network.failureOrNull())
        assertEquals(DomainFailure.NOT_FOUND, DomainResult.NotFound.failureOrNull())
        assertEquals(DomainFailure.SERVER, DomainResult.Server.failureOrNull())
    }
}
