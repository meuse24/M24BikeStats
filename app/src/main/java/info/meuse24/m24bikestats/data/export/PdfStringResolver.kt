package info.meuse24.m24bikestats.data.export

import android.content.Context
import androidx.annotation.StringRes

interface PdfStringResolver {
    fun get(@StringRes resId: Int, args: Array<out Any> = emptyArray()): String
}

class AndroidPdfStringResolver(
    private val context: Context,
) : PdfStringResolver {
    override fun get(resId: Int, args: Array<out Any>): String =
        context.getString(resId, *args)
}
