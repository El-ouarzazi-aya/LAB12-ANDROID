package com.example.localisation;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private TextView tvMarkerCount;
    private final String showUrl = "http://10.0.2.2/localisation/showPositions.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tvMarkerCount = findViewById(R.id.tvMarkerCount);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Style sombre pour la carte
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception e) {
            // Style par défaut si fichier absent
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        loadPositions();
    }

    private void loadPositions() {
        tvMarkerCount.setText("Chargement...");

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, showUrl, null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");
                        int count = positions.length();
                        tvMarkerCount.setText(count + " position" + (count > 1 ? "s" : "") + " enregistrée" + (count > 1 ? "s" : ""));

                        if (count == 0) return;

                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                        for (int i = 0; i < count; i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            double lat = pos.getDouble("latitude");
                            double lon = pos.getDouble("longitude");
                            String date = pos.optString("date", "");
                            String imei = pos.optString("imei", "");

                            LatLng point = new LatLng(lat, lon);

                            // Dernier marqueur en cyan, les autres en bleu
                            float hue = (i == count - 1)
                                    ? BitmapDescriptorFactory.HUE_CYAN
                                    : BitmapDescriptorFactory.HUE_AZURE;

                            mMap.addMarker(new MarkerOptions()
                                    .position(point)
                                    .title(i == count - 1 ? "📍 Dernière position" : "Position " + (i + 1))
                                    .snippet(String.format(Locale.getDefault(),
                                            "%.5f, %.5f\n%s", lat, lon, date))
                                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));

                            boundsBuilder.include(point);
                        }

                        // Zoom automatique sur tous les marqueurs
                        LatLngBounds bounds = boundsBuilder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));

                    } catch (JSONException e) {
                        tvMarkerCount.setText("Erreur de chargement");
                    }
                },
                error -> tvMarkerCount.setText("Erreur réseau")
        );
        requestQueue.add(req);
    }
}