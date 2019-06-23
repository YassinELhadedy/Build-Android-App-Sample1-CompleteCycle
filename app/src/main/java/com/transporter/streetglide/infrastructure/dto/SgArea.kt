package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.models.Area

/**
 *Created by yassin on 5/13/18.
 */
class SgArea(val id: Int, val name: String, val city: String, val governorate: String) {
    companion object {
        fun Area.toSgArea(): SgArea = SgArea(id, name, city, governorate)
    }

    fun toArea(): Area = Area(id, name, city, governorate)
}