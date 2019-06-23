package com.transporter.streetglide.ui.exception

import android.content.Context
import com.transporter.streetglide.R
import com.transporter.streetglide.models.exception.UnauthorizedException
import com.transporter.streetglide.models.exception.*

/**
 * Factory used to create error messages from an Exception as a condition.
 */
class ErrorMessageFactory {
    companion object {
        fun create(context: Context, t: Throwable): String {
            var message = context.getString(R.string.exception_message_generic)

            when (t) {
                is UnauthorizedException -> {
                    message = if (t.message == "[Invalid Credentials]") context.getString(R.string.exception_message_user_not_found)
                    else context.getString(R.string.exception_message_user_inactive)
                }
                is TokenExpiredException -> message = context.getString(R.string.token_expired)
                is SheetNotFoundException -> message = context.getString(R.string.sheet_not_existed)
                is UserNotFoundException -> message = context.getString(R.string.user_not_existed)
            }
            return message
        }
    }
}
