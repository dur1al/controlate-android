package com.dur1al.controlate.porteria.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dur1al.controlate.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

/** Mensaje para el usuario expresado como recurso, resuelto en la vista (testeable sin Context). */
data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList()) {
    fun resolve(context: Context): String = context.getString(res, *args.toTypedArray())
}

/**
 * Canal de eventos de una sola vez (navegación, snackbars). A diferencia de un `StateFlow`,
 * un evento no se vuelve a entregar tras una rotación.
 */
class EventChannel<T> {
    private val channel = Channel<T>(Channel.BUFFERED)
    val events: Flow<T> = channel.receiveAsFlow()
    fun send(event: T) {
        channel.trySend(event)
    }
}

/** Recolecta [flow] solo mientras la vista está al menos en STARTED (evita trabajo en segundo plano). */
fun <T> LifecycleOwner.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

/**
 * Abre el selector de "Compartir" para un PDF de `files/reports`.
 * @return `false` si no hay ninguna app capaz de recibirlo.
 */
fun Context.shareReport(file: File): Boolean {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        startActivity(Intent.createChooser(send, getString(R.string.share_report_chooser)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Aplica formato de RUT mientras se escribe (`12.345.678-5`). */
fun EditText.applyRutFormatter(maxLength: Int = 9) {
    addTextChangedListener(RutTextWatcher(this, maxLength))
}

fun EditText.textTrimmed(): String = text?.toString().orEmpty().trim()
