package com.yakivmospan.templates.core.common

interface Mapper<IN, OUT> {
    fun map(input: IN): OUT
}