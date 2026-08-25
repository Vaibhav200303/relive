package com.vaibhav.relive.platform.system

import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.UIKit.UIDevice
import platform.UIKit.UIApplication

actual fun platformAppInfo(): PlatformAppInfo {
    val bundle = NSBundle.mainBundle
    return PlatformAppInfo(
        version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unavailable",
        build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "Unavailable",
        platform = "iOS",
        osVersion = UIDevice.currentDevice.systemVersion,
    )
}

actual fun platformMailComposer(): MailComposer = object : MailComposer {
    override fun open(request: MailRequest): Boolean {
        val value = "mailto:${request.recipient}?subject=${request.subject}&body=${request.body}"
        val encoded = value.replace(" ", "%20").replace("\n", "%0A")
        val url = NSURL.URLWithString(encoded) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
}
