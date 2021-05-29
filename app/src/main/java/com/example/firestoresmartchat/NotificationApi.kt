package com.example.firestoresmartchat

import com.codingwithme.firebasechat.model.PushNotification
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import com.example.firestoresmartchat.Constants
import com.example.firestoresmartchat.Constants.Companion.CONTENT_TYPE
import com.example.firestoresmartchat.Constants.Companion.SERVER_KEY
import retrofit2.http.POST

interface NotificationApi {

    @Headers("Authorization: key=$SERVER_KEY","Content-type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification:PushNotification
    ): Response<ResponseBody>
}