package com.transporter.streetglide.models

import io.reactivex.Observable


interface ScannedBarcodeRepository<in T, out U> : WriteRepository<T, U>, GetAllRepository<U> {
    fun getCount(): Observable<Long>
}