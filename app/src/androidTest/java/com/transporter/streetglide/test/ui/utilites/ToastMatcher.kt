package com.transporter.streetglide.test.ui.utilites

import android.os.IBinder
import android.support.test.espresso.Root
import android.view.WindowManager
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher



// Custom matcher to assert Toast msg isDisplayed
class ToastMatcher : TypeSafeMatcher<Root>() {

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    public override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams.get().type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {  /* This constant was deprecated in API level 26.
                                                                The alternative is not supported by API level 21. */
            val windowToken: IBinder = root.decorView.windowToken
            val appToken: IBinder = root.decorView.applicationWindowToken
            if (windowToken == appToken) {
                return true
            }
        }
        return false
    }

    companion object {
        val isToast: Matcher<Root>
            get() = ToastMatcher()

    }
}