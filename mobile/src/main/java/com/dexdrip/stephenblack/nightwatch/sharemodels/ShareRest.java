package com.dexdrip.stephenblack.nightwatch.sharemodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.dexdrip.stephenblack.nightwatch.model.Bg;
import com.dexdrip.stephenblack.nightwatch.model.ShareGlucose;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ShareRest {
    private Context mContext;
    private String login;
    private String password;
    private SharedPreferences prefs;
    OkHttpClient client;

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public ShareRest(Context context) {
        client = new OkHttpClient();
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        login = prefs.getString("dexcom_account_name", "");
        password = prefs.getString("dexcom_account_password", "");
    }

    public boolean getBgData() {
        if (prefs.getBoolean("share_poll", false) && login.compareTo("") != 0 && password.compareTo("") != 0) {
            return loginAndGetData();
        } else {
            return false;
        }
    }
    public boolean sendBgData(Bg bg) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String receiverSn = preferences.getString("share_key", "SM00000000").toUpperCase();
        if (prefs.getBoolean("share_upload", false) && login.compareTo("") != 0 && password.compareTo("") != 0 && receiverSn.compareTo("SM00000000") != 0) {
            return loginAndSendData(bg);
        } else {
            return false;
        }
    }

    private boolean loginAndGetData() {
        try {
            dexcomShareAuthorizeInterface().getSessionId(new ShareAuthenticationBody(password, login), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("ShareRest", "Success!! got a response on auth.");
                    //String returnedSessionId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");

                    String returnedSessionId="Get";
                    getBgData(returnedSessionId);
                }

            });
            return true;
        } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "loginAndGetData " + e);
                    return false;
        }
    }

    private boolean loginAndSendData(final Bg bg) {
        try {
            dexcomShareAuthorizeInterface().getSessionId(new ShareAuthenticationBody(password, login), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("loginAndSendData", "Failed");

                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("ShareRest", "Success!! got a response on auth.");

                    response.body().bytes();
                    //String returnedSessionId = new String(((TypedByteArray)
                    //       response.body.byteStream().repla
                    //      response.getBody()).getBytes()).replace("\"", "");

                    String returnedSessionId = "not";
                    sendBgData(returnedSessionId, bg);
                }
            });
            return true;
        } catch (Exception e) {
            Log.e("REST CALL ERROR: ", "loginAndSendData " + e);
            return false;
        }
    }

    private void getBgData(String sessionId) {
        DataFetcher dataFetcher = new DataFetcher(mContext, sessionId);
        dataFetcher.execute((Void) null);
    }

    private void sendBgData(String sessionId, Bg bg) {
        DataSender dataSender = new DataSender(mContext, sessionId, bg);
        dataSender.execute((Void) null);
    }

    private DexcomShareInterface dexcomShareAuthorizeInterface() {
        Retrofit adapter = authorizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    private DexcomShareInterface dexcomShareGetBgInterface() {
        Retrofit adapter = getBgAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    private DexcomShareInterface dexcomShareSendBgInterface() {
        Retrofit adapter = authorizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    private DexcomShareInterface checkSessionActive() {
        Retrofit adapter = getBgAdapterBuilder().build();
        DexcomShareInterface checkSessionActive =
                adapter.create(DexcomShareInterface.class);
        return checkSessionActive;
    }

    private Retrofit.Builder authorizeAdapterBuilder() {
        Retrofit.Builder adapterBuilder = new Retrofit.Builder();
        OkHttpClient authClient = new OkHttpClient.Builder()
                .addInterceptor(authorizationRequestInterceptor)
                .build();

        adapterBuilder
                .client(authClient)
                .baseUrl("https://share1.dexcom.com/ShareWebServices/Services/")
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder() .excludeFieldsWithoutExposeAnnotation().create()));

        return adapterBuilder;
    }

    private Retrofit.Builder getBgAdapterBuilder() {
        Retrofit.Builder adapterBuilder = new Retrofit.Builder();
        OkHttpClient getBg = new OkHttpClient.Builder()
                .addInterceptor(getBgRequestInterceptor)
                .build();
        adapterBuilder
                .client(getBg)
                .baseUrl("https://share1.dexcom.com/ShareWebServices/Services/")
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder() .excludeFieldsWithoutExposeAnnotation().create()));
        return adapterBuilder;
    }

    Interceptor authorizationRequestInterceptor = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request newRequest;

            newRequest = request.newBuilder()
                    .header("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            okhttp3.Response response = chain.proceed(newRequest);

            return response;
        }
    };
    Interceptor getBgRequestInterceptor = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request newRequest;
            newRequest = request.newBuilder()
            .header("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0")
            .header("Content-Type", "application/json")
            .header("Content-Length", "0")
            .header("Accept", "application/json")
            .build();

            okhttp3.Response response = chain.proceed(newRequest);

            return response;
        }
    };

    public OkHttpClient getOkHttpClient() {

        try {
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.sslSocketFactory();


            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public Map<String, String> queryParamMap(String sessionId) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        map.put("minutes", String.valueOf(minutesCount()));
        map.put("maxCount", String.valueOf(requestCount()));
        return map;

    }

    public class DataFetcher extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mSessionId;
        DataFetcher(Context context, String sessionId) {
            mContext = context;
            mSessionId = sessionId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                try {
                    final ShareGlucose[] shareGlucoses = dexcomShareGetBgInterface().getShareBg(queryParamMap(mSessionId));
                    Log.d("REST Success: ", "YAY!");
                    if(shareGlucoses != null && shareGlucoses.length > 0) {
                        for (ShareGlucose shareGlucose : shareGlucoses) {
                            shareGlucose.processShareData(mContext);
                        }
                    return true;
                    }
                    return false;
                } catch (Exception e) {
                    Log.d("REST CALL ERROR: ", "getShareBg");
                    return false;
                }
            }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "doInBackGround"); }
            return false;
        }
    }

    public class DataSender extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mSessionId;
        Bg mBg;
        ShareUploadableBg uBg;
        DataSender(Context context, String sessionId, Bg bg) {
            mContext = context;
            mSessionId = sessionId;
            mBg = bg;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                String receiverSn = preferences.getString("share_key", "SM00000000").toUpperCase();
                dexcomShareSendBgInterface().uploadBGRecords(querySessionMap(mSessionId), new ShareUploadPayload(receiverSn, uBg), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d("ShareRest", "Success!! Uploaded!!");
                    }

                });
            }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
            return false;
        }
    }
    public int requestCount() {
        Bg bg = Bg.last();
        if(bg != null) {
            return 20;
        } else if (bg.datetime < new Date().getTime()) {
            return Math.min((int) Math.ceil(((new Date().getTime() - bg.datetime) / (5 * 1000 * 60))), 10);
        } else {
            return 1;
        }
    }

    public int minutesCount() {
        Bg bg = Bg.last();
        if(bg != null && bg.datetime < new Date().getTime()) {
            return Math.min((int) Math.ceil(((new Date().getTime() - bg.datetime) / (1000 * 60))), 1440);
        } else {
            return 1440;
        }
    }

    public Map<String, String> querySessionMap(String sessionId) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        return map;

    }
}
