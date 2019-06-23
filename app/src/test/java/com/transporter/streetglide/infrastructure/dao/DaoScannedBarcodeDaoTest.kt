package com.transporter.streetglide.infrastructure.dao

import org.junit.Assert



class DaoScannedBarcodeDaoTest : DaoTest<DaoScannedBarcode, DaoScannedBarcodeDao>() {
    override val dao: DaoScannedBarcodeDao
        get() = daoSession.daoScannedBarcodeDao

    override val it: DaoScannedBarcode
        get() {
            val scannedBarcode = DaoScannedBarcode()
            scannedBarcode.barcode = "123456789"
            scannedBarcode.found = true
            return scannedBarcode
        }

    override fun assertDeepEquals(expected: DaoScannedBarcode, actual: DaoScannedBarcode) {
        Assert.assertEquals(expected.barcode, actual.barcode)
        Assert.assertEquals(expected.found, actual.found)
    }

    override fun assertChanged(expected: DaoScannedBarcode, actual: DaoScannedBarcode) {
        Assert.assertNotEquals(expected.found, actual.found)
    }

    override fun doChange(it: DaoScannedBarcode) {
        it.found = false
    }
}