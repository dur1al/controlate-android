package com.dur1al.controlate.porteria.ui.examen

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.DialogExamenResultadoBinding
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import java.util.Locale

class ExamenResultadoDialogFragment : DialogFragment() {

    private var _binding: DialogExamenResultadoBinding? = null
    private val binding get() = _binding!!

    private var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExamenResultadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val puntaje = arguments?.getInt(ARG_PUNTAJE) ?: 0
        val total = arguments?.getInt(ARG_TOTAL) ?: 60
        val porcentaje = arguments?.getFloat(ARG_PORCENTAJE) ?: 0f
        val aprobado = arguments?.getBoolean(ARG_APROBADO) ?: false
        val guardia = arguments?.getString(ARG_GUARDIA) ?: "Guardia OS-10"
        val fecha = arguments?.getString(ARG_FECHA) ?: ""
        val tiempoSegs = arguments?.getLong(ARG_TIEMPO) ?: 0L

        binding.tvPuntaje.text = String.format(Locale.getDefault(), "%d / %d", puntaje, total)
        binding.tvPorcentaje.text = String.format(Locale.getDefault(), "%.1f%%", porcentaje)
        binding.tvGuardiaNombre.text = String.format(Locale.getDefault(), "Guardia: %s", guardia)
        binding.tvFecha.text = String.format(Locale.getDefault(), "Fecha: %s", fecha)

        val mins = tiempoSegs / 60
        val segs = tiempoSegs % 60
        binding.tvTiempo.text = String.format(Locale.getDefault(), "Tiempo empleado: %d min %d seg", mins, segs)

        if (aprobado) {
            binding.tvEstadoBadge.text = "APROBADO"
            binding.tvEstadoBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_inside)
            binding.tvPorcentaje.setTextColor(ContextCompat.getColor(requireContext(), R.color.bicicletero1_primary))
        } else {
            binding.tvEstadoBadge.text = "REPROBADO"
            binding.tvEstadoBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_exited)
            binding.tvPorcentaje.setTextColor(Color.parseColor("#E53935"))
        }

        binding.btnCerrarResultado.setOnClickListener {
            dismiss()
            onDismissCallback?.invoke()
        }
    }

    fun setOnDismissListener(callback: () -> Unit) {
        this.onDismissCallback = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExamenResultadoDialogFragment"
        private const val ARG_PUNTAJE = "ARG_PUNTAJE"
        private const val ARG_TOTAL = "ARG_TOTAL"
        private const val ARG_PORCENTAJE = "ARG_PORCENTAJE"
        private const val ARG_APROBADO = "ARG_APROBADO"
        private const val ARG_GUARDIA = "ARG_GUARDIA"
        private const val ARG_FECHA = "ARG_FECHA"
        private const val ARG_TIEMPO = "ARG_TIEMPO"

        fun newInstance(resultado: ExamenResultado): ExamenResultadoDialogFragment {
            val fragment = ExamenResultadoDialogFragment()
            val args = Bundle().apply {
                putInt(ARG_PUNTAJE, resultado.puntaje)
                putInt(ARG_TOTAL, resultado.totalPreguntas)
                putFloat(ARG_PORCENTAJE, resultado.porcentaje)
                putBoolean(ARG_APROBADO, resultado.aprobado)
                putString(ARG_GUARDIA, resultado.guardiaNombre)
                putString(ARG_FECHA, resultado.fecha)
                putLong(ARG_TIEMPO, resultado.tiempoEmpleadoSegundos)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
