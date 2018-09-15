package uk.co.nickthecoder.tedi

private fun getOs(): String = System.getProperty("os.name").toLowerCase()

val isMac by lazy { getOs().startsWith("mac") }
val isLinux by lazy { getOs().startsWith("linux") }
val isWindows by lazy { getOs().startsWith("windows") }
