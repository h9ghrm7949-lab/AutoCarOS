package com.autocar.launcher.core.util

import java.lang.AutoCloseable

interface SafeCloseable : AutoCloseable {
    override fun close()
}