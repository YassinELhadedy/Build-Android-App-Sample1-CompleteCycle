package com.transporter.streetglide.model.services

import android.content.Context
import android.content.SharedPreferences
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.UserSuperGlideRepository
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.TokenExpiredException
import com.transporter.streetglide.models.exception.UserNotFoundException
import com.transporter.streetglide.models.services.GetRunnerIdService
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.TimeUnit

/**
 *Created by yassin on 6/12/18.
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class GetRunnerIdServiceUnitTest(private val setupTestParameter: GetRunnerIdServiceUnitTest.SetupTestParameter<*>) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : SetupTestParameter<User> {
                    override fun setup(): TestParameter<User> {
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val correctUserIdsMap = hashMapOf(1290 to User("محمد احمد محمد احمد", "محمد احمد", null, true, "101"))
                        val faultUserIdsMap = hashMapOf(1989 to UserNotFoundException("user Not Found 404"))
                        val correctUserIdsThrowException = hashMapOf(1290 to TokenExpiredException("Unauthorized 401"))
                        val mockUserSuperGlideRepository = Mockito.mock(UserSuperGlideRepository::class.java)

                        return object : TestParameter<User> {
                            override fun runnerIdWithCorrectRunners(): Set<Int> = correctUserIdsMap.keys

                            override fun getCorrectRunner(userId: Int): Triple<Observable<out User>, Int, User?> {
                                Mockito.`when`(mockUserSuperGlideRepository
                                        .get(userId))
                                        .thenReturn(Observable.just(User("محمد احمد محمد احمد", "محمد احمد", null, true, "101")))

                                val mockGetRunnerIdService = GetRunnerIdService(mockUserSuperGlideRepository, ConfigurationRepository(sharedPreference))

                                return Triple(mockGetRunnerIdService.getRunnerId(userId), userId, correctUserIdsMap[userId])
                            }

                            override fun runnerIdWithFaultRunners(): Set<Int> = faultUserIdsMap.keys

                            override fun getFaultRunner(userId: Int): Triple<Observable<out User>, Int, Throwable?> {
                                val emptyResponseBody = ResponseBody.create(null, "HTTP 404 NotFound")
                                val errorResponse = Response.error<Any?>(404, emptyResponseBody)
                                Mockito.`when`(mockUserSuperGlideRepository
                                        .get(userId))
                                        .thenReturn(Observable.error { HttpException(errorResponse) })

                                val mockGetRunnerIdService = GetRunnerIdService(mockUserSuperGlideRepository, ConfigurationRepository(sharedPreference))

                                return Triple(mockGetRunnerIdService.getRunnerId(userId), userId, faultUserIdsMap[userId])
                            }

                            override fun getCorrectRunnerWithThrowTokenValidationException(userId: Int): Triple<Observable<out User>, Int, Throwable?> {
                                val emptyResponseBody = ResponseBody.create(null, "HTTP 401 Unauthorized")
                                val errorResponse = Response.error<Any?>(401, emptyResponseBody)

                                Mockito.`when`(mockUserSuperGlideRepository
                                        .get(userId))
                                        .thenReturn(Observable.error { HttpException(errorResponse) })

                                val mockGetRunnerIdService = GetRunnerIdService(mockUserSuperGlideRepository, ConfigurationRepository(sharedPreference))

                                return Triple(mockGetRunnerIdService.getRunnerId(userId), userId, correctUserIdsThrowException[userId])
                            }
                        }
                    }
                })
        )
    }

    @Test
    fun testGetCorrectRunner() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Any?, Int, Any?>>()
        Observable.fromIterable(testParameter.runnerIdWithCorrectRunners()
                .map {
                    val triple = testParameter.getCorrectRunner(it)
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
    fun testGetFaultRunner() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Notification<out Any?>, Int, Throwable?>>()
        Observable.fromIterable(testParameter.runnerIdWithFaultRunners()
                .map {
                    val triple = testParameter.getFaultRunner(it)
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
            Assert.assertTrue(it.second.toString(), it.first.error is UserNotFoundException)
//            Assert.assertEquals(it.second.toString(),
//                    it.third?.message, it.first.error?.cause?.message)  //FIXME : initiate throwable object
        }
    }

    @Test
    fun testGetCorrectRunnerWithThrowTokenValidationException() {

        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Notification<out Any?>, Int, Throwable?>>()
        Observable.fromIterable(testParameter.runnerIdWithCorrectRunners()
                .map {
                    val triple = testParameter.getCorrectRunnerWithThrowTokenValidationException(it)
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

    interface TestParameter<out T> {
        fun runnerIdWithCorrectRunners(): Set<Int>
        fun getCorrectRunner(userId: Int): Triple<Observable<out T>, Int, T?>
        fun runnerIdWithFaultRunners(): Set<Int>
        fun getFaultRunner(userId: Int): Triple<Observable<out T>, Int, Throwable?>
        fun getCorrectRunnerWithThrowTokenValidationException(userId: Int): Triple<Observable<out T>, Int, Throwable?>
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }
}