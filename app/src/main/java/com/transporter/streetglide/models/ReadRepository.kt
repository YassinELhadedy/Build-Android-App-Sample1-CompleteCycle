package com.transporter.streetglide.models

/**
 * Created by mcherri on 11/22/17.
 * Read Repository with Read Only Methods
 */
interface ReadRepository<out T> : GetRepository<T>, GetAllRepository<T>