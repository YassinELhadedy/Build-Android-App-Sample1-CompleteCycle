package com.transporter.streetglide.infrastructure.dto

/**
 * Created by yassin on 10/18/17.
 * SgPaginated
 */
data class SgPaginated<out T>(val totalCount: Int,
                              val result: T)