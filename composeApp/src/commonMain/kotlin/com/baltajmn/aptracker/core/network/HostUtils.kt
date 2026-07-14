package com.baltajmn.aptracker.core.network

fun sanitizeHost(raw: String): String = raw.trim()
    .removePrefix("wss://")
    .removePrefix("ws://")
    .removePrefix("https://")
    .removePrefix("http://")
    .substringBefore("/")
    .substringBefore(":")
    .trim()
