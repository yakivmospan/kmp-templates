package com.yakivmospan.templates.core.storage

actual fun defaultStorageFactoryProvider(): StorageFactory = IOSStorageFactory()

class IOSStorageFactory : StorageFactory {
    override fun createDatabase(): Any {
        return "iOS Storage Stub"
    }
}