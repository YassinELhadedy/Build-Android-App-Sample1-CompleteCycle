package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.dto.SgArea.Companion.toSgArea
import com.transporter.streetglide.models.Address

data class SgAddress(val areaId: Int,
                     val area: SgArea,
                     val street: String,
                     val propertyNumber: String?,
                     val floor: Int?,
                     val apartement: String?,
                     val specialMark: String?) {
    companion object {
        fun Address.toSgAddress(): SgAddress = SgAddress(areaId, area.toSgArea(),
                street,
                propertyNumber,
                floor,
                apartment,
                specialMark)
    }

    fun toAddress(): Address = Address(areaId, area.toArea(),
            street,
            propertyNumber,
            floor,
            apartement,
            specialMark)
}