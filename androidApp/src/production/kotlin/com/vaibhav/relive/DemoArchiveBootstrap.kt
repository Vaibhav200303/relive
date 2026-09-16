package com.vaibhav.relive

import android.content.Context
import com.vaibhav.relive.di.ReliveAppContainer
import kotlinx.coroutines.CoroutineScope

/** Production deliberately contains no demo archive implementation or reset hook. */
object DemoArchiveBootstrap {
    fun prepare(context: Context, container: ReliveAppContainer, scope: CoroutineScope) = Unit
}
