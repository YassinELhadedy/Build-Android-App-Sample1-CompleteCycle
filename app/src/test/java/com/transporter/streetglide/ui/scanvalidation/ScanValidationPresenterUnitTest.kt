package com.transporter.streetglide.ui.scanvalidation

import com.nhaarman.mockito_kotlin.*
import com.transporter.streetglide.models.services.ValidationService
import com.transporter.streetglide.ui.util.ImmediateSchedulerProvider
import io.reactivex.Observable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class ScanValidationPresenterUnitTest(private val setupTestParameter: SetupTestParameter) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter {
            override fun setup(): TestParameter {
                val mockValidationService = mock<ValidationService>()
                val mockView = mock<ScanActivity>()
                val classUnderTest = ScanValidationPresenter(mockValidationService,
                        ImmediateSchedulerProvider)
                classUnderTest.setView(mockView)

                val barcode = "43212"
                val nonExistingBarcode = "12345"

                return object : TestParameter {

                    override fun foundInSheetAndNotDuplicateScannedBarcode() {
                        whenever(mockValidationService.verifyBarcode(eq(barcode)))
                                .thenReturn(Observable.just(Pair(false, true)))

                        classUnderTest.verifyBarcode(barcode)

                        verify(mockView).showShipmentsCount(eq(1))
                    }

                    override fun notFoundInSheetAndNotDuplicateScannedBarcode() {
                        whenever(mockValidationService.verifyBarcode(eq(nonExistingBarcode)))
                                .thenReturn(Observable.just(Pair(false, false)))

                        doNothing().whenever(mockView).showErrorMsgShipmentNotInSheet()

                        classUnderTest.verifyBarcode(nonExistingBarcode)

                        verify(mockView).showErrorMsgShipmentNotInSheet()
                        verify(mockView).showShipmentsCount(eq(1))
                    }

                    override fun duplicatedScannedBarcodeAndNotInSheet() {
                        whenever(mockValidationService.verifyBarcode(eq(nonExistingBarcode)))
                                .thenReturn(Observable.just(Pair(true, false)))

                        doNothing().whenever(mockView).showErrorMsgShipmentAlreadyScanned()

                        classUnderTest.verifyBarcode(nonExistingBarcode)

                        verify(mockView).showErrorMsgShipmentAlreadyScanned()
                    }

                    override fun duplicatedScannedBarcodeAndFoundInSheet() {
                        whenever(mockValidationService.verifyBarcode(eq(barcode)))
                                .thenReturn(Observable.just(Pair(true, true)))

                        doNothing().whenever(mockView).showErrorMsgShipmentAlreadyScanned()

                        classUnderTest.verifyBarcode(barcode)

                        verify(mockView).showErrorMsgShipmentAlreadyScanned()
                    }

                    override fun restoreScannedShipmentCount() {
                        whenever(mockValidationService.getScannedShipmentsCount())
                                .thenReturn(Observable.just(2L))

                        classUnderTest.subscribe()

                        verify(mockValidationService).getScannedShipmentsCount()
                        verify(mockView).showShipmentsCount(eq(2))
                    }
                }
            }

            override fun toString(): String = ScanValidationPresenter::class.java.simpleName!!
        }))
    }

    @Test
    fun testShipmentIsFoundInSheetAndNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        testParameter.foundInSheetAndNotDuplicateScannedBarcode()
    }

    @Test
    fun testDisplayErrorMsgWhenShipmentIsNotFoundInSheetAlthoughNotScannedBefore() {
        val testParameter = setupTestParameter.setup()
        testParameter.notFoundInSheetAndNotDuplicateScannedBarcode()
    }

    @Test
    fun testDisplayErrorMsgWhenDuplicateBarcodeIsScanned() {
        val testParameter = setupTestParameter.setup()
        testParameter.duplicatedScannedBarcodeAndNotInSheet()
    }

    @Test
    fun testDisplayErrorMsgWhenDuplicateBarcodeIsScannedAlthoughFoundInSheet() {
        val testParameter = setupTestParameter.setup()
        testParameter.duplicatedScannedBarcodeAndFoundInSheet()
    }

    @Test
    fun testRestoreScannedShipmentCountOnView() {
        val testParameter = setupTestParameter.setup()
        testParameter.restoreScannedShipmentCount()
    }

    interface TestParameter {
        fun foundInSheetAndNotDuplicateScannedBarcode()
        fun notFoundInSheetAndNotDuplicateScannedBarcode()
        fun duplicatedScannedBarcodeAndNotInSheet()
        fun duplicatedScannedBarcodeAndFoundInSheet()
        fun restoreScannedShipmentCount()
    }

    interface SetupTestParameter {
        fun setup(): TestParameter
    }
}