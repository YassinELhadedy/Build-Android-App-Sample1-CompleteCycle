package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.models.ShipmentMoney
import java.math.BigDecimal


data class SgShipmentMoney(val goodsPrice: BigDecimal,
                           val freightChargesOnReceiver: BigDecimal,
                           val freightChargesOnClient: BigDecimal) {
    companion object {
        fun ShipmentMoney.toSgShipmentMoney(): SgShipmentMoney = SgShipmentMoney(goodsPrice,
                freightChargesOnReceiver,
                freightChargesOnClient)
    }

    fun toShipmentMoney(): ShipmentMoney = ShipmentMoney(goodsPrice,
            freightChargesOnReceiver,
            freightChargesOnClient)
}