package com.yakivmospan.templates.core.biometrics

import androidx.fragment.app.FragmentActivity

interface BiometricsActivityProvider {
    fun getActivity(): FragmentActivity?
    fun setActivity(activity: FragmentActivity?)
}

class DefaultBiometricsActivityProvider : BiometricsActivityProvider {
    private var activity: FragmentActivity? = null

    override fun getActivity(): FragmentActivity? = activity
    override fun setActivity(activity: FragmentActivity?) {
        this.activity = activity
    }
}