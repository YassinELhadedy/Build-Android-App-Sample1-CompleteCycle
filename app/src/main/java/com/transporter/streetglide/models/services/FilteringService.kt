package com.transporter.streetglide.models.services

import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.models.Consignee
import com.transporter.streetglide.models.Status
import io.reactivex.Observable

class FilteringService(private val sheetDiskRepository: SheetDiskRepository) {

    fun filterConsignees(status: Status): Observable<List<Consignee>> {
        return sheetDiskRepository.get(1).map {
            it.filterConsigneeByStatusOfShipment(status)
        }
    }
}