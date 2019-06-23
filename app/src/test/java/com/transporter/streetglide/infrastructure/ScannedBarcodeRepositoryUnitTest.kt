package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.ScannedBarcode
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class ScannedBarcodeRepositoryUnitTest(private val setupTestParameter: SetupTestParameter) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter {
            override fun setup(): TestParameter {
                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)

                val barcode1 = "123456789"
                val barcode2 = "254789154"
                val barcode3 = "178462480"

                val scannedBarcodes = listOf(ScannedBarcode(12, barcode1, false),
                        ScannedBarcode(13, barcode2, true),
                        ScannedBarcode(14, barcode3, true))

                return object : TestParameter {
                    override fun getCountOfScannedBarcodeWhichIsNotEmpty(): Observable<Long> {
                        scannedBarcodes.map { it.toDaoScannedBarcode() }
                                .forEach { daoSession.daoScannedBarcodeDao.insert(it) }
                        return scannedBarcodeRepository.getCount()
                    }

                    override fun getCountOfScannedBarcodeWhichIsEmpty(): Observable<Long> {
                        return scannedBarcodeRepository.getCount()
                    }
                }
            }

            override fun toString(): String = ScannedBarcodeRepositoryImp::class.java.simpleName
        }))
    }

    @Test
    fun testGetCountOfScannedBarcodeWhichIsNotEmpty() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()

        testParameter.getCountOfScannedBarcodeWhichIsNotEmpty()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(3L)
    }

    @Test
    fun testGetCountOfScannedBarcodeWhichIsEmpty() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()

        testParameter.getCountOfScannedBarcodeWhichIsEmpty()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
        testObserver.assertResult(0L)
    }

    interface TestParameter {
        fun getCountOfScannedBarcodeWhichIsNotEmpty(): Observable<Long>
        fun getCountOfScannedBarcodeWhichIsEmpty(): Observable<Long>
    }

    interface SetupTestParameter {
        fun setup(): TestParameter
    }
}