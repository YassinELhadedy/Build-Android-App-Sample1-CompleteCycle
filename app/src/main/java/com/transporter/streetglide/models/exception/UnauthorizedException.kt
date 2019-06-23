package com.transporter.streetglide.models.exception

import com.transporter.streetglide.models.ModelException

class UnauthorizedException : ModelException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}