package com.transporter.streetglide.test.ui.loginfeature

import android.app.Activity
import android.content.ComponentName
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import com.transporter.streetglide.ui.nocurrentsheet.NoCurrentSheetActivity
import com.transporter.streetglide.ui.runnerlogin.LoginActivity
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import junit.framework.Assert.assertNotNull
import org.hamcrest.Matchers.allOf
import org.junit.Rule



class LoginSteps {

    @Rule
    val activityTestRule = ActivityTestRule(LoginActivity::class.java)

    private lateinit var activity: Activity
    @Before
    fun setup() {
        activityTestRule.launchActivity(null)
        activity = activityTestRule.activity
    }

    @After
    fun tearDown() {
        activityTestRule.finishActivity()
    }

    @Given("^Runner at the login page$")
    @Throws(Throwable::class)
    fun runnerAtLoginPage() {
        assertNotNull(activity)
    }

    @When("^He inserts his credentials successfully$")
    @Throws(Throwable::class)
    fun iInsertAsUsername() {
        onView(withHint("اسم المستخدم")).perform(typeText("omar"))
        closeSoftKeyboard()
        onView(withHint("الرقم السري")).perform(typeText("adel1234"))
        closeSoftKeyboard()
        onView(withText("دخول")).perform(click())
    }

    @Then("^He will be logged to the app successfully$")
    @Throws(Throwable::class)
    fun loggedSuccessfully() {
//        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText("لديك شيت اليوم")))
//                .check(matches(isDisplayed()))
        Intents.intended(IntentMatchers.hasComponent(ComponentName(InstrumentationRegistry.getTargetContext(), NoCurrentSheetActivity::class.java)))

    }

    @When("^Runner login in using ([^/*]*)$")
    @Throws(Throwable::class)
    fun loggedUnSuccessfully(credential: String) {
        when (credential) {
            "wrong credentials" -> {
                onView(withHint("اسم المستخدم")).perform(typeText("invalidUser"))
                closeSoftKeyboard()
                onView(withHint("الرقم السري")).perform(typeText("invalidPassword"))
                closeSoftKeyboard()
                onView(withText("دخول")).perform(click())
            }
            "deleted user credentials" -> {
                onView(withHint("اسم المستخدم")).perform(typeText("farghaly"))
                closeSoftKeyboard()
                onView(withHint("الرقم السري")).perform(typeText("adel1234"))
                closeSoftKeyboard()
                onView(withText("دخول")).perform(click())
            }
            "missing credentials" -> {
                onView(withHint("اسم المستخدم")).perform(typeText(""))
                closeSoftKeyboard()
                onView(withHint("الرقم السري")).perform(typeText("adel1234"))
                closeSoftKeyboard()
                onView(withText("دخول")).perform(click())
            }
            "inactive user credentials" -> {
                onView(withHint("اسم المستخدم")).perform(typeText("khaled"))
                closeSoftKeyboard()
                onView(withHint("الرقم السري")).perform(typeText("adel1234"))
                closeSoftKeyboard()
                onView(withText("دخول")).perform(click())
            }
        }
    }

    @Then("^This ([^/*]*) is displayed$")
    @Throws(Throwable::class)
    fun showErrorMessage(msg: String) {
        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText(msg)))
                .check(matches(isDisplayed()))
    }
}