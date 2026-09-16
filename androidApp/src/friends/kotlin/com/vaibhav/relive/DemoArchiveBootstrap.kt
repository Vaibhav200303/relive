package com.vaibhav.relive

import android.content.Context
import com.vaibhav.relive.di.ReliveAppContainer
import kotlinx.coroutines.CoroutineScope

/** Friends builds start with an empty, production-shaped local archive. */
object DemoArchiveBootstrap {
    fun prepare(context: Context, container: ReliveAppContainer, scope: CoroutineScope) = Unit
}
