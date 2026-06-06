package com.example.localisation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 101;
    
    // UI components
    private TextView latitudeDisplay, longitudeDisplay;
    private Button showMapButton;

    // Networking and Location
    private RequestQueue volleyQueue;
    private LocationManager gpsManager;

    // Configuration - Change this to your server's IP
    private static final String ENDPOINT_CREATE = "http://192.168.43.228/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
        initLocationServices();
    }

    private void initUi() {
        latitudeDisplay = findViewById(R.id.display_lat);
        longitudeDisplay = findViewById(R.id.display_lon);
        showMapButton = findViewById(R.id.btn_open_map);

        volleyQueue = Volley.newRequestQueue(this);
        gpsManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        showMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }

    private void initLocationServices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            subscribeToGpsUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void subscribeToGpsUpdates() {
        // Request updates every 60s or if moved 150m
        gpsManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateDisplay(location);
            sendPositionToServer(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String statusLabel;
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE: statusLabel = "Hors service"; break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE: statusLabel = "Temporairement indisponible"; break;
                case LocationProvider.AVAILABLE: statusLabel = "Disponible"; break;
                default: statusLabel = "Inconnu";
            }
            showToast(getString(R.string.status_changed, provider, statusLabel));
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            showToast(getString(R.string.status_enabled, provider));
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            showToast(getString(R.string.status_disabled, provider));
        }
    };

    private void updateDisplay(Location loc) {
        latitudeDisplay.setText("Latitude: " + loc.getLatitude());
        longitudeDisplay.setText("Longitude: " + loc.getLongitude());

        String detailedMsg = String.format(Locale.getDefault(), 
                getString(R.string.location_update),
                loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getAccuracy());
        
        showToast(detailedMsg);
    }

    private void sendPositionToServer(final double lat, final double lon) {
        StringRequest postRequest = new StringRequest(Request.Method.POST, ENDPOINT_CREATE,
                response -> {
                    // Success handling if needed
                },
                error -> showToast(getString(R.string.network_error, error.getMessage()))
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                data.put("latitude", String.valueOf(lat));
                data.put("longitude", String.valueOf(lon));
                data.put("date", dateFormat.format(new Date()));
                data.put("imei", fetchUniqueId());

                return data;
            }
        };

        volleyQueue.add(postRequest);
    }

    private String fetchUniqueId() {
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.isEmpty()) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    if (manager != null) id = manager.getDeviceId();
                }
            } catch (Exception e) {
                id = "DEFAULT_ID";
            }
        }
        return (id != null) ? id : "UNKNOWN";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                subscribeToGpsUpdates();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }
}
