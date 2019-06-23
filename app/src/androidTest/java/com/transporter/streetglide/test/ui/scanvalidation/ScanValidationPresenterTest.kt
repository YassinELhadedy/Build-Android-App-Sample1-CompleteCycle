package com.transporter.streetglide.test.ui.scanvalidation

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.KEY_PREFERENCE
import com.transporter.streetglide.infrastructure.ScannedBarcodeRepositoryImp
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.services.ValidationService
import com.transporter.streetglide.ui.scanvalidation.ScanValidationContract
import com.transporter.streetglide.ui.scanvalidation.ScanValidationPresenter
import com.transporter.streetglide.ui.util.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by nehal on 29/04/18.
 *
 */
@RunWith(Parameterized::class)
class ScanValidationPresenterTest(private val setupTestParameter: SetupTestParameter<*>) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<ScannedBarcode> {
            override fun setup(): TestParameter<ScannedBarcode> {
                val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val config = listOf(Configuration(SignedToken("jdhhfhf"), 1, "122"))
                val jsonString = Gson().toJson(config[0])
                val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configurationRepository = ConfigurationRepository(sharedPreference)
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)
                val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)
                val validationService = ValidationService(sheetDiskRepository, scannedBarcodeRepository)
                val scanValidationPresenter = ScanValidationPresenter(validationService,
                        SchedulerProvider)
                val mockScanView = Mockito.mock(ScanValidationContract.View::class.java)
                scanValidationPresenter.setView(mockScanView)
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
                val barcode1 = "43212"
                val barcode2 = "4321"
                val duplicatedBarcode = "123456"
                val nonExistingBarcode = "12345"

                val expectedBarcodes = listOf(ScannedBarcode(barcode1.toInt(), barcode1, true),
                        ScannedBarcode(nonExistingBarcode.toInt(), nonExistingBarcode, false))

                return object : TestParameter<ScannedBarcode> {

                    override val data: List<ScannedBarcode>
                        get() = expectedBarcodes

                    override fun setUp(): Observable<Unit> =
                            sheetDiskRepository.insert(sheet).flatMap {
                                validationService.verifyBarcode(duplicatedBarcode)
                            }.flatMap {
                                validationService.verifyBarcode(barcode2)
                            }.map { Unit }

                    override fun duplicatedScannedBarcodeAndFoundInSheet() {
                        scanValidationPresenter.verifyBarcode(barcode2)
                        Mockito.verify(mockScanView).showErrorMsgShipmentAlreadyScanned()
                    }

                    override fun duplicatedScannedBarcodeAndNotInSheet() {
                        scanValidationPresenter.verifyBarcode(duplicatedBarcode)
                        Mockito.verify(mockScanView).showErrorMsgShipmentAlreadyScanned()
                        Mockito.verify(mockScanView, Mockito.never()).showErrorMsgShipmentNotInSheet()
                    }

                    override fun foundInSheetAndNotDuplicateScannedBarcode() {
                        scanValidationPresenter.verifyBarcode(barcode1)
                        Mockito.verify(mockScanView, Mockito.never()).showErrorMsgShipmentNotInSheet()
                        Mockito.verify(mockScanView, Mockito.never()).showErrorMsgShipmentAlreadyScanned()
                        Mockito.verify(mockScanView).showShipmentsCount(ArgumentMatchers.anyInt())
                    }

                    override fun notFoundInSheetAndNotDuplicateScannedBarcode() {
                        scanValidationPresenter.verifyBarcode(nonExistingBarcode)
                        Mockito.verify(mockScanView).showErrorMsgShipmentNotInSheet()
                        Mockito.verify(mockScanView).showShipmentsCount(ArgumentMatchers.anyInt())
                    }

                    override fun restoreScannedShipmentCount() {
                        scanValidationPresenter.subscribe()
                        Mockito.verify(mockScanView).showShipmentsCount(ArgumentMatchers.eq(2))
                    }
                }
            }

            override fun toString(): String = ScanValidationPresenter::class.java.simpleName!!
        }))
    }

    @Test
    fun testDisplayErrorMsgWhenDuplicateBarcodeIsScanned() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setUp().doOnComplete { testParameter.duplicatedScannedBarcodeAndNotInSheet() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
    }

    @Test
    fun testDisplayErrorMsgWhenDuplicateBarcodeIsScannedAlthoughFoundInSheet() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setUp().doOnComplete { testParameter.duplicatedScannedBarcodeAndFoundInSheet() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
    }

    @Test
    fun testShipmentIsFoundInSheetAndNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setUp().doOnComplete { testParameter.foundInSheetAndNotDuplicateScannedBarcode() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
    }

    @Test
    fun testDisplayErrorMsgWhenShipmentIsNotFoundInSheetAlthoughNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setUp().doOnComplete { testParameter.notFoundInSheetAndNotDuplicateScannedBarcode() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
    }

    @Test
    fun testRestoreScannedShipmentCountOnView() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.setUp().doOnComplete { testParameter.restoreScannedShipmentCount() }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
    }

    interface TestParameter<out T> {
        val data: List<T>
        fun setUp(): Observable<Unit>
        fun foundInSheetAndNotDuplicateScannedBarcode()
        fun notFoundInSheetAndNotDuplicateScannedBarcode()
        fun duplicatedScannedBarcodeAndNotInSheet()
        fun duplicatedScannedBarcodeAndFoundInSheet()
        fun restoreScannedShipmentCount()
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }
}