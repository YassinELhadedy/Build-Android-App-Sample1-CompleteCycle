package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
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
 * Test All DeleteRepository
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class DeleteRepositoryUnitTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<Sheet> {

            override fun setup(): TestParameter<Sheet> {

                val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val config = Configuration(SignedToken("jdhhfhf"), 2, "122")
                val jsonString = Gson().toJson(config)
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
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

                return object : TestParameter<Sheet> {

                    override val data: Sheet
                        get() = sheet

                    override fun deleteExistingEntity(): Observable<Unit> {
                        daoSession.daoSheetDao.insert(daoSheet)
                        return sheetDiskRepository.delete(sheet.id)
                    }

                    override fun deleteNonExistingEntity(): Observable<Unit> {
                        return sheetDiskRepository.delete(sheet.id)
                    }

                    override fun checkDelete() {
                        daoSession.clear()
                        val loadedSheet = daoSession.daoSheetDao.load(sheet.id.toLong())
                        val loadedShipment = daoSession.daoShipmentDao.load(sheet.consignees[0].shipments[0].id.toLong())
                        Assert.assertNull(loadedSheet)
                        Assert.assertNull(loadedShipment)
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

                        return object : TestParameter<ScannedBarcode> {
                            override val data: ScannedBarcode
                                get() = scannedBarcode

                            override fun deleteExistingEntity(): Observable<Unit> {
                                daoSession.daoScannedBarcodeDao.insert(daoScannedBarcode)
                                return scannedBarcodeRepository.delete(scannedBarcode.id)
                            }

                            override fun deleteNonExistingEntity(): Observable<Unit> {
                                return scannedBarcodeRepository.delete(scannedBarcode.id)
                            }

                            override fun checkDelete() {
                                daoSession.clear()
                                val loadedScannedBarcode = daoSession.daoScannedBarcodeDao.load(scannedBarcode.id.toLong())
                                Assert.assertNull(loadedScannedBarcode)
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

                        return object : TestParameter<Configuration> {
                            override val data: Configuration
                                get() = config

                            override fun deleteExistingEntity(): Observable<Unit> {
                                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                                return configRepository.delete(1)
                            }

                            override fun deleteNonExistingEntity(): Observable<Unit> {
                                return configRepository.delete(1)
                            }

                            override fun checkDelete() {
                                val json = sharedPreference.getString(ConfigurationRepository.KEY_CONFIG, null)
                                val loadedConfiguration = Gson().fromJson(json, Configuration::class.java)
                                Assert.assertNull(loadedConfiguration)
                            }

                        }
                    }

                    override fun toString(): String = ConfigurationRepository::class.java.simpleName!!

                }))
    }

    @Test
    fun testDeleteExistingEntityInRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.deleteExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
        testParameter.checkDelete()
    }

    @Test
    fun testDeleteNonExistingEntityInRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.deleteNonExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError(InfrastructureException::class.java)
    }

    interface TestParameter<out T> {
        val data: T
        fun deleteExistingEntity(): Observable<Unit>
        fun deleteNonExistingEntity(): Observable<Unit>
        fun checkDelete()
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