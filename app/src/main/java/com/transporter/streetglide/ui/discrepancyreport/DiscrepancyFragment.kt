package com.transporter.streetglide.ui.discrepancyreport

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.transporter.streetglide.R


class DiscrepancyFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_discrepancy, container, false)
        val rv = rootView.findViewById<RecyclerView>(R.id.rv_discrepancy_discrepancy_shipments)
        rv.setHasFixedSize(true)
        val adapter = DiscrepancyAdapter(mutableListOf("يس الحديدي صالح", "نهال بكر", "نهال زكريا", "شاهندا الديب", "شاهندا الديب", "يحي الهادي", "يحي الهادي"))
        rv.adapter = adapter
        val llm = LinearLayoutManager(activity)
        rv.layoutManager = llm

        return rootView
    }
}