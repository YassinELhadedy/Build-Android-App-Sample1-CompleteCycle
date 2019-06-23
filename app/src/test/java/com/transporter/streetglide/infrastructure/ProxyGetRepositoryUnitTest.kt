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
import com.transporter.streetglide.models.Area
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.Sheet
import com.transporter.streetglide.models.SignedToken
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
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
 * ProxyGetRepositoryUnitTest
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class ProxyGetRepositoryUnitTest(private val setupTestParameter: ProxyGetRepositoryUnitTest.SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : ProxyGetRepositoryUnitTest.SetupTestParameter<Sheet> {
                    override fun setup(): TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val config = Configuration(SignedToken(""), 1, "123")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)
                        // Remove the milli-second part as it is not supported by Superglide
                        val datetime = Date(Date().time / 1000 * 1000)
                        val sheet = Sheet(1, "12345", 2, 3, datetime, false, consignees = emptyList())
                        val area = Area(233, "cairo", "nasr city", "cairo")
                        val daoSheets = sheet.toDaoSheet()

                        return object : TestParameter<Sheet> {

                            override val data: Sheet
                                get() = sheet

                            override fun getExistingCachedEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))

                                Mockito.`when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that Superglide was never called
                                    Mockito.verify(mockSuperGlideRestService,
                                            Mockito.never()).getDrsBySheetId(ArgumentMatchers.anyString(),
                                            ArgumentMatchers.eq(1))
                                }
                            }

                            override fun getExistingNonCachedEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))
                                Mockito.`when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that Superglide was called
                                    Mockito.verify(mockSuperGlideRestService)
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(1))
                                }
                            }

                            override fun getNonExistingCachedEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that Superglide was never called
                                    Mockito.verify(mockSuperGlideRestService, Mockito.never())
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(1))
                                }
                            }

                            override fun getNonExistingNonCachedEntity(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(9999999)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(9999999).doOnComplete {
                                    // Verify that Superglide was called
                                    Mockito.verify(mockSuperGlideRestService)
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(9999999))
                                }
                            }

                            override fun getExistingCachedEntityWithExceptionInCache(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))

                                Mockito.`when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSuperGlideRestService, Mockito.only())
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(1))
                                }
                            }

                            override fun getNonExistingCachedEntityWithExceptionInCache(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                Mockito.`when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                daoSession.daoSheetDao.insertInTx(daoSheets)
                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSuperGlideRestService, Mockito.only())
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(1))
                                }
                            }

                            override fun getNonExistingNonCachedEntityWithExceptionInService(): Observable<out Sheet> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(500, emptyResponseBody)

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(9999999)))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(sheetDiskRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(9999999).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSuperGlideRestService, Mockito.only())
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(9999999))
                                }
                            }

                            override fun getExistingNonCachedEntityWithExceptionWhileCaching(): Observable<out Sheet> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                Mockito.`when`(mockRepository.get(ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.empty())
                                Mockito.`when`(mockRepository.insertOrUpdate(sheet))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))

                                Mockito.`when`(mockSuperGlideRestService
                                        .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                ArgumentMatchers.eq(1)))
                                        .thenReturn(Observable.just(sheet.toSgDeliveryRunSheetListing()))
                                Mockito.`when`(mockSuperGlideRestService
                                        .getAreas(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                                        .thenReturn(Observable.just(SgPaginated(100, listOf(SgAreaWithRunner(area.toSgArea()))
                                        )))

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                val sheetCacheRepository = SheetProxyRepository(mockRepository,
                                        mockSheetSuperGlideRepository)

                                return sheetCacheRepository.get(1).doOnComplete {
                                    // Verify that cache was called once
                                    Mockito.verify(mockRepository, Mockito.times(1))
                                            .get(ArgumentMatchers.eq(1))
                                    Mockito.verify(mockRepository, Mockito.times(1))
                                            .insertOrUpdate(sheet)
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSuperGlideRestService, Mockito.only())
                                            .getDrsBySheetId(ArgumentMatchers.anyString(),
                                                    ArgumentMatchers.eq(1))
                                }
                            }

                        }

                    }

                    override fun toString(): String =
                            SheetProxyRepository::class.java.simpleName
                }))
    }

    @Test
    fun testGetExistingCachedEntityFromRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getExistingCachedEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
    }

    @Test
    fun testGetExistingNonCachedEntityFromRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getExistingNonCachedEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
    }

    @Test
    fun testGetNonExistingCachedEntityFromRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getNonExistingCachedEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
    }

    @Test
    fun testGetNonExistingNonCachedEntityFromRepository() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getNonExistingNonCachedEntity()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(0)

    }
//FIXME :Caused by: org.mockito.exceptions.verification.NoInteractionsWanted
//    @Test
//    @Config(shadows = [(ProxyGetRepositoryUnitTest.ShadowSQLiteDatabase::class)])
//    fun testGetExistingCachedEntityFromRepositoryWithExceptionInCache() {
//        val testParameter = setupTestParameter.setup()
//        val testObserver = TestObserver<Any>()
//        testParameter.getExistingCachedEntityWithExceptionInCache()
//                .subscribeOn(Schedulers.io())
//                .subscribe(testObserver)
//
//        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
//        testObserver.assertResult(testParameter.data)
//    }

    @Test
    @Config(shadows = [(ProxyGetRepositoryUnitTest.ShadowSQLiteDatabase::class)])
    fun testGetNonExistingCachedEnityFromRepositoryWithExeceptionInCache() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getNonExistingCachedEntityWithExceptionInCache()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertComplete()
                .assertNoErrors()
                .assertValueCount(0)
    }

    @Test
    fun testGetNonExistingNonCachedEnityFromRepositoryWithExeceptionInService() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.getNonExistingNonCachedEntityWithExceptionInService()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError { throwable: Throwable ->
            val cause = throwable.cause
            throwable is InfrastructureException &&
                    cause is HttpException
        }
    }
//FIXME :Caused by: org.mockito.exceptions.verification.NoInteractionsWanted
//    @Test
//    fun testGetExistingNonCachedEnityFromRepositoryWithExeceptionWhileCaching() {
//        val testParameter = setupTestParameter.setup()
//        val testObserver = TestObserver<Any>()
//        testParameter.getExistingNonCachedEntityWithExceptionWhileCaching()
//                .subscribeOn(Schedulers.io())
//                .subscribe(testObserver)
//
//        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
//        testObserver.assertResult(testParameter.data)
//    }

    interface TestParameter<out T> {
        fun getExistingCachedEntity(): Observable<out T>
        fun getExistingNonCachedEntity(): Observable<out T>
        fun getNonExistingCachedEntity(): Observable<out T>
        fun getNonExistingNonCachedEntity(): Observable<out T>
        fun getExistingCachedEntityWithExceptionInCache(): Observable<out T>
        fun getNonExistingCachedEntityWithExceptionInCache(): Observable<out T>
        fun getNonExistingNonCachedEntityWithExceptionInService(): Observable<out T>
        fun getExistingNonCachedEntityWithExceptionWhileCaching(): Observable<out T>
        val data: T
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