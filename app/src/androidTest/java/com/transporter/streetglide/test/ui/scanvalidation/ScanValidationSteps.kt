package com.transporter.streetglide.test.ui.scanvalidation

import android.app.Activity
import android.content.ComponentName
import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.*
import com.transporter.streetglide.R
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dao.DaoSession
import com.transporter.streetglide.models.ScannedBarcode
import com.transporter.streetglide.ui.discrepancyreport.DiscrepancyActivity
import com.transporter.streetglide.ui.runsheet.StartTripActivity
import com.transporter.streetglide.ui.scanvalidation.ScanActivity
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.And
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import org.junit.Assert.assertNotNull
import org.junit.Rule


/**
 * one test failed because it depend on un implemented story
 */

class ScanValidationSteps {

    private lateinit var activity: Activity
    private lateinit var daoSession: DaoSession

    @Rule
    var activityTestRule = IntentsTestRule(ScanActivity::class.java)

    @Before
    fun setup() {
        activityTestRule.launchActivity(null)
        activity = activityTestRule.activity
        assertNotNull(activity)
        val openHelper = DaoMaster.DevOpenHelper(activity, null)
        daoSession = DaoMaster(openHelper.writableDb).newSession()
    }

    @After
    fun tearDown() {
        activityTestRule.finishActivity()
    }

    @Given("^There was a discrepancy between scanned shipments and requested sheet$")
    @Throws(Throwable::class)
    fun thereWasDiscrepancy() {
        val nonExistingBarcode1 = "123453"
        val nonExistingBarcode2 = "432127"
        val existingBarcode = "123456789"

        val scannedBarcodes = listOf(ScannedBarcode(nonExistingBarcode1.toInt(), nonExistingBarcode1, false),
                ScannedBarcode(existingBarcode.toInt(), existingBarcode, true),
                ScannedBarcode(nonExistingBarcode2.toInt(), nonExistingBarcode2, false))
        val daoScannedBarcodes = scannedBarcodes.map { it.toDaoScannedBarcode() }
        for (daoScannedBarcode in daoScannedBarcodes) {
            daoSession.daoScannedBarcodeDao.insert(daoScannedBarcode)
        }
    }

    @And("^Runner has finished scanning shipments$")
    @Throws(Throwable::class)
    fun runnerFinishedScanning() {
        onView(withText(R.string.btn_scan_done_scanning)).check(matches(isDisplayed())).check(matches(isClickable()))
        onView(withText(R.string.btn_scan_done_scanning)).perform(click())
    }

    @Then("^Runner should be navigated to discrepancy report screen$")
    @Throws(Throwable::class)
    fun runnerShouldSeeDiscrepancyReport() {
        intended(hasComponent(ComponentName(getTargetContext(), DiscrepancyActivity::class.java)))
    }

    @Given("^There was no discrepancy$")
    @Throws(Throwable::class)
    fun thereWasNoDiscrepancy() {
        val existingBarcode = "1234567"
        val scannedBarcode = ScannedBarcode(existingBarcode.toInt(), existingBarcode, true)
        daoSession.daoScannedBarcodeDao.insert(scannedBarcode.toDaoScannedBarcode())
    }

    @Then("^Runner should be able to start the trip$")
    @Throws(Throwable::class)
    fun runnerShouldBeAbleToStartTheTrip() {
        intended(hasComponent(ComponentName(getTargetContext(), StartTripActivity::class.java)))
    }
}