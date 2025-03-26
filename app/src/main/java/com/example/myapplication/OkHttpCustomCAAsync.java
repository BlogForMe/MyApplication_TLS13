package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpCustomCAAsync {


    // Create an OkHttpClient that trusts only the custom CA certificate.
    public static OkHttpClient getClient(Context context) {
        try {
            // Load the custom CA certificate from res/raw/my_ca.crt
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getAssets().open("ca.crt");
//            InputStream caInput = context.getAssets().open("tls13.1d.pw");
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CA
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("my_ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            // Get the X509TrustManager from the factory
            X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

            // Build and return the OkHttpClient with the custom SSL settings
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create custom SSL client", e);
        }
    }

    // Makes an asynchronous request using OkHttp's enqueue() method.
    public static void makeAsyncRequest(Context context) {
        OkHttpClient client = getClient(context);
        Request request = new Request.Builder()
                .url("https://tls13.1d.pw/")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Check if the error is an SSLHandshakeException (CA verification failure)
                if (e instanceof SSLHandshakeException) {
                    Log.e("OkHttpCustomCAAsync", "SSL handshake failed: " + e.getMessage());
                    // Additional handling for certificate errors can be added here.
                } else {
                    Log.e("OkHttpCustomCAAsync", "Request failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Log.e("OkHttpCustomCAAsync", "Unexpected response code: " + response);
                        return;
                    }
                    // Process the response as needed.
                    String responseData = response.body().string();
                    Log.d("OkHttpCustomCAAsync", "Response: " + responseData);
                } catch (Exception e) {
                    Log.e("OkHttpCustomCAAsync", "Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

}
