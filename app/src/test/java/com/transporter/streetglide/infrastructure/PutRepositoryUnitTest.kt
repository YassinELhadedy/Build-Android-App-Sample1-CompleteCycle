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
 * Test All PutRepository
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class PutRepositoryUnitTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        const val NEW_BARCODE = "NewBarcode"
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<Sheet> {

            override fun setup(): TestParameter<Sheet> {

                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val configRepository = ConfigurationRepository(sharedPreference)
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)

                val sheet = Sheet(2, "12346", 4, 6, Date(), false, consignees = listOf(
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
                        )))
                val daoSheet = sheet.toDaoSheet()

                val modifiedSheet = sheet.copy(barcode = NEW_BARCODE,
                        consignees = listOf(Consignee("name",
                                Address(5, Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"), "street", null, null, null, null), "65432",
                                listOf(Shipment(4,
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

                    override fun updateExistingEntity(): Observable<Unit> {
                        daoSession.daoSheetDao.insert(daoSheet)
                        return sheetDiskRepository.update(modifiedSheet)
                    }

                    override fun updateNonExistingEntity(): Observable<Unit> =
                            sheetDiskRepository.update(sheet)

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

                        val scannedBarcode = ScannedBarcode(0, "123456789", false)
                        val daoScannedBarcode = scannedBarcode.toDaoScannedBarcode()

                        val modifiedScannedBarcode = scannedBarcode.copy(found = true)
                        return object : TestParameter<ScannedBarcode> {
                            override val data: ScannedBarcode
                                get() = scannedBarcode

                            override fun updateExistingEntity(): Observable<Unit> {
                                daoSession.daoScannedBarcodeDao.insert(daoScannedBarcode)
                                return scannedBarcodeRepository.update(modifiedScannedBarcode)
                            }

                            override fun updateNonExistingEntity(): Observable<Unit> =
                                    scannedBarcodeRepository.update(modifiedScannedBarcode)

                            override fun checkUpdate() {
                                daoSession.clear()
                                val loadedScannedBarcode = daoSession.daoScannedBarcodeDao.load(modifiedScannedBarcode.id.toLong())
                                Assert.assertEquals(modifiedScannedBarcode.found, loadedScannedBarcode.found)
                            }
                        }
                    }

                    override fun toString(): String = ScannedBarcodeRepositoryImp::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<Configuration> {
                    override fun setup(): TestParameter<Configuration> {
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val configRepository = ConfigurationRepository(sharedPreference)

                        val config = Configuration(SignedToken("jdhhfhf"), 1, "122")
                        val jsonString = Gson().toJson(config)
                        val modifiedConfiguration = config.copy(signedToken = SignedToken("hgd5s4s"), sheetId = 5)
                        return object : TestParameter<Configuration> {
                            override val data: Configuration
                                get() = config

                            override fun updateExistingEntity(): Observable<Unit> {
                                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                                return configRepository.update(modifiedConfiguration)
                            }

                            override fun updateNonExistingEntity(): Observable<Unit> {
                                return configRepository.update(modifiedConfiguration)
                            }

                            override fun checkUpdate() {
                                val json = sharedPreference.getString(ConfigurationRepository.KEY_CONFIG, null)
                                val loadedConfiguration = Gson().fromJson(json, Configuration::class.java)
                                Assert.assertEquals(modifiedConfiguration.signedToken, loadedConfiguration.signedToken)
                                Assert.assertEquals(modifiedConfiguration.sheetId, loadedConfiguration.sheetId)
                            }
                        }
                    }

                    override fun toString(): String = ConfigurationRepository::class.java.simpleName!!
                }))
    }

    @Test
    fun testUpdateExistingEntityInRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.updateExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
        testParameter.checkUpdate()
    }

    @Test
    fun testUpdateNonExistingEntityInRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.updateNonExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
    }

    interface TestParameter<out T> {
        val data: T

        fun updateExistingEntity(): Observable<Unit>
        fun updateNonExistingEntity(): Observable<Unit>
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