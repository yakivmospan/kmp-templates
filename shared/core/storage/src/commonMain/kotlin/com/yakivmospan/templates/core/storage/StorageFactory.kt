package com.yakivmospan.templates.core.storage

interface StorageFactory {
    fun createDatabase(): Any // Placeholder
}

expect fun defaultStorageFactoryProvider(): StorageFactory