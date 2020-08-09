package com.transporter.streetglide.test.ui.utilites

import android.os.Bundle
import android.support.test.runner.AndroidJUnitRunner

import cucumber.api.android.CucumberInstrumentationCore

class CucumberInstrumentationRunner : AndroidJUnitRunner() {

    private val instrumentationCore = CucumberInstrumentationCore(this)
    private var resultCode: Int = 0
    private lateinit var results: Bundle

    override fun onCreate(bundle: Bundle) {
        super.onCreate(bundle)
        instrumentationCore.create(bundle)
    }

    override fun onStart() {
        super.onStart()
        waitForIdleSync()
        instrumentationCore.start()
        super.finish(resultCode, results)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        this.resultCode = resultCode
        this.results = results
    }
}