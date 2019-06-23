package com.transporter.streetglide.model

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.*
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dto.SgLogin
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.exception.SheetNotFoundException
import com.transporter.streetglide.models.exception.TokenExpiredException
import com.transporter.streetglide.models.services.RequestSheetService
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *Created by yassin on 4/15/18.
 */
@RunWith(Parameterized::class)
class RequestSheetTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<Boolean> {
            override fun setup(): TestParameter<Boolean> {
                val superGlideRestService: SuperGlideRestService = SuperGlideRestServiceFactory(MOCK_BASE_URL).service
                val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                val signedToken = superGlideRestService.login(SgLogin(USER_NAME, PASS)).blockingFirst()
                val config = listOf(Configuration(signedToken.toSignedToken(), 1, "122"))
                val jsonString = Gson().toJson(config[0])
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configurationRepository = ConfigurationRepository(sharedPreference)
                val sheetSuperGlideRepository = SheetSuperGlideRepository(superGlideRestService, configurationRepository)
                val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                val daoSession = DaoMaster(openHelper.writableDb).newSession()
                val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)

                val requestSheetTest = RequestSheetService(sheetSuperGlideRepository, sheetDiskRepository, ConfigurationRepository(sharedPreference))
                val dateEX1 = 1463646433000L
                val dateEX2 = 1486616999000L //Thu Feb 09 07:09:59 GMT+02:00 2017
                val dateEX3 = 1588616999000L

                val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX1))
                val expr2 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX2))
                val expr3 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX3))
                val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())
                val exception = Throwable("HTTP 401 Unauthorized")

                val correctPaginationSheetMap = hashMapOf(
                        Pagination(expr1, sort1, 0, 1) to true,
                        Pagination(expr2, sort1, 0, 1) to false  // sheet returned
                )
                val faultyPaginationSheetMap = hashMapOf(
                        Pagination(expr3, sort1, 0, 1) to SheetNotFoundException("sheet not found")
                )
                val faultyPaginationSheetWithThrowTokenValidationExceptionMap = hashMapOf(
                        Pagination(expr3, sort1, 0, 1) to TokenExpiredException(exception)
                )
                return object : TestParameter<Boolean> {

                    override fun getCorrectSheets(): Set<Pagination> = correctPaginationSheetMap.keys

                    override fun getValidSheet(pagination: Pagination): Triple<Observable<out Boolean>, Pagination, Boolean?> {

                        return Triple(requestSheetTest.requestSheet(pagination),
                                pagination,
                                correctPaginationSheetMap[pagination])
                    }

                    override fun getNoSheets(): Set<Pagination> = faultyPaginationSheetMap.keys

                    override fun getInValidSheet(pagination: Pagination): Triple<Observable<out Boolean>, Pagination, SheetNotFoundException?> {
                        return Triple(requestSheetTest.requestSheet(pagination),
                                pagination,
                                faultyPaginationSheetMap[pagination])
                    }

                    override fun getInValidSheetWithThrowTokenValidationException(pagination: Pagination): Triple<Observable<out Boolean>, Pagination, TokenExpiredException?> {
                        sharedPreference.edit().clear().apply()
                        val faultSignedToken = SignedToken("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoi2LTYsdmK2YEg2KfZhNiv2YjZitin2KrZiiIsInVzZXJuYW1lIjoic2hlcmlmIiwiZXhwIjoxNTE4MTA4ODI1LCJpZCI6MTAxNSwidHlwZSI6NH0.IrtnijGwUEexMvF-apReIbBjK1vYRZ05ePA3EFZ0yfUlAoH026p8nOnO62RnFSPMJRi0FrASq-rG_CEFQmJdRA")
                        val faultConfig = listOf(Configuration(faultSignedToken, 1, "122"))
                        val faultJsonString = Gson().toJson(faultConfig[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, faultJsonString).apply()
                        return Triple(requestSheetTest.requestSheet(pagination),
                                pagination,
                                faultyPaginationSheetWithThrowTokenValidationExceptionMap[pagination])
                    }
                }
            }
        }))
    }

    @Test
    fun testRequestSheetWithCorrectPagination() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Any?, Pagination, Any?>>()
        Observable.fromIterable(testParameter.getCorrectSheets()
                .map {
                    val triple = testParameter.getValidSheet(it)
                    triple.first.map {
                        Triple(it, triple.second, triple.third)
                    }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertSubscribed()
                .assertNoErrors()
                .assertComplete()
        testObserver.values().forEach {
            Assert.assertEquals(it.second.toString(), it.third, it.first)
        }
    }

    @Test
    fun testRequestSheetWithInCorrectPaginationWithThrowTokenValidationException() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Notification<out Any?>, Pagination, Throwable?>>()
        Observable.fromIterable(testParameter.getNoSheets()
                .map {
                    val triple = testParameter.getInValidSheetWithThrowTokenValidationException(it)
                    triple.first.materialize().map { Triple(it, triple.second, triple.third) }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertSubscribed()
                .assertNoErrors()
                .assertComplete()
        testObserver.values().forEach {
            Assert.assertTrue(it.second.toString(), it.first.isOnError)
            Assert.assertTrue(it.second.toString(), it.first.error is TokenExpiredException)
//            Assert.assertEquals(it.second.toString(),
//                    it.third?.message, it.first.error?.cause?.message)  //FIXME : initiate throwable object
        }
    }

    @Test
    fun testRequestSheetWithInValidSheetPagination() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Notification<out Any?>, Pagination, Throwable?>>()
        Observable.fromIterable(testParameter.getNoSheets()
                .map {
                    val triple = testParameter.getInValidSheet(it)
                    triple.first.materialize().map { Triple(it, triple.second, triple.third) }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertSubscribed()
                .assertNoErrors()
                .assertComplete()

        testObserver.values().forEach {
            Assert.assertTrue(it.second.toString(), it.first.isOnError)
            Assert.assertTrue(it.second.toString(), it.first.error is SheetNotFoundException)
//            Assert.assertEquals(it.second.toString(),
//                    it.third?.message, it.first.error?.cause?.message)  //FIXME : initiate throwable object
        }
    }

    interface TestParameter<out T> {
        fun getCorrectSheets(): Set<Pagination>
        fun getValidSheet(pagination: Pagination): Triple<Observable<out T>, Pagination, T?>
        fun getNoSheets(): Set<Pagination>
        fun getInValidSheet(pagination: Pagination): Triple<Observable<out T>, Pagination, Throwable?>
        fun getInValidSheetWithThrowTokenValidationException(pagination: Pagination): Triple<Observable<out T>, Pagination, Throwable?>
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }
}