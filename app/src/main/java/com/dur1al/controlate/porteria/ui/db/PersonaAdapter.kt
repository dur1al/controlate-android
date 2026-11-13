package com.dur1al.controlate.porteria.ui.db

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.databinding.ItemPersonaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona

class PersonaAdapter(
    private val onEdit: ((Persona) -> Unit)? = null,
    private val onDelete: ((Persona) -> Unit)? = null,
    private val isReadOnly: Boolean = false
) : ListAdapter<Persona, PersonaAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPersonaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(persona: Persona) {
            binding.apply {
                tvNombre.text = persona.nombreApellido
                tvPatente.text = persona.patente
                tvDepto.text = "Depto: ${persona.departamento}"
                tvDetalle.text = "${persona.descripcionAuto} | ${persona.tipoPersona ?: "Visita"}"
                
                if (!isReadOnly) {
                    root.setOnClickListener { onEdit?.invoke(persona) }
                    root.setOnLongClickListener {
                        onDelete?.invoke(persona)
                        true
                    }
                } else {
                    root.setOnClickListener(null)
                    root.setOnLongClickListener(null)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Persona>() {
        override fun areItemsTheSame(oldItem: Persona, newItem: Persona) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Persona, newItem: Persona) = oldItem == newItem
    }
}
