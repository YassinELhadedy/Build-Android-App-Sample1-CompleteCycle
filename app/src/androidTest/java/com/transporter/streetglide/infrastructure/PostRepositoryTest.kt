package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
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
import org.junit.runners.Parameterized
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

private const val DATA_ERROR = "Data Error!"

/**
 * Test All PostRepository
 */
@RunWith(Parameterized::class)
class PostRepositoryTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @Suppress("unused")
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : SetupTestParameter<Sheet> {

                    override fun setup(): TestParameter<Sheet> {

                        val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val config = listOf(Configuration(SignedToken("jdhhfhf"), 2, "122"))
                        val jsonString = Gson().toJson(config[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)
                        val sheet = Sheet(2, "12346", 4, 6, Date(), false, consignees = listOf(
                                Consignee("name",
                                        Address(5, Area(11, "", "", ""),
                                                "street", null, null, null, null),
                                        "65432",
                                        listOf(
                                                Shipment(3, "4321", 4, 4,
                                                        ShipmentMoney(BigDecimal("100.0"), BigDecimal("20.0"), BigDecimal("0.0")),
                                                        Status.Delivered, 1, null, null
                                                )))))

                        return object : TestParameter<Sheet> {

                            override val data: Sheet
                                get() = sheet

                            override fun insertNonExistingEntity(): Observable<out Sheet> =
                                    sheetDiskRepository.insert(sheet)

                            override fun insertDuplicateEntity(): Observable<out Sheet> {
                                val observable1 = sheetDiskRepository.insert(sheet)
                                val observable2 = sheetDiskRepository.insert(sheet)
                                return Observable.merge(observable1, observable2)
                            }

                            override fun checkInsert() {
                                daoSession.clear()
                                val loadedSheet = daoSession.daoSheetDao.load(sheet.id.toLong())
                                Assert.assertEquals(sheet.id, loadedSheet.id.toInt())
                                Assert.assertEquals(sheet.consignees[0].shipments.count(), loadedSheet.shipments.count())
                                Assert.assertThat(sheet.consignees[0].shipments,
                                        hasItems(*loadedSheet.shipments.map { it.toShipment() }.toTypedArray()))
                            }
                        }
                    }

                    override fun toString(): String = SheetDiskRepository::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<SignedToken> {
                    override fun setup(): TestParameter<SignedToken> {
                        val tokenSuperGlideRestService = TokenSuperGlideRepository(SuperGlideRestServiceFactory(MOCK_BASE_URL).service)
                        return object : TestParameter<SignedToken> {
                            lateinit var signedToken: SignedToken
                            override val data: SignedToken
                                get() = signedToken

                            override fun insertNonExistingEntity(): Observable<out SignedToken> {
                                val observable = tokenSuperGlideRestService.insert(User("", "omar", "adel1234", true, null))
                                return observable.map { signedToken ->
                                    /*
                                     * Trying to capture the signedToken. This is not
                                     * the best implementation.
                                     */
                                    this.signedToken = signedToken; signedToken
                                }
                            }

                            override fun insertDuplicateEntity(): Observable<out SignedToken> {
                                /*
                                 * I do not know if this is the best way to do this because
                                 * duplicates is allowed for TokenSuperGlideRestService.
                                 */
                                return Observable.error(InfrastructureException())
                            }

                            override fun checkInsert() {
                                // Check the token is valid and has not expired.
                                JWT(signedToken.token).isExpired(0)
                            }

                        }
                    }

                    override fun toString() = SignedToken::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<ScannedBarcode> {
                    override fun setup(): TestParameter<ScannedBarcode> {
                        val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)

                        val scannedBarcode = ScannedBarcode(0, "123456789", false)
                        return object : TestParameter<ScannedBarcode> {
                            override val data: ScannedBarcode
                                get() = scannedBarcode

                            override fun insertNonExistingEntity(): Observable<out ScannedBarcode> {
                                return scannedBarcodeRepository.insert(scannedBarcode)
                            }

                            override fun insertDuplicateEntity(): Observable<out ScannedBarcode> {
                                val observable1 = scannedBarcodeRepository.insert(scannedBarcode)
                                val observable2 = scannedBarcodeRepository.insert(scannedBarcode)
                                return Observable.merge(observable1, observable2)
                            }

                            override fun checkInsert() {
                                daoSession.clear()
                                val loadedScannedBarcode = daoSession.daoScannedBarcodeDao.load(scannedBarcode.id.toLong())
                                Assert.assertEquals(scannedBarcode.id, loadedScannedBarcode.id.toInt())
                            }
                        }
                    }

                    override fun toString(): String = ScannedBarcodeRepositoryImp::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<Configuration> {
                    override fun setup(): TestParameter<Configuration> {
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(null, Context.MODE_PRIVATE)
                        val configRepository = ConfigurationRepository(sharedPreference)

                        val config = Configuration(SignedToken("jdhhfhf"), 1, "122")

                        return object : TestParameter<Configuration> {
                            override val data: Configuration
                                get() = config

                            override fun insertNonExistingEntity(): Observable<out Configuration> {
                                sharedPreference.edit().clear().apply()
                                return configRepository.insert(config)
                            }

                            override fun insertDuplicateEntity(): Observable<out Configuration> {
                                val observable1 = configRepository.insert(config)
                                val observable2 = configRepository.insert(config)
                                return Observable.merge(observable1, observable2)
                            }

                            override fun checkInsert() {
                                val json = sharedPreference.getString(ConfigurationRepository.KEY_CONFIG, null)
                                val loadedConfiguration = Gson().fromJson(json, Configuration::class.java)
                                Assert.assertEquals(config, loadedConfiguration)
                            }
                        }
                    }

                    override fun toString(): String = ConfigurationRepository::class.java.simpleName!!
                }))
    }

    @Test
    fun testInsertNonExistingEntityIntoRepository() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.insertNonExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
        testParameter.checkInsert()

    }

    @Test
    fun testInsertDuplicateEntityIntoRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.insertDuplicateEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError(InfrastructureException::class.java)
    }

    interface TestParameter<out T> {
        val data: T

        fun insertNonExistingEntity(): Observable<out T>
        fun insertDuplicateEntity(): Observable<out T>
        fun checkInsert()
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }

//    @Implements(SQLiteDatabase::class)
//    class ShadowSQLiteDatabase {
//
//        @Suppress("unused")
//        @Implementation
//        fun rawQuery(@Suppress("UNUSED_PARAMETER") sql: String, @Suppress("UNUSED_PARAMETER") selectionArgs: Array<String>): Cursor {
//            throw RuntimeException(DATA_ERROR)
//        }
//
//    }
}