package com.noise.synccore

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform