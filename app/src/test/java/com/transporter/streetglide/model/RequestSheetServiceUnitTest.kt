package com.transporter.streetglide.model

import android.content.Context
import android.content.SharedPreferences
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.InfrastructureException
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.SheetSuperGlideRepository
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.exception.SheetNotFoundException
import com.transporter.streetglide.models.exception.TokenExpiredException
import com.transporter.streetglide.models.services.RequestSheetService
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit

private const val DATA_ERROR = "Data Error!"

/**
 *Created by yassin on 4/24/18.
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class RequestSheetServiceUnitTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : SetupTestParameter<Boolean> {
                    override fun setup(): TestParameter<Boolean> {
                        return object : TestParameter<Boolean> {
                            val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)

                            val dateEX1 = 1463646433000L
                            val dateEX2 = 1486616999000L //Thu Feb 09 07:09:59 GMT+02:00 2017
                            val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX1))
                            val expr2 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX2))
                            val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())
                            val datetime = Date(Date().time / 1000 * 1000)
                            val sheet1 = Sheet(id = 1, barcode = "1594413926", runnerId = 4312, branchId = 1, datetime = datetime, isReturned = true, consignees = emptyList())
                            val sheet2 = Sheet(id = 2, barcode = "1594413927", runnerId = 4312, branchId = 1, datetime = datetime, isReturned = false, consignees = emptyList())

                            var requestSheetData: Boolean = true  // which default value
                            override val data: Boolean
                                get() = requestSheetData

                            override fun requestSheetAndCash(): Observable<out Boolean> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)
                                Mockito.`when`(mockRepository.insertOrUpdate(sheet2))
                                        .thenReturn(Observable.just(sheet2))


                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr1, sort1, 0, 1)))
                                        .thenReturn(Observable.just(listOf(sheet2)))
                                Mockito.`when`(mockSheetSuperGlideRepository.get(2))
                                        .thenReturn(Observable.just(sheet2))

                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))

                                requestSheetData = true
                                return mockRequestSheetService.requestSheet(Pagination(expr1, sort1, 0, 1)).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSheetSuperGlideRepository, Mockito.only())
                                            .get(ArgumentMatchers.eq(2))

                                    Mockito.verify(mockRepository, Mockito.only())
                                            .insertOrUpdate(sheet2)
                                }
                            }

                            override fun requestSheetReturned(): Observable<out Boolean> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)

                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr2, sort1, 0, 1)))
                                        .thenReturn(Observable.just(listOf(sheet1)))

                                requestSheetData = false
                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))
                                return mockRequestSheetService.requestSheet(Pagination(expr2, sort1, 0, 1))
                            }

                            override fun requestSheetNotFoundException(): Observable<out Boolean> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)
                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr2, sort1, 0, 1)))
                                        .thenReturn(Observable.error(IndexOutOfBoundsException()))


                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))
                                return mockRequestSheetService.requestSheet(Pagination(expr2, sort1, 0, 1))
                            }

                            override fun requestSheetWhichTokenIsExpiredException(): Observable<out Boolean> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)
                                val emptyResponseBody = ResponseBody.create(null, "HTTP 401 Unauthorized")
                                val errorResponse = Response.error<Any?>(401, emptyResponseBody)
                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr2, sort1, 0, 1)))
                                        .thenReturn(Observable.error(InfrastructureException(HttpException(errorResponse))))


                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))
                                return mockRequestSheetService.requestSheet(Pagination(expr2, sort1, 0, 1))
                            }

                            override fun requestSheetWhileCashingException(): Observable<out Boolean> {
                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)
                                Mockito.`when`(mockRepository.insertOrUpdate(sheet2))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))


                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr1, sort1, 0, 1)))
                                        .thenReturn(Observable.just(listOf(sheet2)))
                                Mockito.`when`(mockSheetSuperGlideRepository.get(2))
                                        .thenReturn(Observable.just(sheet2))

                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))

                                requestSheetData = false
                                return mockRequestSheetService.requestSheet(Pagination(expr1, sort1, 0, 1)).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSheetSuperGlideRepository, Mockito.only())
                                            .get(ArgumentMatchers.eq(2))

                                    Mockito.verify(mockRepository, Mockito.only())
                                            .insertOrUpdate(sheet2)
                                }
                            }

                            override fun requestSheetWithGetByIdServiceException(): Observable<out Boolean> {
                                val emptyResponseBody = ResponseBody.create(null, "")
                                val errorResponse = Response.error<Any?>(500, emptyResponseBody)

                                val mockRepository = Mockito.mock(SheetDiskRepository::class.java)

                                val mockSheetSuperGlideRepository = mock(SheetSuperGlideRepository::class.java)
                                Mockito.`when`(mockSheetSuperGlideRepository
                                        .getAll(Pagination(expr1, sort1, 0, 1)))
                                        .thenReturn(Observable.just(listOf(sheet2)))
                                Mockito.`when`(mockSheetSuperGlideRepository.get(2))
                                        .thenReturn(Observable.error(HttpException(errorResponse)))

                                val mockRequestSheetService = RequestSheetService(mockSheetSuperGlideRepository, mockRepository, ConfigurationRepository(sharedPreference))
                                return mockRequestSheetService.requestSheet(Pagination(expr1, sort1, 0, 1)).doOnComplete {
                                    // Verify that Superglide was called once
                                    Mockito.verify(mockSheetSuperGlideRepository, Mockito.only())
                                            .get(ArgumentMatchers.eq(2))
                                }
                            }
                        }
                    }
                })
        )
    }
//   //FIXME: Caused by: org.mockito.exceptions.verification.NoInteractionsWanted
//    @Test
//    fun testSheetNotReturnedCaseAndCashItCase() {
//
//        val testParameter = setupTestParameter.setup()
//        val testObserver = TestObserver<Any>()
//        testParameter.requestSheetAndCash()
//                .subscribeOn(Schedulers.io())
//                .subscribe(testObserver)
//
//        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
//        testObserver.assertResult(testParameter.data)
//
//    }

    @Test
    fun testSheetIsReturnedCase() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.requestSheetReturned()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertResult(testParameter.data)
    }

    @Test
    fun testSheetNotFoundInSuperglideCase() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.requestSheetNotFoundException()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError(SheetNotFoundException::class.java)
    }

    @Test
    fun testRequestSheetWhichTokenIsExpiredExceptionCase() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.requestSheetWhichTokenIsExpiredException()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError(TokenExpiredException::class.java)
    }
//   //FIXME: Caused by: org.mockito.exceptions.verification.NoInteractionsWanted
//    @Test
//    fun testRequestSheetWhileCashingExceptionCase() {
//
////        val testParameter = setupTestParameter.setup()
////        val testObserver = TestObserver<Any>()
////        testParameter.requestSheetWhileCashingException()
////                .subscribeOn(Schedulers.io())
////                .subscribe(testObserver)
////
////        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
////        testObserver.assertResult(testParameter.data)
//
//    }

    @Test
    fun testRequestSheetWithGetByIdServiceExceptionCase() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Any>()
        testParameter.requestSheetWithGetByIdServiceException()
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertError(ModelException::class.java)
    }

    interface TestParameter<out T> {
        val data: T

        fun requestSheetAndCash(): Observable<out T>
        fun requestSheetReturned(): Observable<out T>
        fun requestSheetNotFoundException(): Observable<out T>
        fun requestSheetWhichTokenIsExpiredException(): Observable<out T>
        fun requestSheetWhileCashingException(): Observable<out T>
        fun requestSheetWithGetByIdServiceException(): Observable<out T>
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }
}