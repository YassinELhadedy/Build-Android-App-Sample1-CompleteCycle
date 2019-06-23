package com.transporter.streetglide.test.ui.utilites

import android.view.View
import android.widget.EditText

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


class ErrorTextMatcher private constructor(private val expectedError: String) : TypeSafeMatcher<View>() {

    public override fun matchesSafely(view: View): Boolean {
        return view is EditText && expectedError == view.error
    }

    override fun describeTo(description: Description) {
        description.appendText("with error: $expectedError")
    }

    companion object {

        fun hasErrorText(expectedError: String): Matcher<in View> {
            return ErrorTextMatcher(expectedError)
        }
    }
}
