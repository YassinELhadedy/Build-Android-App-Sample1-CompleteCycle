package com.transporter.streetglide.infrastructure.dto

/**
 * SgErrorResult
 */
data class SgErrorResult(val type: String,
                         val errors: Map<String, List<String>>?)