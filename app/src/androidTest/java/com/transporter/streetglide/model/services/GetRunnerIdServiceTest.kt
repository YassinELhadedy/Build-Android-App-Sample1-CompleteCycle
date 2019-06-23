package com.transporter.streetglide.model.services

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.*
import com.transporter.streetglide.infrastructure.dto.SgLogin
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.TokenExpiredException
import com.transporter.streetglide.models.exception.UserNotFoundException
import com.transporter.streetglide.models.services.GetRunnerIdService
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

/**
 *Created by yassin on 6/14/18.
 */
@RunWith(Parameterized::class)
class GetRunnerIdServiceTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : GetRunnerIdServiceTest.SetupTestParameter<User> {
            override fun setup(): TestParameter<User> {

                val superGlideRestService: SuperGlideRestService = SuperGlideRestServiceFactory(MOCK_BASE_URL).service
                val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                val signedToken = superGlideRestService.login(SgLogin(USER_NAME, PASS)).blockingFirst()
                val config = listOf(Configuration(signedToken.toSignedToken(), 1, "122"))
                val jsonString = Gson().toJson(config[0])
                sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                val configurationRepository = ConfigurationRepository(sharedPreference)
                val getRunnerIdService = GetRunnerIdService(UserSuperGlideRepository(superGlideRestService, configurationRepository), configurationRepository)

                val correctUserIdsMap = hashMapOf(1290 to User("محمد احمد محمد احمد", "محمد احمد", null, true, "101"))
                val faultUserIdsMap = hashMapOf(1989 to UserNotFoundException("user Not Found 404"))
                val correctUserIdsThrowException = hashMapOf(1290 to TokenExpiredException("Unauthorized 401"))
                return object : GetRunnerIdServiceTest.TestParameter<User> {
                    override fun runnerIdWithCorrectRunners(): Set<Int> = correctUserIdsMap.keys

                    override fun getCorrectRunner(userId: Int): Triple<Observable<out User>, Int, User?> {
                        return Triple(getRunnerIdService.getRunnerId(userId), userId, correctUserIdsMap[userId])
                    }

                    override fun runnerIdWithFaultRunners(): Set<Int> = faultUserIdsMap.keys

                    override fun getFaultRunner(userId: Int): Triple<Observable<out User>, Int, Throwable?> {
                        return Triple(getRunnerIdService.getRunnerId(userId), userId, faultUserIdsMap[userId])
                    }

                    override fun getCorrectRunnerWithThrowTokenValidationException(userId: Int): Triple<Observable<out User>, Int, Throwable?> {
                        sharedPreference.edit().clear().apply()
                        val faultSignedToken = SignedToken("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoi2LTYsdmK2YEg2KfZhNiv2YjZitin2KrZiiIsInVzZXJuYW1lIjoic2hlcmlmIiwiZXhwIjoxNTE4MTA4ODI1LCJpZCI6MTAxNSwidHlwZSI6NH0.IrtnijGwUEexMvF-apReIbBjK1vYRZ05ePA3EFZ0yfUlAoH026p8nOnO62RnFSPMJRi0FrASq-rG_CEFQmJdRA")
                        val faultConfig = listOf(Configuration(faultSignedToken, 1, "122"))
                        val faultJsonString = Gson().toJson(faultConfig[0])
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, faultJsonString).apply()
                        return Triple(getRunnerIdService.getRunnerId(userId), userId, correctUserIdsThrowException[userId])
                    }
                }
            }
        }))
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