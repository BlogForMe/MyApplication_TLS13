package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpSelfSignedDemo {



    public static void makeRequest(final Context context) {
        new Thread(() -> {
            try {
                // Load the CA certificate from the assets folder
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = context.getAssets().open("ca.crt");
                Certificate ca = cf.generateCertificate(caInput);
                caInput.close();

                // Create a KeyStore containing the trusted CA
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, "123456".toCharArray());
                keyStore.setCertificateEntry("ca", ca);

                // Create a TrustManager that trusts the CA in our KeyStore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                X509TrustManager trustManager = null;
                for (TrustManager tm : trustManagers) {
                    if (tm instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) tm;
                        break;
                    }
                }
                if (trustManager == null) {
                    throw new IllegalStateException("No X509TrustManager found");
                }

                // Create an SSLContext for TLSv1.3
                SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, new TrustManager[]{trustManager}, null);

                // Build the OkHttpClient with the custom SSL settings
                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                // Accept the specific IP address (replace with your actual server IP)
                                return hostname.equals("192.168.43.167");
                            }
                        })
                        .build();

                // Build and execute the HTTPS request (adjust URL, port, and path as needed)
                Request request = new Request.Builder()
                        .url("https://192.168.43.167:8443/hello")
                        .build();

                client.newCall(request).enqueue(new Callback() {

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("OkHttpDemo", "Request failed", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
                        String responseBody = response.body().string();
                        Log.d("OkHttpDemo", "Response: " + responseBody);
                    }
                });
            } catch (Exception e) {
                Log.e("OkHttpDemo", "Error setting up HTTPS connection", e);
            }
        }).start();
    }


    public static void makeRequestwithCA(final Context context) {
        new Thread(() -> {
            try {
                // Load the CA certificate from the assets folder
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = context.getAssets().open("ca.crt");
                Certificate ca = cf.generateCertificate(caInput);
                caInput.close();

                // Create a KeyStore containing the trusted CA
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, "123456".toCharArray());
                keyStore.setCertificateEntry("ca", ca);

                // Create a TrustManager that trusts the CA in our KeyStore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                X509TrustManager trustManager = null;
                for (TrustManager tm : trustManagers) {
                    if (tm instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) tm;
                        break;
                    }
                }
                if (trustManager == null) {
                    throw new IllegalStateException("No X509TrustManager found");
                }

                // Create an SSLContext for TLSv1.3
                SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, new TrustManager[]{trustManager}, null);

                // Build the OkHttpClient with the custom SSL settings
                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                // Accept the specific IP address (replace with your actual server IP)
                                return hostname.equals("192.168.43.167");
                            }
                        })
                        .build();

                // Build and execute the HTTPS request (adjust URL, port, and path as needed)
                Request request = new Request.Builder()
                        .url("https://192.168.43.167:8443/hello")
                        .build();

                client.newCall(request).enqueue(new Callback() {

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("OkHttpDemo", "Request failed", e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
                        String responseBody = response.body().string();
                        Log.d("OkHttpDemo", "Response: " + responseBody);
                    }
                });
            } catch (Exception e) {
                Log.e("OkHttpDemo", "Error setting up HTTPS connection", e);
            }
        }).start();
    }

}
