package com.vaibhav.relive

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform