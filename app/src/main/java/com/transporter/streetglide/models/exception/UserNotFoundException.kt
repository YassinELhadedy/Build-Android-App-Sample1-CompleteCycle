package com.transporter.streetglide.models.exception

import com.transporter.streetglide.models.ModelException

/**
 * Exception throw by the application when a there is a network connection exception.
 */
class UserNotFoundException : ModelException {

    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}