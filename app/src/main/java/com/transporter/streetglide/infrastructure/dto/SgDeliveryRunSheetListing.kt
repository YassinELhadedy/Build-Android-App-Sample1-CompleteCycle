package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.dto.SgDrs.Companion.toSgDrs
import com.transporter.streetglide.infrastructure.dto.SgShipment.Companion.toSgShipmentsWithPickUp
import com.transporter.streetglide.models.Consignee
import com.transporter.streetglide.models.Sheet

data class SgDeliveryRunSheetListing(val deliveryRunSheet: SgDrs,
                                     val returned: Int?,
                                     val shipments: List<SgShipmentWithPickUp>) {
    companion object {
        fun Sheet.toSgDeliveryRunSheetListing(): SgDeliveryRunSheetListing {
            return SgDeliveryRunSheetListing(
                    toSgDrs(),
                    null,
                    consignees.flatMap {
                        it.toSgShipmentsWithPickUp(it)
                    }
            )
        }
    }

    fun toSheet(): Sheet {
        return Sheet(deliveryRunSheet.id,
                deliveryRunSheet.barcode.code,
                deliveryRunSheet.runnerId,
                deliveryRunSheet.branchId,
                deliveryRunSheet.datetime.toDate(),
                returned != null,
                shipments.groupBy {
                    it.shipment.phone
                }.map { toConsignee(it.value) }
        )
    }

    private fun toConsignee(shipments: List<SgShipmentWithPickUp>): Consignee {
        return Consignee(shipments[0].shipment.consigneeName,
                shipments[0].shipment.consigneeAddress.toAddress(),
                shipments[0].shipment.phone,
                shipments.map {
                    it.shipment.toShipment()
                }
        )
    }
}