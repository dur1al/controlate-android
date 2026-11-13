package com.dur1al.controlate.porteria.ui.directory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.databinding.ItemPersonaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona

class PersonaAdapter(
    private val onItemClick: (Persona) -> Unit,
    private val onLongClick: (Persona) -> Unit
) : ListAdapter<Persona, PersonaAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPersonaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Persona) {
            binding.apply {
                tvPatente.text = item.patente
                tvNombre.text = item.nombreApellido
                tvDepto.text = "Depto: ${item.departamento}"
                tvDetalle.text = "${item.descripcionAuto} | ${item.empresa ?: "Particular"}"
                
                root.setOnClickListener { onItemClick(item) }
                root.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Persona>() {
        override fun areItemsTheSame(oldItem: Persona, newItem: Persona): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Persona, newItem: Persona): Boolean {
            return oldItem == newItem
        }
    }
}
