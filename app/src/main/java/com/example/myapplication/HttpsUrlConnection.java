package com.example.myapplication;

import android.content.Context;
import android.content.res.Resources;
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
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class HttpsUrlConnection {
    static String TAG = "HttpsUrlConnection";

    public static void reqeustConnection(Context context, String url) {
        try {

            SSLSocketFactory sslSocketFactory = socketFactory(context).getSocketFactory();

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

    public static void printSubjectAlternativeNames(Context context) {
        try {
            // Load certificate from res/raw/server.crt
            Resources res = context.getResources();
            InputStream is = res.openRawResource(R.raw.ca); // "server" is the filename (without .crt extension)

            // Generate X509Certificate instance
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certificate = cf.generateCertificate(is);
            is.close();

            X509Certificate x509Cert = (X509Certificate) certificate;

            // Retrieve SAN (Subject Alternative Names)
            Collection<List<?>> sanCollection = x509Cert.getSubjectAlternativeNames();

            if (sanCollection == null) {
                Log.d("SAN", "No Subject Alternative Names found.");
                return;
            }

            // Iterate over SAN entries and print
            for (List<?> sanItem : sanCollection) {
                Integer sanType = (Integer) sanItem.get(0);
                Object sanValue = sanItem.get(1);

                String sanTypeName = "";
                switch (sanType) {
                    case 0:
                        sanTypeName = "Other Name";
                        break;
                    case 1:
                        sanTypeName = "RFC822 Name (Email)";
                        break;
                    case 2:
                        sanTypeName = "DNS Name";
                        break;
                    case 7:
                        sanTypeName = "IP Address";
                        break;
                    default:
                        sanTypeName = "Type (" + sanType + ")";
                }

                Log.d("SAN", sanTypeName + ": " + sanValue);
            }
        } catch (Exception e) {
            Log.e("SAN", "Error retrieving SAN: ", e);
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

            printSubjectAlternativeNames(context);

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
