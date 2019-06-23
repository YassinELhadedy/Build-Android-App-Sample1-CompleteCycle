package com.transporter.streetglide.models

import java.math.BigDecimal


// FIXME: The shipment misses a lot of fields.
data class Shipment(val id: Int, val barcode: String, val pickUpId: Int?,
                    val runnerId: Int?, val money: ShipmentMoney,
                    val status: Status, val type: Int, // FIXME: Both should be enums
                    val note: String?, val reason: String?)

data class ShipmentMoney(val goodsPrice: BigDecimal, // We may face issues when using BigDecimal.
                         val freightChargesOnReceiver: BigDecimal,
                         val freightChargesOnClient: BigDecimal)

enum class Status(val status: Int) {
    OutForDelivery(4),
    Delivered(13),
    RefusedReceive(8),
    DelayedCustomerRequest(2),
    NotAvailable(16)
}