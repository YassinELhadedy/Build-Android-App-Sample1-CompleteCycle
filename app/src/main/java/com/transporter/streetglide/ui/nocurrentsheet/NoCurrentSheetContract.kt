package com.transporter.streetglide.ui.nocurrentsheet

import com.transporter.streetglide.models.Pagination
import com.transporter.streetglide.ui.BasePresenter
import com.transporter.streetglide.ui.BaseView


interface NoCurrentSheetContract {

    interface View : BaseView {
        fun showLoading()
        fun hideLoading()
        fun showError(error: Throwable)
        fun showValid(messageKey: Int)
    }

    interface Presenter : BasePresenter<View> {
        fun requestSheet(pagination: Pagination)
    }
}