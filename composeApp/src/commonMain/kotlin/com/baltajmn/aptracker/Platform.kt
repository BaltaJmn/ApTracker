package com.baltajmn.aptracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform