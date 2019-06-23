package com.transporter.streetglide.models.services

import android.support.annotation.VisibleForTesting
import com.transporter.streetglide.models.*
import io.reactivex.Observable


open class ValidationService(private val sheetDiskRepository: Repository<Sheet, Sheet>,
                             val scannedBarcodeRepository: ScannedBarcodeRepository<ScannedBarcode, ScannedBarcode>) {

    open fun verifyBarcode(barcode: String): Observable<Pair<Boolean, Boolean>> {
        return isBarcodeScannedBefore(barcode).flatMap { scannedBefore ->
            getAndSaveBarcodeFoundInSheet(barcode).map { Pair(scannedBefore, it) }
        }
    }

    fun isThereAnyScannedShipmentNotFoundInSheet(): Observable<Boolean> {
        return scannedBarcodeRepository.getAll(Pagination(Condition("found", Operator.Equal, false)))
                .map { scannedBarcodeList: List<ScannedBarcode> ->
                    !scannedBarcodeList.isEmpty()
                }
    }

    open fun getScannedShipmentsCount(): Observable<Long> =
            scannedBarcodeRepository.getCount()

    private fun getAndSaveBarcodeFoundInSheet(barcode: String): Observable<Boolean> =
            isFoundInSheet(barcode).flatMap {
                saveScannedBarcode(barcode, it)
            }.map {
                it.found
            }

    private fun isFoundInSheet(barcode: String): Observable<Boolean> {
        return sheetDiskRepository.get(1).map { sheet: Sheet ->
            sheet.hasShipment(barcode)
        }
    }

    private fun isBarcodeScannedBefore(barcode: String): Observable<Boolean> =
            scannedBarcodeRepository.getAll(Pagination(Condition("barcode", Operator.Equal, barcode)))
                    .map { scannedBarcodeList: List<ScannedBarcode> ->
                        !scannedBarcodeList.isEmpty()
                    }

    @VisibleForTesting
    fun saveScannedBarcode(barcode: String, found: Boolean): Observable<out ScannedBarcode> =
            scannedBarcodeRepository.insertOrUpdate(ScannedBarcode(barcode.toInt(), barcode, found))
}