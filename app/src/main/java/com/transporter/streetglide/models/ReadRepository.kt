package com.transporter.streetglide.models

/**
 * Read Repository with Read Only Methods
 */
interface ReadRepository<out T> : GetRepository<T>, GetAllRepository<T>