package com.dur1al.controlate.porteria.ui.bicicletero

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemBicicletaBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta

/**
 * Adapter para la lista de registros de bicicletas.
 * Extraído de [BicicleteroDashboardActivity] para reutilización y testeo.
 */
class BicicletaAdapter(
    private val onSalidaClick: (RegistroBicicleta) -> Unit
) : ListAdapter<RegistroBicicleta, BicicletaAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBicicletaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onSalidaClick)
    }

    class ViewHolder(
        private val binding: ItemBicicletaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegistroBicicleta, onSalidaClick: (RegistroBicicleta) -> Unit) {
            val context = binding.root.context
            binding.tvNombre.text = context.getString(
                R.string.nombre_rut_format,
                item.nombre,
                item.rut
            )
            binding.tvCarrera.text = context.getString(
                R.string.bike_detail_format,
                item.carrera,
                item.tipoPersona
            )
            binding.tvEntrada.text = context.getString(R.string.entrada_format, item.horaEntrada)
            binding.tvSalida.text = context.getString(
                R.string.salida_format,
                item.horaSalida ?: "---"
            )
            binding.root.setOnClickListener { onSalidaClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RegistroBicicleta>() {
        override fun areItemsTheSame(
            old: RegistroBicicleta,
            new: RegistroBicicleta
        ): Boolean = old.id == new.id

        override fun areContentsTheSame(
            old: RegistroBicicleta,
            new: RegistroBicicleta
        ): Boolean = old == new
    }
}
