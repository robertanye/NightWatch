package com.dexdrip.stephenblack.nightwatch.sharemodels;

import com.dexdrip.stephenblack.nightwatch.model.ShareGlucose;

import java.util.Map;


import okhttp3.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.QueryMap;
import retrofit2.Callback;

/**
 * Created by stephenblack on 3/16/15.
 */
public interface DexcomShareInterface {
    @POST("/Publisher/ReadPublisherLatestGlucoseValues")
    ShareGlucose[] getShareBg(@QueryMap Map<String, String> options);

    @POST("/General/LoginPublisherAccountByName")
    void getSessionId(@Body ShareAuthenticationBody body, okhttp3.Callback callback);
    //Since this seems to respond with a string we need a callback that will parse the response body
    //new String(((TypedByteArray) response.getBody()).getBytes());
    @POST("/Publisher/PostReceiverEgvRecords")
    void uploadBGRecords(@QueryMap Map<String, String> options, @Body ShareUploadPayload payload, okhttp3.Callback callback);

}
