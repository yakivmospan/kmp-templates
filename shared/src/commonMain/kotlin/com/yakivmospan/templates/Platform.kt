package com.yakivmospan.templates

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform