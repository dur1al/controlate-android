package com.dur1al.controlate.porteria.ui.db

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemPersonaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.ui.common.labelRes

class PersonaAdapter(
    private val onEdit: (Persona) -> Unit,
    private val onDelete: (Persona) -> Unit
) : ListAdapter<Persona, PersonaAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPersonaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemPersonaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(persona: Persona) = with(binding) {
            val context = root.context
            tvNombre.text = persona.nombreApellido
            tvPatente.text = persona.patente
            tvDepto.text = context.getString(R.string.label_department_format, persona.departamento)
            tvDetalle.text = context.getString(
                R.string.persona_detail_format,
                persona.descripcionAuto,
                context.getString(PersonaType.fromCode(persona.tipoPersona).labelRes)
            )
            root.setOnClickListener { onEdit(persona) }
            root.setOnLongClickListener {
                onDelete(persona)
                true
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Persona>() {
        override fun areItemsTheSame(old: Persona, new: Persona) = old.id == new.id
        override fun areContentsTheSame(old: Persona, new: Persona) = old == new
    }
}
