package com.transporter.streetglide.models

/**
 *Created by yassin on 4/28/18.
 */
data class Consignee(val name: String, val address: Address, val phone: String, val shipments: List<Shipment>) {

    fun findShipmentByBarcode(barcode: String): Shipment? {
        return shipments.firstOrNull { it.barcode == barcode }
    }

    fun hasShipment(barcode: String): Boolean = shipments.any { it.barcode == barcode }
}