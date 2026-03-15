package org.kalpeshbkundanani.burnmate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform