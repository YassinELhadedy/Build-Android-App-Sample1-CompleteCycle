package com.transporter.streetglide.ui.helpers

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.TextView

/**
 *Created by yassin on 6/13/18.
 */
class Helper {
    companion object {
        fun isThereInternetConnection(context: Context): Boolean {
            val isConnected: Boolean
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
            return isConnected
        }

        fun showSnackBar(view: View, message: String) {
            val snackbar = Snackbar
                    .make(view, message, Snackbar.LENGTH_LONG)

            // Changing message text color
            snackbar.setActionTextColor(Color.RED)
            // Changing action button text color
            val sbView = snackbar.view
            val textView = sbView.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
            textView.setTextColor(Color.YELLOW)
            snackbar.show()
        }
    }
}