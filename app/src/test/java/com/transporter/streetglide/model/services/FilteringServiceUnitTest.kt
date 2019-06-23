package com.transporter.streetglide.model.services

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.services.FilteringService
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class FilteringServiceUnitTest(private val setupTestParameter: SetupTestParameter) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter {
            override fun setup(): TestParameter {
                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val config = Configuration(SignedToken("jdhhfhf"), 1, "122")
                val jsonString = Gson().toJson(config)
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configRepository = ConfigurationRepository(sharedPreference)
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)
                val filteringService = FilteringService(sheetDiskRepository)

                val sheet = Sheet(1, "12346", 4, 6, Date(), false,
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
                                                null, null))),
                                Consignee("name",
                                        Address(5, Area(5, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                                "street",
                                                null,
                                                null,
                                                null,
                                                null), "01145", listOf(
                                        Shipment(6,
                                                "43251",
                                                4,
                                                4,
                                                ShipmentMoney(BigDecimal("100.0"),
                                                        BigDecimal("20.0"),
                                                        BigDecimal("0.0")),
                                                Status.NotAvailable,
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

                return object : TestParameter {
                    override val data: List<Consignee>
                        get() = listConsignees

                    override fun setup(): Observable<out Sheet> {
                        return sheetDiskRepository.insert(sheet)
                    }

                    override fun filterConsigneesByExistingStatus(): Observable<List<Consignee>> {
                        return filteringService.filterConsignees(Status.OutForDelivery)
                    }

                    override fun filterConsigneesByNonExistingStatus(): Observable<List<Consignee>> {
                        return filteringService.filterConsignees(Status.DelayedCustomerRequest)
                    }
                }
            }

            override fun toString(): String = FilteringService::class.java.simpleName!!
        }))
    }

    @Test
    fun testFilteringConsigneesWithShipmentsWithExistingStatus() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { _ ->
            testParameter.filterConsigneesByExistingStatus()
        }.subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(testParameter.data)
    }

    @Test
    fun testFilterConsigneesByNonExistingStatus() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { _ ->
            testParameter.filterConsigneesByNonExistingStatus()
        }.subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(emptyList<Consignee>())
    }

    interface TestParameter {
        fun setup(): Observable<out Sheet>
        fun filterConsigneesByExistingStatus(): Observable<List<Consignee>>
        fun filterConsigneesByNonExistingStatus(): Observable<List<Consignee>>
        val data: List<Consignee>
    }

    interface SetupTestParameter {
        fun setup(): TestParameter
    }
}