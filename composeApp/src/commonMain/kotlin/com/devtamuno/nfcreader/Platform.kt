package com.devtamuno.nfcreader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform