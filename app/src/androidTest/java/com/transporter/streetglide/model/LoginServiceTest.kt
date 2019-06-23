package com.transporter.streetglide.model

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.auth0.android.jwt.JWT
import com.transporter.streetglide.infrastructure.*
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.UnauthorizedException
import com.transporter.streetglide.models.services.LoginService
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
 *Created by yassin on 4/5/18.
 */
@RunWith(Parameterized::class)
class LoginServiceTest(private val setupTestParameter: SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(arrayOf(object : SetupTestParameter<SignedToken> {
            override fun setup(): TestParameter<SignedToken> {
                val tokenSuperGlideRestService =
                        TokenSuperGlideRepository(SuperGlideRestServiceFactory(MOCK_BASE_URL).service)
                val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                val login = LoginService(tokenSuperGlideRestService, ConfigurationRepository(sharedPreference))

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
                        val observable = login.login(username = user.username!!, password = user.password!!)
                        return Triple(observable, user, validUserMap[user])
                    }

                    override fun getInCorrectUsers(): Set<User> = inValidUserMap.keys

                    override fun loginUnauthorizedUser(user: User): Pair<Observable<out SignedToken>, Throwable?> {
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
                    triple.first.map {
                        Triple(it, triple.third, testParameter.checkTokenExpiration(it))
                    }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)
        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.values().forEach {
            val signedToken = it.first as SignedToken
            Assert.assertEquals(signedToken.name, it.second)
            Assert.assertEquals(it.third, false)
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