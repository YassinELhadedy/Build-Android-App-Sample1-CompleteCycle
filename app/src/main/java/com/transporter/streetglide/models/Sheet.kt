package com.transporter.streetglide.models


import android.support.annotation.VisibleForTesting
import java.util.*


// FIXME: The sheet misses a lot of fields.
data class Sheet(val id: Int,
                 val barcode: String,
                 val runnerId: Int,
                 val branchId: Int,
                 val datetime: Date, /* Had to use java.util.Date instead of
                                      * LocalDateTime because Android API
                                      * level 21 does not support LocalDateTime
                                      */
                 val isReturned: Boolean = false,
                 val consignees: List<Consignee>) {

    // I make it return consignee to support scan to search shipment which will navigate a runner to consignee details page
    @VisibleForTesting
    fun findConsigneeByBarcodeOfShipment(barcode: String): Consignee? {
        return consignees.firstOrNull {
            it.findShipmentByBarcode(barcode) != null
        }
    }

    fun filterConsigneeByStatusOfShipment(status: Status): List<Consignee> {
        return consignees.filter { consignee ->
            consignee.shipments.any { it.status == status }
        }.map { it.copy(shipments = it.shipments.filter { it.status == status }) }
    }

    fun hasShipment(barcode: String): Boolean = consignees.any { it.hasShipment(barcode) }
}