package com.transporter.streetglide.models

data class Address(val areaId: Int, val area: Area, val street: String, val propertyNumber: String?,
                   val floor: Int?, val apartment: String?, val specialMark: String?)