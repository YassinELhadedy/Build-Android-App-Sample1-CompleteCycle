package com.transporter.streetglide.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.ScannedBarcodeRepositoryImp
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.services.ValidationService
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
class ValidationServiceUnitTest(private val setupTestParameter: SetupTestParameter) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter {
            override fun setup(): TestParameter {
                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val config = Configuration(SignedToken(""), 1, "123")
                val jsonString = Gson().toJson(config)
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configRepository = ConfigurationRepository(sharedPreference)
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)
                val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)
                val validationService = ValidationService(sheetDiskRepository, scannedBarcodeRepository)

                val sheet = Sheet(1, "12346", 4, 6, Date(), false, consignees = listOf(
                        Consignee("name",
                                Address(5,
                                        Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                        "street", null, null, null, null),
                                "65432",
                                listOf(
                                        Shipment(3, "4321",
                                                4, 4,
                                                ShipmentMoney(
                                                        BigDecimal("100.0"), BigDecimal("20.0"), BigDecimal("0.0")
                                                ), Status.Delivered, 1, null, null
                                        ))
                        ), Consignee("name",
                        Address(5, Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                "street",
                                null,
                                null,
                                null,
                                null), "", listOf(
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
                                Status.Delivered,
                                1,
                                null, null)))))
                val existingBarcode = "43212"
                val existingButNotDuplicated = "4321"
                val nonExistingBarcode = "12345"
                val reversedBarcode = "1234"

                val scannedBarcode = listOf(ScannedBarcode(nonExistingBarcode.toInt(), nonExistingBarcode, false),
                        ScannedBarcode(existingButNotDuplicated.toInt(), existingButNotDuplicated, true))

                return object : TestParameter {
                    override val data: List<ScannedBarcode>
                        get() = scannedBarcode

                    override fun setup(): Observable<Unit> =
                            sheetDiskRepository.insert(sheet).flatMap {
                                validationService.saveScannedBarcode(existingBarcode, true)
                            }.flatMap {
                                validationService.saveScannedBarcode(reversedBarcode, true)
                            }.map { Unit }

                    override fun foundInSheetAndNotDuplicateScannedBarcode(): Observable<Pair<Boolean, Boolean>> =
                            validationService.verifyBarcode(existingButNotDuplicated)

                    override fun duplicatedScannedBarcodeAndNotInSheet(): Observable<Pair<Boolean, Boolean>> =
                            validationService.verifyBarcode(reversedBarcode)

                    override fun duplicatedScannedBarcodeAndFoundInSheet(): Observable<Pair<Boolean, Boolean>> =
                            validationService.verifyBarcode(existingBarcode)

                    override fun notFoundInSheetAndNotDuplicateScannedBarcode(): Observable<Pair<Boolean, Boolean>> =
                            validationService.verifyBarcode(nonExistingBarcode)

                    override fun checkSaveNonExistingBarcode(): Observable<out List<ScannedBarcode>> =
                            scannedBarcodeRepository.getAll(Pagination((Condition("barcode", Operator.Equal, nonExistingBarcode))))

                    override fun checkSaveExistingBarcode(): Observable<out List<ScannedBarcode>> =
                            scannedBarcodeRepository.getAll(Pagination((Condition("barcode", Operator.Equal, existingButNotDuplicated))))

                    override fun getCountScannedShipments(): Observable<Long> {
                        return validationService.getScannedShipmentsCount()
                    }
                }
            }

            override fun toString(): String = ValidationService::class.java.simpleName!!
        }))
    }

    @Test
    fun testShipmentIsFoundInSheetAndNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { testParameter.foundInSheetAndNotDuplicateScannedBarcode() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(Pair(false, true))
    }

    @Test
    fun testShipmentIsNotFoundInSheetWithReversingBarcodeButScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { testParameter.duplicatedScannedBarcodeAndNotInSheet() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(Pair(true, false))
    }

    @Test
    fun testShipmentIsFoundInSheetBarcodeButScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { testParameter.duplicatedScannedBarcodeAndFoundInSheet() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(Pair(true, true))
    }

    @Test
    fun testShipmentIsNotFoundInSheetWithReversingBarcodeAndNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { testParameter.notFoundInSheetAndNotDuplicateScannedBarcode() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(Pair(false, false))
    }

    @Test
    fun testSavingNonExistingBarcode() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap {
            testParameter.notFoundInSheetAndNotDuplicateScannedBarcode()
        }.flatMap {
            testParameter.checkSaveNonExistingBarcode()
        }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertValue(listOf(testParameter.data[0]))
    }

    @Test
    fun testSavingExistingBarcode() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap { testParameter.foundInSheetAndNotDuplicateScannedBarcode() }.flatMap {
            testParameter.checkSaveExistingBarcode()
        }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(listOf(testParameter.data[1]))
    }

    @Test
    fun testGetCountOfScannedShipment() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setup().flatMap {
            testParameter.getCountScannedShipments()
        }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(2L)
    }

    interface TestParameter {
        val data: List<ScannedBarcode>
        fun setup(): Observable<Unit>
        fun foundInSheetAndNotDuplicateScannedBarcode(): Observable<Pair<Boolean, Boolean>>
        fun duplicatedScannedBarcodeAndNotInSheet(): Observable<Pair<Boolean, Boolean>>
        fun duplicatedScannedBarcodeAndFoundInSheet(): Observable<Pair<Boolean, Boolean>>
        fun notFoundInSheetAndNotDuplicateScannedBarcode(): Observable<Pair<Boolean, Boolean>>
        fun checkSaveNonExistingBarcode(): Observable<out List<ScannedBarcode>>
        fun checkSaveExistingBarcode(): Observable<out List<ScannedBarcode>>
        fun getCountScannedShipments(): Observable<Long>
    }

    interface SetupTestParameter {
        fun setup(): TestParameter
    }
}