package com.transporter.streetglide.model

import android.content.Context
import android.content.SharedPreferences
import com.auth0.android.jwt.JWT
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.TokenSuperGlideRepository
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.UnauthorizedException
import com.transporter.streetglide.models.services.LoginService
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
 *Created by yassin on 4/5/18.
 */
@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class LoginServiceUnitTest(private val setupTestParameter: SetupTestParameter<*>) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<SignedToken> {
            override fun setup(): TestParameter<SignedToken> {
                val token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoi2KfYrdmF2K8g2LnYp9iv2YQg2YbZiNixIiwidXNlcm5hbWUiOiJvbWFyIiwiZXhwIjoxNTI1MDM1MTk1LCJpZCI6MTAxMCwidHlwZSI6NH0.raiHIMM0AoZytuukqcaQhhiXsoyYY2CYmCtWqBrvIyIxs3DEueTvzWqNTcyskgEbkeiV3p-XcyumF_K-lTIrow"
                val mockTokenSuperGlideRestService = Mockito.mock(TokenSuperGlideRepository::class.java)
                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                val login = LoginService(mockTokenSuperGlideRestService, ConfigurationRepository(sharedPreference))
                val signedToken5=  SignedToken(token)

                val validUserMap = hashMapOf(
                        User("احمد عادل نور", "omar", "adel1234", true, "") to "احمد عادل نور"
                )

                val inValidUserMap = hashMapOf(
                        User("", "farghaly", "adel1234", true, "") to UnauthorizedException("[Invalid Credentials]")
                )
                return object : TestParameter<SignedToken> {
                    override fun checkTokenExpiration(signedToken: SignedToken): Boolean {
                        return JWT(signedToken.token).isExpired(0)
                    }

                    override fun getCorrectUsers(): Set<User> = validUserMap.keys

                    override fun successfulLogin(user: User): Triple<Observable<out SignedToken>, User, String?> {
                        Mockito.`when`(mockTokenSuperGlideRestService.insert(User("", "omar", "adel1234", true, "")))
                                .thenReturn(Observable.just(
                                       signedToken5))
                        val observable = login.login(username = user.username!!, password = user.password!!)
                        return Triple(observable, user, validUserMap[user])
                    }

                    override fun getInCorrectUsers(): Set<User> = inValidUserMap.keys

                    override fun loginUnauthorizedUser(user: User): Pair<Observable<out SignedToken>, Throwable?> {
                        val responseBody = ResponseBody.create(null, "{\"type\":\"ValidationError\",\"errors\":{\"message\":[\"Invalid Credentials\"]}}")
                        val errorResponse = Response.error<Any?>(401, responseBody)

                        Mockito.`when`(mockTokenSuperGlideRestService.insert(user))
                                .thenReturn(Observable.error(HttpException(errorResponse)))
                        val observable = login.login(username = user.username!!, password = user.password!!)
                        return Pair(observable, inValidUserMap[user])
                    }
                }
            }

            override fun toString() = SignedToken::class.java.simpleName!!
        }))
    }

    @Test
    fun successfulLogin() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Triple<Any?, Any?, Any?>>()
        Observable.fromIterable(testParameter.getCorrectUsers()
                .map {
                    val triple = testParameter.successfulLogin(it)
                    triple.first.map { Triple(it, triple.third, testParameter.checkTokenExpiration(it)) }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.values().forEach {
            val signedToken = it.first as SignedToken
            Assert.assertEquals(signedToken.name, it.second)
            Assert.assertEquals(it.third, true)
        }
    }

    @Test
    fun loginUnauthorizedUser() {
        val testParameter = setupTestParameter.setup()
        val testObserver = TestObserver<Pair<Notification<out Any?>, Throwable?>>()
        Observable.fromIterable(testParameter.getInCorrectUsers()
                .map {
                    val pair = testParameter.loginUnauthorizedUser(it)
                    pair.first.materialize().map { Pair(it, pair.second) }
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
            Assert.assertTrue(it.second.toString(), it.first.error is UnauthorizedException)
//            Assert.assertEquals(it.second.toString(),
//                    it.third?.message, it.first.error?.cause?.message)  //FIXME : initiate throwable object
        }
    }

    interface TestParameter<out T> {
        fun getCorrectUsers(): Set<User>
        fun successfulLogin(user: User): Triple<Observable<out SignedToken>, User, String?>
        fun getInCorrectUsers(): Set<User>
        fun loginUnauthorizedUser(user: User): Pair<Observable<out T>, Throwable?>
        fun checkTokenExpiration(signedToken: SignedToken): Boolean
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }
}