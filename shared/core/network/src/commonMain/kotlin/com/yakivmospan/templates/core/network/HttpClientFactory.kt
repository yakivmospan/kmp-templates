package com.yakivmospan.templates.core.network

import io.ktor.client.HttpClient

interface HttpClientFactory {
    fun create(): HttpClient
}

expect fun defaultHttpClientFactoryProvider(): HttpClientFactory
