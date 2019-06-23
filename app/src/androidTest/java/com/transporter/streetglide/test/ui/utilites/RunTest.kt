package com.transporter.streetglide.test.ui.utilites

import android.support.test.filters.Suppress

import org.junit.runner.RunWith

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber


@Suppress
@RunWith(Cucumber::class)
@CucumberOptions(features = ["features"], glue = ["com.transporter.streetglide.test.ui"])
class RunTest