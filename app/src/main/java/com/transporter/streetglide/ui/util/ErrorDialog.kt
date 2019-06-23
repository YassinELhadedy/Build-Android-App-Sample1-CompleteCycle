package com.transporter.streetglide.ui.util

import android.content.Context
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import com.transporter.streetglide.R


open class ErrorDialog {

    companion object {
        fun show(context: Context, message: String) {
            val sweetAlertDialog = SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
            sweetAlertDialog.setTitleText(context.getString(R.string.title_scan_failure))
                    .setContentText(message)
                    .setConfirmClickListener {
                        sweetAlertDialog.dismissWithAnimation()
                    }.show()
        }
    }
}