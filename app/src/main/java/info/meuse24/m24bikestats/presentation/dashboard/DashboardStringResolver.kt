package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import androidx.annotation.StringRes

interface DashboardStringResolver {
    fun get(@StringRes resId: Int, args: Array<out Any>): String
}

class AndroidDashboardStringResolver(
    private val context: Context,
) : DashboardStringResolver {
    override fun get(resId: Int, args: Array<out Any>): String =
        context.getString(resId, *args)
}
