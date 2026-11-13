package com.dur1al.controlate.porteria.ui.examen

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.DialogExamenResultadoBinding
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.domain.time.TimeFormat

/**
 * Resultado del intento. Al cerrarse termina la Activity del examen; se resuelve en
 * [onDismiss] para que funcione también tras una rotación (un callback en memoria se perdería).
 */
class ExamenResultadoDialogFragment : DialogFragment() {

    private var _binding: DialogExamenResultadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogExamenResultadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val aprobado = args.getBoolean(ARG_APROBADO)
        val tiempo = args.getLong(ARG_TIEMPO)

        binding.tvPuntaje.text = getString(R.string.examen_resultado_puntaje, args.getInt(ARG_PUNTAJE), args.getInt(ARG_TOTAL))
        binding.tvPorcentaje.text = getString(R.string.examen_resultado_porcentaje, args.getFloat(ARG_PORCENTAJE))
        binding.tvGuardiaNombre.text = getString(R.string.examen_resultado_guardia, args.getString(ARG_GUARDIA).orEmpty())
        binding.tvFecha.text = getString(R.string.examen_resultado_fecha, TimeFormat.displayDateTime(args.getLong(ARG_FECHA)))
        binding.tvTiempo.text = getString(R.string.examen_resultado_tiempo, tiempo / 60, tiempo % 60)
        binding.tvEstadoBadge.setText(if (aprobado) R.string.examen_aprobado else R.string.examen_reprobado)
        binding.tvEstadoBadge.background = ContextCompat.getDrawable(
            requireContext(), if (aprobado) R.drawable.bg_status_inside else R.drawable.bg_status_exited
        )
        binding.tvPorcentaje.setTextColor(
            ContextCompat.getColor(requireContext(), if (aprobado) R.color.bicicletero1_primary else R.color.error_red)
        )
        binding.btnCerrarResultado.setOnClickListener { dismiss() }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ExamenResultadoDialog"
        private const val ARG_PUNTAJE = "puntaje"
        private const val ARG_TOTAL = "total"
        private const val ARG_PORCENTAJE = "porcentaje"
        private const val ARG_APROBADO = "aprobado"
        private const val ARG_GUARDIA = "guardia"
        private const val ARG_FECHA = "fecha"
        private const val ARG_TIEMPO = "tiempo"

        fun newInstance(r: ExamenResultado) = ExamenResultadoDialogFragment().apply {
            arguments = bundleOf(
                ARG_PUNTAJE to r.puntaje,
                ARG_TOTAL to r.totalPreguntas,
                ARG_PORCENTAJE to r.porcentaje,
                ARG_APROBADO to r.aprobado,
                ARG_GUARDIA to r.guardiaNombre,
                ARG_FECHA to r.realizadoAt,
                ARG_TIEMPO to r.tiempoEmpleadoSegundos
            )
        }
    }
}
