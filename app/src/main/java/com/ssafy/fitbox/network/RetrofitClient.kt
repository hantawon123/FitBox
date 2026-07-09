package com.ssafy.fitbox.network

import com.ssafy.fitbox.network.api.CartApi
import com.ssafy.fitbox.network.api.ChatApi
import com.ssafy.fitbox.network.api.IngredientApi
import com.ssafy.fitbox.network.api.UserApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java
import com.ssafy.fitbox.network.api.MealApi
import com.ssafy.fitbox.network.api.OrderApi
import com.ssafy.fitbox.network.api.StoreApi
import com.ssafy.fitbox.network.api.AddressApi
import com.ssafy.fitbox.network.api.NotificationApi

object RetrofitClient {

     const val BASE_URL = "http://192.168.33.113:8080/"

//     const val BASE_URL = "http://192.168.33.114:8080/" // 한준꺼

//     const val BASE_URL = "http://192.168.219.107:8080/" // 한준 집

//    const val BASE_URL = "http://10.249.252.96:8080" //태원 집
    private const val GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/"

    private val chatRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GMS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val ingredientApi: IngredientApi by lazy {
        retrofit.create(IngredientApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }
    val cartApi: CartApi by lazy {
        retrofit.create(CartApi::class.java)
    }

    val mealApi: MealApi by lazy {
        retrofit.create(MealApi::class.java)
    }

    val orderApi: OrderApi by lazy {
        retrofit.create(OrderApi::class.java)
    }

    val storeApi: StoreApi by lazy {
        retrofit.create(StoreApi::class.java)
    }

    val addressApi: AddressApi by lazy {
        retrofit.create(AddressApi::class.java)
    }

    val notificationApi: NotificationApi by lazy {
        retrofit.create(NotificationApi::class.java)
    }

    val chatApi: ChatApi by lazy {
        chatRetrofit.create(ChatApi::class.java)
    }
}




