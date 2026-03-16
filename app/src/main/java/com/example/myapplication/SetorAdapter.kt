package com.example.myapplication

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Locale

class SetorAdapter(
    private val onItemClick: (Setor) -> Unit,
    private val onItemLongClick: (Setor) -> Unit
) : ListAdapter<Setor, SetorAdapter.SetorViewHolder>(SetorDiffCallback()) {

    class SetorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vStatusIndicator: View = view.findViewById(R.id.vStatusIndicator)
        val tvNome: TextView = view.findViewById(R.id.tvNome)
        val tvIp: TextView = view.findViewById(R.id.tvIp)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvLatencia: TextView = view.findViewById(R.id.tvLatencia)
        val tvUltimaVerificacao: TextView = view.findViewById(R.id.tvUltimaVerificacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_setor, parent, false)
        return SetorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetorViewHolder, position: Int) {
        val setor = getItem(position)
        holder.tvNome.text = setor.nome
        
        val uptimeText = String.format(Locale.getDefault(), " %.1f%%", setor.uptimePercent)
        holder.tvIp.text = "${setor.ip} | Uptime:$uptimeText"
        
        holder.tvCategoria.text = setor.categoria
        
        val colorCat = when (setor.categoria) {
            "Servidores" -> "#E3F2FD"
            "Câmeras" -> "#FBE9E7"
            "Roteadores" -> "#E8F5E9"
            "Smartphone" -> "#F3E5F5"
            "Computador" -> "#FFF3E0"
            else -> "#F5F5F5"
        }
        holder.tvCategoria.background.setTint(Color.parseColor(colorCat))

        if (setor.isOnline && setor.latencia >= 0) {
            holder.tvLatencia.visibility = View.VISIBLE
            holder.tvLatencia.text = "${setor.latencia}ms"
        } else {
            holder.tvLatencia.visibility = View.GONE
        }
        
        val colorStatus = if (setor.isOnline) Color.GREEN else Color.RED
        holder.vStatusIndicator.background.setTint(colorStatus)

        if (setor.ultimaVerificacao > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = setor.ultimaVerificacao }
            val time = DateFormat.format("HH:mm:ss", cal).toString()
            holder.tvUltimaVerificacao.text = "Última verificação: $time"
        } else {
            holder.tvUltimaVerificacao.text = "Nunca verificado"
        }

        holder.itemView.setOnClickListener {
            onItemClick(setor)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(setor)
            true
        }
    }

    class SetorDiffCallback : DiffUtil.ItemCallback<Setor>() {
        override fun areItemsTheSame(oldItem: Setor, newItem: Setor): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Setor, newItem: Setor): Boolean = oldItem == newItem
    }
}
