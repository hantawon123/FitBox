package com.ssafy.fitbox.util

import com.ssafy.fitbox.network.RetrofitClient

object ImageUrlHelper {

    fun getFullImageUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) {
            return null
        }

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }

        val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
        val path = imageUrl.trimStart('/')

        return "$baseUrl/$path"
    }
}