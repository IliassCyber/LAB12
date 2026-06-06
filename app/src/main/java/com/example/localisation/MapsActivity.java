package com.example.localisation;

import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap googleMapInstance;
    private RequestQueue networkQueue;
    
    // Server endpoint to fetch all stored positions
    private static final String ENDPOINT_SHOW = "http://192.168.43.228/localisation/showPositions.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        networkQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMapInstance = googleMap;
        loadPositionsFromServer();
    }

    private void loadPositionsFromServer() {
        JsonObjectRequest fetchRequest = new JsonObjectRequest(
                Request.Method.POST,
                ENDPOINT_SHOW,
                null,
                response -> {
                    try {
                        JSONArray positionsArray = response.getJSONArray("positions");
                        displayMarkers(positionsArray);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur de lecture des données", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        networkQueue.add(fetchRequest);
    }

    private void displayMarkers(JSONArray positions) throws JSONException {
        LatLng lastLocation = null;
        
        for (int i = 0; i < positions.length(); i++) {
            JSONObject posObj = positions.getJSONObject(i);
            double latitude = posObj.getDouble("latitude");
            double longitude = posObj.getDouble("longitude");
            String timestamp = posObj.optString("date", "Position " + i);

            LatLng point = new LatLng(latitude, longitude);
            googleMapInstance.addMarker(new MarkerOptions()
                    .position(point)
                    .title(timestamp));
            
            lastLocation = point;
        }

        // Move camera to the last known position if available
        if (lastLocation != null) {
            googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 12.0f));
        }
    }
}
