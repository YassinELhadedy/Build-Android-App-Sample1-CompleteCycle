package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dto.SgLogin
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

private const val DATA_ERROR = "Data Error!"

/**
 * Test All GetRepository
 */
@RunWith(Parameterized::class)
class GetRepositoryTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @Suppress("unused")
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : SetupTestParameter<Sheet> {
                    override fun setup(insertData: Boolean): TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val config = listOf(Configuration(SignedToken("jdhhfhf"), 1, "122"))
                        val jsonString = Gson().toJson(config[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)

                        val sheets = listOf(
                                Sheet(1, "12345", 2, 3, Date(), false, consignees = emptyList()),
                                Sheet(2, "12346", 4, 6, Date(), false, consignees = emptyList()),
                                Sheet(3, "12347", 6, 9, Date(), false, consignees = emptyList()))

                        val daoSheets = sheets.map { it.toDaoSheet() }

                        return object : TestParameter<Sheet> {
                            override fun getExistingEntity(): Observable<out Sheet> {
                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetDiskRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Sheet> {
                                return sheetDiskRepository.get(1)
                            }

                            override val data: List<*>
                                get() = sheets

                        }
                    }

                    override fun toString(): String =
                            SheetDiskRepository::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<Sheet> {
                    override fun setup(insertData: Boolean): TestParameter<Sheet> {
                        val superGlideRestService: SuperGlideRestService = SuperGlideRestServiceFactory(MOCK_BASE_URL).service
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val signedToken = superGlideRestService.login(SgLogin(USER_NAME, PASS)).blockingFirst()
                        val config = listOf(Configuration(signedToken.toSignedToken(), 1, "122"))
                        val jsonString = Gson().toJson(config[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val sheetSuperGlideRepository =
                                SheetSuperGlideRepository(superGlideRestService, configurationRepository)

                        return object : TestParameter<Sheet> {
                            lateinit var sheets: List<Sheet>
                            override val data: List<Sheet>
                                get() = sheets

                            //FIXME: Actual is the same Expected
                            override fun getExistingEntity(): Observable<out Sheet> {
                                sheets = listOf(Sheet(920, "6658855419", 3245, 1, Date(1465906138000L), false, consignees = listOf(
                                        Consignee("ندا حسام", Address(233, Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                                "نور الدين بهجت مكرم عبيد", "19", floor = 3, apartment = "8", specialMark = null),
                                                "01008063806", listOf(
                                                Shipment(8502, "4956707318", 2865, 3245,
                                                        ShipmentMoney(BigDecimal(240.00), BigDecimal(20.00), BigDecimal(0.00)),
                                                        Status.Delivered, 0, null, "ضيق وقت")

                                        )),
                                        Consignee("رنا شريف", Address(233, Area(233, "مكرم عبيد", "مدينة نصر", "القاهرة"),
                                                "ابو داود الظاهرى", "37", 7, "701", null),
                                                "01000360024", listOf(
                                                Shipment(8850, "3399540690", 3002, 3245,
                                                        ShipmentMoney(BigDecimal(100.00), BigDecimal(20.00), BigDecimal(0.00)),
                                                        Status.Delivered, 0, null, null)

                                        ))
                                )))
                                return sheetSuperGlideRepository.get(920)
                            }

                            override fun getNonExistingEntity(): Observable<out Sheet> {
                                sharedPreference.edit().clear().apply()
                                return sheetSuperGlideRepository.get(9999999)
                            }

                        }
                    }

                    override fun toString(): String =
                            SheetSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : SetupTestParameter<User> {
                    override fun setup(insertData: Boolean): TestParameter<User> {
                        val superGlideRestService: SuperGlideRestService = SuperGlideRestServiceFactory(MOCK_BASE_URL).service
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val signedToken = superGlideRestService.login(SgLogin(USER_NAME, PASS)).blockingFirst()
                        val config = listOf(Configuration(signedToken.toSignedToken(), 1, "122"))
                        val jsonString = Gson().toJson(config[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val userSuperGlideRepository =
                                UserSuperGlideRepository(superGlideRestService, configurationRepository)

                        return object : TestParameter<User> {
                            lateinit var users: List<User>
                            override val data: List<User>
                                get() = users

                            override fun getExistingEntity(): Observable<out User> {
                                return userSuperGlideRepository.get(999).map { user ->
                                    /*
                                     * Trying to capture the data. This is not
                                     * the best implementation.
                                     */
                                    users = listOf(user)
                                    user
                                }
                            }

                            override fun getNonExistingEntity(): Observable<out User> {
                                sharedPreference.edit().clear().apply()
                                return userSuperGlideRepository.get(9999999)
                            }
                        }
                    }

                    override fun toString(): String =
                            UserSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : SetupTestParameter<Configuration> {
                    override fun setup(insertData: Boolean): TestParameter<Configuration> {
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val config = listOf(Configuration(SignedToken("jdhhfhf"), 1, "122"))
                        val jsonString = Gson().toJson(config[0])

                        return object : TestParameter<Configuration> {

                            override val data: List<Configuration>
                                get() = config

                            override fun getExistingEntity(): Observable<out Configuration> {
                                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                                return configRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Configuration> {
                                sharedPreference.edit().clear().apply()
                                return configRepository.get(1)
                            }
                        }
                    }

                    override fun toString(): String = ConfigurationRepository::class.java.simpleName!!
                }))
    }

    @Test
    fun testGetExistingEntityFromRepository() {

        val testParameter = setupTestParameter.setup(true)
        val testObserver = TestObserver<Any>()
        testParameter.getExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data[0])

    }

    @Test
    fun testGetNonExistingEntityFromRepository() {

        val testParameter = setupTestParameter.setup(false)
        val testObserver = TestObserver<Any>()
        testParameter.getNonExistingEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(0)

    }

    interface TestParameter<out T> {
        fun getExistingEntity(): Observable<out T>
        fun getNonExistingEntity(): Observable<out T>
        val data: List<*>
    }

    interface SetupTestParameter<out T> {
        fun setup(insertData: Boolean): TestParameter<T>
    }
}