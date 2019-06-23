package com.transporter.streetglide.infrastructure.dao


import org.junit.Assert


class DaoShipmentDaoTest : DaoTest<DaoShipment, DaoShipmentDao>() {

    override val dao: DaoShipmentDao
        get() = daoSession.daoShipmentDao

    override val it: DaoShipment
        get() {
            val shipment = DaoShipment()
            shipment.consigneeName = "nehal"
            shipment.goodsPrice = 120.0
            shipment.freightChargesOnReceiver = 20.0
            shipment.freightChargesOnClient = 0.0
            shipment.street = "cairo, mokattam"
            shipment.phone = "0114225846"
            shipment.note = "after 5 pm"
            shipment.status = 0
            shipment.sheetId = 123456
            shipment.barcode = "n547af"
            shipment.areaId = 125
            shipment.areaCity = "nasr city"
            shipment.areaGovernorate = "cairo"
            shipment.areaName = "nasr city"
            shipment.apartement = "5"
            shipment.floor = 2
            shipment.pickUpId = 31
            shipment.id = 15478
            return shipment
        }

    override fun assertDeepEquals(expected: DaoShipment, actual: DaoShipment) {
        Assert.assertEquals(expected.barcode, actual.barcode)
        Assert.assertEquals(expected.status, actual.status)
    }

    override fun assertChanged(expected: DaoShipment, actual: DaoShipment) {
        Assert.assertNotEquals(expected.status, actual.status)
    }

    override fun doChange(it: DaoShipment) {
        it.status = 1
    }

}