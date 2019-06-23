package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.dto.SgUser.Companion.toSgUser
import com.transporter.streetglide.models.User

/**
 * Created by yassin on 10/18/17.
 * SgUserWithAddressesAndPhonesAndAreasAndCapacities
 */
data class SgUserWithAddressesAndPhonesAndAreasAndCapacities(val info: SgUser) {
    companion object {
        fun User.toSgUserWithAddressesAndPhonesAndAreasAndCapacities() =
                SgUserWithAddressesAndPhonesAndAreasAndCapacities(toSgUser())
    }
}