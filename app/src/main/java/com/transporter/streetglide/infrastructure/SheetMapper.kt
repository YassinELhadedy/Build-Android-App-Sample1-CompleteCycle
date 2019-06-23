package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.ShipmentMapper.toShipment
import com.transporter.streetglide.infrastructure.dao.DaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoShipment
import com.transporter.streetglide.models.Address
import com.transporter.streetglide.models.Area
import com.transporter.streetglide.models.Consignee
import com.transporter.streetglide.models.Sheet

/**
 * Map from/to Sheet
 */
object SheetMapper {
    fun DaoSheet.toSheet(): Sheet = Sheet(
            id.toInt(),
            barcode,
            runnerId,
            branchId,
            dateTime,
            false,
            shipments.groupBy {
                it.phone
            }.map {
                toConsignee(it.value)
            }
    )

    fun Sheet.toDaoSheet(): DaoSheet = DaoSheet(id.toLong(),
            runnerId,
            branchId,
            barcode,
            datetime)

    private fun toConsignee(shipments: List<DaoShipment>): Consignee {
        return Consignee(shipments[0].consigneeName,
                Address(shipments[0].areaId, Area(shipments[0].areaId, shipments[0].areaName, shipments[0].areaCity, shipments[0].areaGovernorate),
                        shipments[0].street, shipments[0].propertyNumber, shipments[0].floor,
                        shipments[0].apartement, shipments[0].specialMark), shipments[0].phone,
                shipments.map {
                    it.toShipment()
                })
    }
}