package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.dao.DaoScannedBarcode
import com.transporter.streetglide.models.ScannedBarcode


object ScannedBarcodeMapper {

    fun DaoScannedBarcode.toScannedBarcode(): ScannedBarcode = ScannedBarcode(id.toInt(),
            barcode,
            found)

    fun ScannedBarcode.toDaoScannedBarcode(): DaoScannedBarcode = DaoScannedBarcode(id.toLong(),
            barcode,
            found)
}