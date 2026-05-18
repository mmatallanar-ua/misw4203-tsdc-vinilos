package com.misw4203.vinilos.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
)
annotation class IoDispatcher
