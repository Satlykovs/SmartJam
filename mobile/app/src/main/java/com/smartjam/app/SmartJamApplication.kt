package com.smartjam.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class SmartJamApplication : Application(), ImageLoaderFactory {

    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
}
