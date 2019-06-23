package com.transporter.streetglide.ui.discrepancyreport

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.transporter.streetglide.R


class DiscrepancyAdapter(private val discrepancyList: MutableList<String>) : RecyclerView.Adapter<DiscrepancyAdapter.DiscrepancyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscrepancyHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_discrepancy_item, parent, false)
        return DiscrepancyHolder(v)
    }

    override fun getItemCount(): Int {
        return discrepancyList.size
    }

    override fun onBindViewHolder(holder: DiscrepancyHolder, position: Int) {
        holder.name.text = discrepancyList[position]
    }

    inner class DiscrepancyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.tv_discrepancy_consignee_name)
        private var mCheckbox: CheckBox = itemView.findViewById(R.id.checkbox_discrepancy_shipment_item)

        init {
            itemView.setOnLongClickListener {
                mCheckbox.setOnCheckedChangeListener(null)
                mCheckbox.isChecked = !mCheckbox.isChecked
                true
            }
        }
    }
}