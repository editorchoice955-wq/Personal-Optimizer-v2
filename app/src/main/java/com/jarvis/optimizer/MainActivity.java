package com.jarvis.optimizer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView web;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        web.setWebViewClient(new WebViewClient());

        web.addJavascriptInterface(new NativeBridge(), "Android");

        web.loadUrl("file:///android_asset/index.html");

        setContentView(web);

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    public class NativeBridge {

        @JavascriptInterface
        public String getSystemStats() {

            try {

                ActivityManager am =
                        (ActivityManager) getSystemService(ACTIVITY_SERVICE);

                ActivityManager.MemoryInfo memoryInfo =
                        new ActivityManager.MemoryInfo();

                am.getMemoryInfo(memoryInfo);

                long totalRam = memoryInfo.totalMem;
                long freeRam = memoryInfo.availMem;
                long usedRam = totalRam - freeRam;

                android.os.StatFs statFs =
                        new android.os.StatFs(
                                Environment.getDataDirectory().getPath());

                long totalStorage = statFs.getTotalBytes();
                long freeStorage = statFs.getAvailableBytes();
                long usedStorage = totalStorage - freeStorage;

                Intent battery =
                        registerReceiver(
                                null,
                                new IntentFilter(
                                        Intent.ACTION_BATTERY_CHANGED));

                int level = battery != null
                        ? battery.getIntExtra("level", -1)
                        : -1;

                int scale = battery != null
                        ? battery.getIntExtra("scale", 100)
                        : 100;

                int temperature10 = battery != null
                        ? battery.getIntExtra("temperature", -1)
                        : -1;

                float temperature = temperature10 / 10.0f;

                ConnectivityManager cm =
                        (ConnectivityManager)
                                getSystemService(CONNECTIVITY_SERVICE);

                Network network = cm.getActiveNetwork();

                NetworkCapabilities capabilities =
                        network != null
                                ? cm.getNetworkCapabilities(network)
                                : null;

                String networkType = "OFFLINE";

                if (capabilities != null) {

                    if (capabilities.hasTransport(
                            NetworkCapabilities.TRANSPORT_WIFI)) {

                        networkType = "WIFI";

                    } else if (capabilities.hasTransport(
                            NetworkCapabilities.TRANSPORT_CELLULAR)) {

                        networkType = "MOBILE";

                    } else if (capabilities.hasTransport(
                            NetworkCapabilities.TRANSPORT_ETHERNET)) {

                        networkType = "ETHERNET";

                    } else if (capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET)) {

                        networkType = "ONLINE";
                    }
                }

                JSONObject result = new JSONObject();

                result.put("ramTotal", totalRam);
                result.put("ramUsed", usedRam);
                result.put("ramFree", freeRam);

                result.put("storageTotal", totalStorage);
                result.put("storageUsed", usedStorage);
                result.put("storageFree", freeStorage);

                result.put(
                        "battery",
                        level >= 0
                                ? (level * 100.0 / Math.max(1, scale))
                                : -1);

                result.put("temp", temperature);
                result.put("network", networkType);

                return result.toString();

            } catch (Exception e) {

                return "{\"error\":\"native error\"}";
            }
        }

        @JavascriptInterface
        public void openStorageSettings() {

            try {

                startActivity(
                        new Intent(
                                Settings.ACTION_INTERNAL_STORAGE_SETTINGS));

            } catch (Exception e) {

                startActivity(
                        new Intent(Settings.ACTION_SETTINGS));
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
