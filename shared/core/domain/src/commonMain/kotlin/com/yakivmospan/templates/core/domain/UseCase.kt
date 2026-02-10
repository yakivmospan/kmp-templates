package com.yakivmospan.templates.core.domain

abstract class UseCase<in P, out R> {
    abstract suspend operator fun invoke(params: P): R
}