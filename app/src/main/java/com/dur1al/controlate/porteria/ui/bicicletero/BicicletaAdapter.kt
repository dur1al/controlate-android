package com.dur1al.controlate.porteria.ui.bicicletero

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemBicicletaBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.report.ReportDocument
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.domain.validation.Rut
import com.dur1al.controlate.porteria.ui.common.labelRes

/** Lista de bicicletas del turno. Tocar un registro sin salida la marca (con deshacer en la Activity). */
class BicicletaAdapter(
    private val onExitClick: (RegistroBicicleta) -> Unit
) : ListAdapter<RegistroBicicleta, BicicletaAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBicicletaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemBicicletaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegistroBicicleta) = with(binding) {
            val context = root.context
            tvNombre.text = context.getString(R.string.nombre_rut_format, item.nombre, Rut.format(item.rut))
            tvCarrera.text = context.getString(
                R.string.bike_detail_format, item.carrera, context.getString(PersonaType.fromCode(item.tipoPersona).labelRes)
            )
            tvEntrada.text = context.getString(R.string.entrada_format, TimeFormat.time(item.entradaAt))
            tvSalida.text = context.getString(
                R.string.salida_format, item.salidaAt?.let { TimeFormat.time(it) } ?: ReportDocument.NO_TIME
            )
            val inside = item.salidaAt == null
            root.isClickable = inside
            root.contentDescription = if (inside) context.getString(R.string.bike_tap_to_exit) else null
            root.setOnClickListener(if (inside) ({ onExitClick(item) }) else null)
        }
    }

    private object Diff : DiffUtil.ItemCallback<RegistroBicicleta>() {
        override fun areItemsTheSame(old: RegistroBicicleta, new: RegistroBicicleta) = old.id == new.id
        override fun areContentsTheSame(old: RegistroBicicleta, new: RegistroBicicleta) = old == new
    }
}
