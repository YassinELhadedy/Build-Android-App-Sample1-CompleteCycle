package com.transporter.streetglide.models

/**
 * SupportTransaction
 */
interface SupportTransaction {
    fun <T> doInTransaction(operation: () -> T): T
}