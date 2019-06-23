package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dto.SgArea.Companion.toSgArea
import com.transporter.streetglide.infrastructure.dto.SgAreaWithRunner
import com.transporter.streetglide.infrastructure.dto.SgDeliveryRunSheetListing.Companion.toSgDeliveryRunSheetListing
import com.transporter.streetglide.infrastructure.dto.SgPaginated
import com.transporter.streetglide.infrastructure.dto.SgUserWithAddressesAndPhonesAndAreasAndCapacities.Companion.toSgUserWithAddressesAndPhonesAndAreasAndCapacities
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import retrofit2.HttpException
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit


private const val DATA_ERROR = "Data Error!"

/**
 * Test All GetRepository
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class GetRepositoryUnitTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : SetupTestParameter<Sheet> {
                    override fun setup(insertData: Boolean): TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken("jdhhfhf"), 1, "122")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)

                        val sheet =
                                Sheet(1, "12345", 2, 3, Date(), false, consignees = emptyList())

                        val daoSheets = sheet.toDaoSheet()

                        return object : TestParameter<Sheet> {
                            override fun getExistingEntity(): Observable<out Sheet> {
                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetDiskRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Sheet> {
                                return sheetDiskRepository.get(1)
                            }

                            override fun getEntityWithException(): Observable<out Sheet> {
                                return sheetDiskRepository.get(1)
                            }

                            override val data: Sheet
                                get() = sheet
                        }
                    }

                    override fun toString(): String =
                            SheetDiskRepository::class.java.simpleName!!
                }),
                arrayOf(object : SetupTestParameter<Sheet> {
                    override fun setup(insertData: Boolean): TestParameter<Sheet> {
                        // Remove the milli-second part as it is not supported by Superglide
                        val datetime = Date(Date().time / 1000 * 1000)
                        val sheet =
                                Sheet(1, "12345", 2, 3, datetime, false, consignees = emptyList())
                        val area = Area(233, "cairo", "nasr city", "cairo")
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken("jdhhfhf"), 2, "122")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)

                        return object : TestParameter<Sheet> {
                            override val data: Sheet
                                get() = sheet

                            override fun getExistingEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                `when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))

                                `when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return mockSheetSuperGlideRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                `when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(9999999)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return mockSheetSuperGlideRepository.get(9999999)
                            }

                            override fun getEntityWithException(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                /*
                                 * We are checking that the exception will pass transparently
                                 * to us. This may not be a good choice BTW.
                                 */
                                `when`(mockSuperGlideRestService.getDrsBySheetId(ArgumentMatchers.anyString(),
                                        ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return mockSheetSuperGlideRepository.get(1)
                            }
                        }
                    }

                    override fun toString(): String =
                            SheetSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : SetupTestParameter<Sheet> {
                    override fun setup(insertData: Boolean): TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken("jdhhfhf"), 2, "122")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)

                        // Remove the milli-second part as it is not supported by Superglide
                        val datetime = Date(Date().time / 1000 * 1000)
                        val sheet =
                                Sheet(1, "12345", 2, 3, datetime, false, consignees = emptyList())
                        val area = Area(233, "cairo", "nasr city", "cairo")

                        return object : TestParameter<Sheet> {
                            override val data: Sheet
                                get() = sheet

                            override fun getExistingEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                `when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))
                                `when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)
                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                `when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(9999999)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(9999999)
                            }

                            override fun getEntityWithException(): Observable<out Sheet> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                /*
                                 * We are checking that the exception will pass transparently
                                 * to us. This may not be a good choice BTW.
                                 */
                                `when`(mockSuperGlideRestService.getDrsBySheetId(ArgumentMatchers.anyString(),
                                        ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(1)
                            }
                        }
                    }

                    override fun toString(): String =
                            SheetProxyRepository::class.java.simpleName
                }),
                arrayOf(object : SetupTestParameter<User> {
                    override fun setup(insertData: Boolean): TestParameter<User> {
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken("jdhhfhf"), 2, "123")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val user = User("Mohamed Omar",
                                null,
                                null,
                                true,
                                "123")

                        return object : TestParameter<User> {
                            override val data: User
                                get() = user

                            override fun getExistingEntity(): Observable<out User> {
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                `when`(mockSuperGlideRestService
                                        .getUser(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(999)))
                                        .thenReturn(Observable.just(user.toSgUserWithAddressesAndPhonesAndAreasAndCapacities()))

                                val mockUserSuperGlideRepository =
                                        UserSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return mockUserSuperGlideRepository.get(999)
                            }

                            override fun getNonExistingEntity(): Observable<out User> {
                                sharedPreference.edit().clear().apply()
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)
                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                `when`(mockSuperGlideRestService
                                        .getUser(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(9999999)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockUserSuperGlideRepository =
                                        UserSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return mockUserSuperGlideRepository.get(9999999)
                            }

                            override fun getEntityWithException(): Observable<out User> {
                                sharedPreference.edit().putInt(ConfigurationRepository.KEY_CONFIG, 0).apply()
                                val mockSuperGlideRestService = mock(SuperGlideRestService::class.java)

                                /*
                                 * We are checking that the exception will pass transparently
                                 * to us. This may not be a good choice BTW.
                                 */
                                `when`(mockSuperGlideRestService.getUser(ArgumentMatchers.anyString(),
                                        ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))

                                val mockUserSuperGlideRepository =
                                        UserSuperGlideRepository(mockSuperGlideRestService, configRepository)
                                return mockUserSuperGlideRepository.get(1)
                            }
                        }
                    }

                    override fun toString(): String =
                            UserSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : SetupTestParameter<Configuration> {
                    override fun setup(insertData: Boolean): TestParameter<Configuration> {
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken("jdhhfhf"), 1, "122")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)

                        return object : TestParameter<Configuration> {

                            override val data: Configuration
                                get() = config

                            override fun getExistingEntity(): Observable<out Configuration> {
                                return configRepository.get(1)
                            }

                            override fun getNonExistingEntity(): Observable<out Configuration> {
                                sharedPreference.edit().clear().apply()
                                return configRepository.get(1)
                            }

                            override fun getEntityWithException(): Observable<out Configuration> {
                                sharedPreference.edit().putInt(ConfigurationRepository.KEY_CONFIG, 0).apply()
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
        testObserver.assertResult(testParameter.data)
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

    @Test
    @Config(shadows = [(ShadowSQLiteDatabase::class)])
    fun testGetEntityFromRepositoryWithException() {

        val testParameter = setupTestParameter.setup(false)
        val testObserver = TestObserver<Any>()
        testParameter.getEntityWithException()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError { throwable: Throwable ->
            val cause = throwable.cause
            throwable is InfrastructureException &&
                    cause is RuntimeException &&
                    cause.message == DATA_ERROR
        }

    }

    interface TestParameter<out T> {
        fun getExistingEntity(): Observable<out T>
        fun getNonExistingEntity(): Observable<out T>
        fun getEntityWithException(): Observable<out T>
        val data: T
    }

    interface SetupTestParameter<out T> {
        fun setup(insertData: Boolean): TestParameter<T>
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