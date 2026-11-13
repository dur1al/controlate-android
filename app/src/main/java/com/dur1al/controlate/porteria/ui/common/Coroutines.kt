package com.dur1al.controlate.porteria.ui.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Como `runCatching`, pero sin tragarse la cancelación de corrutinas: capturar
 * [CancellationException] rompe la concurrencia estructurada.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
