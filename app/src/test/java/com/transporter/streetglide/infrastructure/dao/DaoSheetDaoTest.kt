package com.transporter.streetglide.infrastructure.dao

import org.junit.Assert
import java.util.*


class DaoSheetDaoTest : DaoTest<DaoSheet, DaoSheetDao>() {

    override val dao: DaoSheetDao
        get() = daoSession.daoSheetDao

    override val it: DaoSheet
        get() {
            val sheet = DaoSheet()
            sheet.branchId = 25
            sheet.dateTime = Date()
            sheet.id = 12345
            sheet.barcode = "125ss8"
            sheet.runnerId = 121
            return sheet
        }

    override fun assertDeepEquals(expected: DaoSheet, actual: DaoSheet) {
        Assert.assertEquals(expected.barcode, actual.barcode)
        Assert.assertEquals(expected.runnerId, actual.runnerId)
    }

    override fun assertChanged(expected: DaoSheet, actual: DaoSheet) {
        Assert.assertNotEquals(expected.runnerId, actual.runnerId)
    }

    override fun doChange(it: DaoSheet) {
        it.runnerId = 547892
    }
}