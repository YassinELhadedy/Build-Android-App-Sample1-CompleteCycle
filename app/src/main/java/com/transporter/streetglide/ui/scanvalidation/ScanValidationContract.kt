package com.transporter.streetglide.ui.scanvalidation

import com.transporter.streetglide.ui.BasePresenter
import com.transporter.streetglide.ui.BaseView


interface ScanValidationContract {

    interface View : BaseView {
        fun showErrorMsgShipmentNotInSheet()
        fun showErrorMsgShipmentAlreadyScanned()
        fun startScan()
        fun showShipmentsCount(count: Int)
        fun navigateToDiscrepancyReport()
        fun navigateToStartTrip()
    }

    interface Presenter : BasePresenter<View> {
        fun verifyBarcode(barcode: String)
        fun checkDiscrepancy()
    }
}