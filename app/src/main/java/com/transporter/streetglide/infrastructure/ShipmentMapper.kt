package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.dao.DaoShipment
import com.transporter.streetglide.models.*
import java.math.BigDecimal

/**
 * Map from/to Shipment
 */
object ShipmentMapper {

    fun DaoShipment.toShipment(): Shipment = Shipment(id.toInt(),
            barcode,
            pickUpId,
            runnerId,
            ShipmentMoney(
                    BigDecimal.valueOf(goodsPrice),
                    BigDecimal.valueOf(freightChargesOnReceiver),
                    BigDecimal.valueOf(freightChargesOnClient)
            ),
            Status.values().first { it.status == status },
            type,
            note,
            null)

    fun Sheet.toDaoShipments(sheetId: Int): List<DaoShipment> {
        return consignees.flatMap { consignee: Consignee ->
            consignee.shipments.map {
                it.toDaoShipments(consignee, sheetId)
            }
        }
    }

    private fun Shipment.toDaoShipments(consignee: Consignee, sheetId: Int): DaoShipment {
        return DaoShipment(
                id.toLong(),
                barcode,
                note,
                consignee.name,
                consignee.phone,
                pickUpId,
                status.status,
                money.goodsPrice.toDouble(),
                money.freightChargesOnReceiver.toDouble(),
                money.freightChargesOnClient.toDouble(),
                consignee.address.areaId,
                consignee.address.area.name,
                consignee.address.area.city,
                consignee.address.area.governorate,
                consignee.address.street,
                consignee.address.propertyNumber,
                consignee.address.floor,
                consignee.address.apartment,
                consignee.address.specialMark,
                type,
                runnerId,
                sheetId.toLong()
        )
    }
}