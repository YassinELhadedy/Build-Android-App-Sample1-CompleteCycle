package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.ShipmentMapper.toShipment
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit


private const val DATA_ERROR = "Data Error!"

/**
 * Test All WriteRepository
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class WriteRepositoryUnitTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        const val NEW_BARCODE = "NewBarcode"

        @Suppress("unused")
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<Sheet> {

            override fun setup(): TestParameter<Sheet> {

                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val config = Configuration(SignedToken(""), 2, "123")
                val jsonString = Gson().toJson(config)
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configRepository = ConfigurationRepository(sharedPreference)
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)

                val sheet = Sheet(2, "12346", 4, 6, Date(), false, consignees = listOf(Consignee("name",
                        Address(5, Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"), "street", null, null, null, null), "65432",
                        listOf(Shipment(3, "4321", 4,
                                4,
                                ShipmentMoney(BigDecimal("100.0"), BigDecimal("20.0"), BigDecimal("0.0")), Status.Delivered, 1, null, null)))))

                val daoSheet = sheet.toDaoSheet()

                val modifiedSheet = sheet.copy(barcode = NEW_BARCODE,
                        consignees = listOf(
                                Consignee("name",
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
                                                Status.OutForDelivery,
                                                1,
                                                null, null)))))

                return object : TestParameter<Sheet> {

                    override val data: Sheet
                        get() = sheet

                    override fun insertOrUpdateNonExistingEntity(): Observable<out Sheet> {
                        return sheetDiskRepository.insertOrUpdate(sheet)
                    }

                    override fun insertOrUpdateDuplicateEntity(): Observable<out Sheet> {
                        daoSession.daoSheetDao.insert(daoSheet)
                        return sheetDiskRepository.insertOrUpdate(modifiedSheet)
                    }

                    override fun checkInsert() {
                        daoSession.clear()
                        val loadedSheet = daoSession.daoSheetDao.load(sheet.id.toLong())
                        Assert.assertEquals(sheet.id, loadedSheet.id.toInt())

                        Assert.assertEquals(sheet.consignees[0].shipments.count(), loadedSheet.shipments.count())
                        Assert.assertThat(sheet.consignees[0].shipments,
                                hasItems(*loadedSheet.shipments.map { it.toShipment() }.toTypedArray()))
                    }

                    override fun checkUpdate() {
                        daoSession.clear()
                        val loadedSheet = daoSession.daoSheetDao.load(modifiedSheet.id.toLong())
                        Assert.assertEquals(modifiedSheet.barcode, loadedSheet.barcode)

                        Assert.assertEquals(modifiedSheet.consignees[0].shipments.count(), loadedSheet.shipments.count())
                        Assert.assertThat(modifiedSheet.consignees[0].shipments,
                                hasItems(*loadedSheet.shipments.map { it.toShipment() }.toTypedArray()))
                    }
                }
            }

            override fun toString(): String = SheetDiskRepository::class.java.simpleName!!
        }),
                arrayOf(object : SetupTestParameter<ScannedBarcode> {

                    override fun setup(): TestParameter<ScannedBarcode> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)

                        val barcode = "123456"
                        val scannedBarcode = ScannedBarcode(barcode.toInt(), barcode, false)
                        val daoScannedBarcode = scannedBarcode.toDaoScannedBarcode()

                        val modifiedScannedBarcode = scannedBarcode.copy(found = true)

                        return object : TestParameter<ScannedBarcode> {
                            override val data: ScannedBarcode
                                get() = scannedBarcode

                            override fun insertOrUpdateNonExistingEntity(): Observable<out ScannedBarcode> {
                                return scannedBarcodeRepository.insertOrUpdate(scannedBarcode)
                            }

                            override fun insertOrUpdateDuplicateEntity(): Observable<out ScannedBarcode> {
                                daoSession.daoScannedBarcodeDao.insert(daoScannedBarcode)
                                return scannedBarcodeRepository.insertOrUpdate(modifiedScannedBarcode)
                            }

                            override fun checkInsert() {
                                daoSession.clear()
                                val loadedScannedBarcode = daoSession.daoScannedBarcodeDao.load(scannedBarcode.id.toLong())
                                Assert.assertEquals(scannedBarcode.id, loadedScannedBarcode.id.toInt())
                            }

                            override fun checkUpdate() {
                                daoSession.clear()
                                val loadedScannedBarcode = daoSession.daoScannedBarcodeDao.load(modifiedScannedBarcode.id.toLong())
                                Assert.assertEquals(modifiedScannedBarcode.found, loadedScannedBarcode.found)
                            }
                        }
                    }

                    override fun toString(): String = ScannedBarcodeRepositoryImp::class.java.simpleName!!
                }))
    }

    @Test
    fun testInsertOrUpdateNonExistingEntityIntoRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.insertOrUpdateNonExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
        testParameter.checkInsert()
    }

    @Test
    fun testInsertOrUpdateDuplicateEntityIntoRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.insertOrUpdateDuplicateEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
        testParameter.checkUpdate()
    }

    interface TestParameter<out T> {
        val data: T

        fun insertOrUpdateNonExistingEntity(): Observable<out T>
        fun insertOrUpdateDuplicateEntity(): Observable<out T>
        fun checkInsert()
        fun checkUpdate()
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }

    @Implements(SQLiteDatabase::class)
    class ShadowSQLiteDatabase {

        @Suppress("unused")
        @Implementation
        fun rawQuery(@Suppress("UNUSED_PARAMETER") sql: String, @Suppress("UNUSED_PARAMETER") selectionArgs: Array<String>): Cursor {
            throw RuntimeException(DATA_ERROR)
        }
    }
}