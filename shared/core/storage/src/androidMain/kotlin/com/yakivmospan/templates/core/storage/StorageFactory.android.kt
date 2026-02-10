package com.yakivmospan.templates.core.storage

actual fun defaultStorageFactoryProvider(): StorageFactory = AndroidStorageFactory()

class AndroidStorageFactory : StorageFactory {
    override fun createDatabase(): Any {
        return "Android Storage Stub"
    }
}