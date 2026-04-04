package info.meuse24.m24bikestats.presentation.login

import android.content.Context
import info.meuse24.m24bikestats.R

interface LoginStringResolver {
    fun cancelled(): String
}

class AndroidLoginStringResolver(
    private val context: Context,
) : LoginStringResolver {
    override fun cancelled(): String = context.getString(R.string.login_cancelled)
}
