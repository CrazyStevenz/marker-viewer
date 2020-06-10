package com.crazystevenz.markerviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<MyMarker> mMyMarkers = new ArrayList<>();
    private View mOverlayView;
    private TextView mDescriptionTextView;
    private TextView mSensorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupOverlay();
        listenToDb();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        MyMarker myMarker = MyMarker.find(mMyMarkers, marker);
        if (myMarker != null) showOverlay(myMarker);
        return false;
    }

    private void setupOverlay() {
        mOverlayView = findViewById(R.id.overlay);
        mDescriptionTextView = findViewById(R.id.descriptionTextView);
        mSensorTextView = findViewById(R.id.sensorTextView);

        // Set the X in the overlay to hide it when clicked
        ImageButton closeImageButton = mOverlayView.findViewById(R.id.close_image_button);
        closeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });
    }

    private void showOverlay(final MyMarker myMarker) {
        // Update the overlay's info
        TextView titleTextView = findViewById(R.id.titleTextView);
        titleTextView.setText(myMarker.getMarker().getTitle());
        String description = myMarker.getMarker().getSnippet();
        if (description.equals("")) {
            mDescriptionTextView.setText("No description provided.");
        } else {
            mDescriptionTextView.setText(myMarker.getMarker().getSnippet());
        }
        mSensorTextView.setText("Temperature: " + myMarker.getSensorReading() + "°C");

        mOverlayView.setVisibility(View.VISIBLE);

        // Display the new marker's info
        myMarker.getMarker().showInfoWindow();

        // Wait for the overlay to update
        mOverlayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                // Move the map up so the overlay doesn't hide the selected marker
                mMap.setPadding(0, 0, 0, findViewById(R.id.overlay).getHeight());

                // Source: https://stackoverflow.com/questions/13932441/android-google-maps-v2-set-zoom-level-for-mylocation
                // Move the camera to the user's location and zoom in
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myMarker.getMarker().getPosition(), 15.0f));
            }
        });
    }

    private void hideOverlay() {
        mOverlayView.setVisibility(View.GONE);
        // Reset the map offset
        mMap.setPadding(0, 0, 0, 0);
    }

    private void listenToDb() {
        final Query docRef = db.collection("markers").limit(6);
        docRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(),
                            "Listen failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                try {
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Map<String, Object> data = dc.getDocument().getData();

                        switch (dc.getType()) {
                            case ADDED:

                                // Get position of marker
                                Map<String, Object> latLngMap =
                                        (Map<String, Object>) data.get("position");
                                LatLng latLng = new LatLng(
                                        Double.parseDouble(latLngMap.get("latitude").toString()),
                                        Double.parseDouble(latLngMap.get("longitude").toString())
                                );

                                // Set position, title and description of marker and add it to the map
                                MarkerOptions markerOptions = new MarkerOptions().position(latLng);
                                markerOptions.title(data.get("title").toString());
                                markerOptions.snippet(data.get("description").toString());
                                Marker newMarker = mMap.addMarker((markerOptions));

                                MyMarker myMarker = new MyMarker(newMarker);

                                // Get the rest of the data
                                myMarker.setColor(data.get("color").toString());

                                if (data.get("sensorReading") != null) {
                                    myMarker.setSensorReading(
                                            Float.parseFloat(data.get("sensorReading").toString())
                                    );
                                }

                                // Add the new marker to the marker list so we can access it later
                                mMyMarkers.add(myMarker);

                                setColor(myMarker.getMarker(), myMarker.getColor());

                                showOverlay(myMarker);
                                break;

                            case MODIFIED:

                                // Get position of marker
                                Map<String, Object> modifiedLatLngMap =
                                        (Map<String, Object>) data.get("position");
                                LatLng modifiedLatLng = new LatLng(
                                        Double.parseDouble(modifiedLatLngMap.get("latitude").toString()),
                                        Double.parseDouble(modifiedLatLngMap.get("longitude").toString())
                                );

                                MyMarker modifiedMyMarker = MyMarker.findByLatLng(mMyMarkers, modifiedLatLng);

                                if (modifiedMyMarker == null) {
                                    Toast.makeText(getApplicationContext(),
                                            "The modified marker wasn't found",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                } else {
                                    modifiedMyMarker.getMarker().setTitle(data.get("title").toString());
                                    modifiedMyMarker.getMarker().setSnippet(data.get("description").toString());
                                    modifiedMyMarker.setColor(data.get("color").toString());
                                    if (data.get("sensorReading") != null) {
                                        modifiedMyMarker.setSensorReading(
                                                Float.parseFloat(data.get("sensorReading").toString())
                                        );
                                    }

                                    setColor(modifiedMyMarker.getMarker(), modifiedMyMarker.getColor());

                                    showOverlay(modifiedMyMarker);
                                }
                                break;

                            case REMOVED:

                                // Get position of marker
                                Map<String, Object> removedLatLngMap =
                                        (Map<String, Object>) data.get("position");
                                LatLng removedLatLng = new LatLng(
                                        Double.parseDouble(removedLatLngMap.get("latitude").toString()),
                                        Double.parseDouble(removedLatLngMap.get("longitude").toString())
                                );

                                MyMarker removedMyMarker = MyMarker.findByLatLng(mMyMarkers, removedLatLng);

                                if (removedMyMarker == null) {
                                    Toast.makeText(getApplicationContext(),
                                            "The deleted marker wasn't found",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                } else {
                                    // Source: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
                                    // Delete the marker from the map
                                    removedMyMarker.getMarker().remove();
                                    // Remove it from the list
                                    mMyMarkers.remove(removedMyMarker);
                                }
                                break;
                        }
                    }
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(),
                            ex.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void setColor(Marker marker, String color) {
        // Source: https://stackoverflow.com/a/49189517/10334320
        switch (color) {
            case "Red": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case "Green": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            case "Blue": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                break;
            case "Yellow": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
            case "Magenta": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                break;
        }
    }
}