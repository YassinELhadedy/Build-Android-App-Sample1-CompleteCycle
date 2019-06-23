package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.dto.SgDateTime.Companion.toSgDateTime
import com.transporter.streetglide.models.Sheet
import java.util.Collections.emptyList

data class SgDrs(val id: Int,
                 val barcode: SgBarcode,
                 val runnerId: Int,
                 val branchId: Int,
                 val datetime: SgDateTime) {

    companion object {
        fun Sheet.toSgDrs(): SgDrs = SgDrs(id,
                SgBarcode(barcode),
                runnerId,
                branchId,
                datetime.toSgDateTime())
    }

    fun toSheet(): Sheet = Sheet(id,
            barcode.code,
            runnerId,
            branchId,
            datetime.toDate(),
            false,
            emptyList())
}