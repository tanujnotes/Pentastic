package app.pentastic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform