package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class HttpsUrlConnection {
    static String TAG = "HttpsUrlConnection";

    public static void reqeustConnection(Context context, String url) {
        try {

//            SSLSocketFactory sslSocketFactory = socketFactory(context).getSocketFactory();

            // Create a URL object for the target server
            URL mUrl = new URL(url);

            Log.i(TAG, "reqeustConnection: " + url);
            // Open a connection to the URL
            HttpsURLConnection connection = (HttpsURLConnection) mUrl.openConnection();

            // Set the enabled protocols to include TLS 1.3
//            connection.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});

            // Set the request method
            connection.setRequestMethod("GET");
//            connection.setSSLSocketFactory(sslSocketFactory);

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine + "\n");
            }
            in.close();

            // Print the response content
            Log.i("onward10HttpUr", "onward10HttpUrlConnection: " + content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SSLContext socketFactory(Context context) {
        try {
            // Load the certificate from the assets folder
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(R.raw.ca);; // or use
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CA
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CA in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext;
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
