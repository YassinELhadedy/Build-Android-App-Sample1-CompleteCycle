package com.transporter.streetglide.model

import com.transporter.streetglide.models.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.*



@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class SheetUnitTest(private val setupTestParameter: SetupTestParameter) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter {
            override fun setup(): TestParameter {

                val sheet = Sheet(2, "12346", 4, 6, Date(), false,
                        consignees = listOf(Consignee("name",
                                Address(5, Area(5, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                        "street", null, null, null, null), "65432",
                                listOf(Shipment(3, "4321", 4,
                                        4,
                                        ShipmentMoney(BigDecimal("100.0"), BigDecimal("20.0"),
                                                BigDecimal("0.0")),
                                        Status.OutForDelivery, 1, null, null))),
                                Consignee("name",
                                        Address(5, Area(5, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                                "street",
                                                null,
                                                null,
                                                null,
                                                null), "0125", listOf(
                                        Shipment(4,
                                                "43211",
                                                4,
                                                4,
                                                ShipmentMoney(BigDecimal("100.0"),
                                                        BigDecimal("20.0"),
                                                        BigDecimal("0.0")),
                                                Status.Delivered,
                                                1,
                                                null, null),
                                        Shipment(5,
                                                "43212",
                                                4,
                                                4,
                                                ShipmentMoney(BigDecimal("100.0"),
                                                        BigDecimal("20.0"),
                                                        BigDecimal("0.0")),
                                                Status.OutForDelivery,
                                                1,
                                                null, null)))))

                val listConsignees = listOf(sheet.consignees[0], Consignee("name",
                        Address(5, Area(5, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                "street",
                                null,
                                null,
                                null,
                                null), "0125", listOf(
                        Shipment(5,
                                "43212",
                                4,
                                4,
                                ShipmentMoney(BigDecimal("100.0"),
                                        BigDecimal("20.0"),
                                        BigDecimal("0.0")),
                                Status.OutForDelivery,
                                1, null, null))))

                val sheetWithEmptyShipment = sheet.copy(consignees = emptyList())
                val barcode = "43212"
                val nonExistingBarcode = "12345"

                return object : TestParameter {

                    override val data: List<Consignee>
                        get() = listConsignees

                    override fun filterConsigneesByExistingStatus(): List<Consignee> {
                        return sheet.filterConsigneeByStatusOfShipment(Status.OutForDelivery)
                    }

                    override fun filterConsigneesByNonExistingStatus(): List<Consignee> {
                        return sheet.filterConsigneeByStatusOfShipment(Status.DelayedCustomerRequest)
                    }

                    override fun searchShipmentExiting(): Consignee? {
                        return sheet.findConsigneeByBarcodeOfShipment(barcode)
                    }

                    override fun searchShipmentNonExiting(): Consignee? {
                        return sheet.findConsigneeByBarcodeOfShipment(nonExistingBarcode)
                    }

                    override fun searchShipmentInEmptyList(): Consignee? {
                        return sheetWithEmptyShipment.findConsigneeByBarcodeOfShipment(barcode)
                    }

                    override fun checkExitingSearch() {
                        val consignee = searchShipmentExiting()
                        Assert.assertNotNull(consignee)
                        Assert.assertEquals(barcode, consignee?.findShipmentByBarcode(barcode)?.barcode)
                    }

                    override fun checkNonExitingSearch(isEmpty: Boolean) {
                        val consignee = if (isEmpty) {
                            searchShipmentInEmptyList()
                        } else {
                            searchShipmentNonExiting()
                        }
                        Assert.assertNull(consignee)
                    }
                }
            }

            override fun toString(): String = Sheet::class.java.simpleName!!
        }))
    }

    @Test
    fun testFilteringConsigneesWithShipmentsWithExistingStatus() {
        val testParameter = setupTestParameter.setup()
        val consigneeList = testParameter.filterConsigneesByExistingStatus()
        Assert.assertNotNull(consigneeList)
        Assert.assertEquals(2, consigneeList.size)
        Assert.assertEquals(listOf(Status.OutForDelivery, Status.OutForDelivery), consigneeList.flatMap { it.shipments.map { it.status } })
        Assert.assertEquals(testParameter.data, consigneeList)
    }

    @Test
    fun testFilteringConsigneesWithShipmentsWithNonExistingStatus() {
        val testParameter = setupTestParameter.setup()
        val consigneeList = testParameter.filterConsigneesByNonExistingStatus()
        Assert.assertNotNull(consigneeList)
        Assert.assertEquals(emptyList<Consignee>(), consigneeList)
    }

    @Test
    fun testFoundExistingShipmentInSheet() {
        val testParameter = setupTestParameter.setup()
        testParameter.searchShipmentExiting()
        testParameter.checkExitingSearch()
    }

    @Test
    fun testNotFoundNonExistingShipmentInSheet() {
        val testParameter = setupTestParameter.setup()
        testParameter.searchShipmentNonExiting()
        testParameter.checkNonExitingSearch(false)
    }

    @Test
    fun testNotFoundShipmentInSheetHasNotShipments() {
        val testParameter = setupTestParameter.setup()
        testParameter.searchShipmentInEmptyList()
        testParameter.checkNonExitingSearch(true)
    }

    interface TestParameter {
        fun searchShipmentExiting(): Consignee?
        fun searchShipmentNonExiting(): Consignee?
        fun searchShipmentInEmptyList(): Consignee?
        fun checkExitingSearch()
        fun checkNonExitingSearch(isEmpty: Boolean)
        fun filterConsigneesByExistingStatus(): List<Consignee>
        fun filterConsigneesByNonExistingStatus(): List<Consignee>
        val data: List<Consignee>
    }

    interface SetupTestParameter {
        fun setup(): TestParameter
    }
}