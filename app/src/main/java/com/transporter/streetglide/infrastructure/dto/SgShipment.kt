package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.dto.SgArea.Companion.toSgArea
import com.transporter.streetglide.models.Consignee
import com.transporter.streetglide.models.Shipment
import com.transporter.streetglide.models.Status


data class SgShipment(val id: Int,
                      val barcode: SgBarcode,
                      val pickUpId: Int?,
                      val consigneeName: String,
                      val consigneeAddress: SgAddress,
                      val runnerId: Int?,
                      val phone: String,
                      val money: SgShipmentMoney,
                      val shipmentFlags: SgShipmentFlags,
                      val note: String?,
                      val reason: String?) {

    companion object {
        fun Consignee.toSgShipmentsWithPickUp(consignee: Consignee): List<SgShipmentWithPickUp> {
            return shipments.map {
                it.toShipmentWithPickUp(consignee)
            }
        }

        private fun Shipment.toShipmentWithPickUp(consignee: Consignee): SgShipmentWithPickUp {
            return SgShipmentWithPickUp(SgShipment(id,
                    SgBarcode(barcode),
                    pickUpId,
                    consignee.name,
                    SgAddress(
                            consignee.address.areaId,
                            consignee.address.area.toSgArea(),
                            consignee.address.street,
                            consignee.address.propertyNumber,
                            consignee.address.floor,
                            consignee.address.apartment,
                            consignee.address.specialMark
                    ),
                    runnerId,
                    consignee.phone,
                    SgShipmentMoney(money.goodsPrice, money.freightChargesOnReceiver, money.freightChargesOnClient),
                    SgShipmentFlags(status.status, type),
                    note,
                    reason)
            )
        }
    }

    fun toShipment(): Shipment {
        return Shipment(id,
                barcode.code,
                pickUpId,
                runnerId,
                money.toShipmentMoney(),
                Status.values().first { it.status == shipmentFlags.status },
                shipmentFlags.shipmentType,
                note,
                reason)
    }
}