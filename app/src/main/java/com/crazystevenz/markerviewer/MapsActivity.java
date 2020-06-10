package com.crazystevenz.markerviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.textfield.TextInputEditText;
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

    String[] COLORS = new String[] {"Red", "Green", "Blue", "Yellow", "Magenta"};
    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<MyMarker> mMyMarkers = new ArrayList<>();
    private View mOverlayView;
    private TextInputEditText mDescriptionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
        showOverlay(marker);
        return false;
    }

    private void setupOverlay() {
        mOverlayView = findViewById(R.id.overlay);
        mDescriptionTextView = findViewById(R.id.descriptionTextInputEditText);

        // Set the X in the overlay to hide it when clicked
        ImageButton closeImageButton = mOverlayView.findViewById(R.id.close_image_button);
        closeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });
    }

    private void showOverlay(final Marker marker) {
        // Update the overlay's info
        TextView titleTextView = findViewById(R.id.titleTextView);
        titleTextView.setText(marker.getTitle());
        mDescriptionTextView.setText(marker.getSnippet());

        mOverlayView.setVisibility(View.VISIBLE);

        // Wait for the overlay to update
        mOverlayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                // Move the map up so the overlay doesn't hide the selected marker
                mMap.setPadding(0, 0, 0, findViewById(R.id.overlay).getHeight());

                // Source: https://stackoverflow.com/questions/13932441/android-google-maps-v2-set-zoom-level-for-mylocation
                // Move the camera to the user's location and zoom in
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15.0f));
            }
        });
    }

    private void hideOverlay() {
        mOverlayView.setVisibility(View.GONE);
        // Reset the map offset
        mMap.setPadding(0, 0, 0, 0);
    }

    private void addMarker() {

    }

    private void listenToDb() {
        final Query docRef = db.collection("cities").limit(5);
        docRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    System.out.println("Listen failed.");
                    return;
                }

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Map<String, Object> a = dc.getDocument().getData();
//                            mMyMarkers.get
//                            Log.d(TAG, "New city: " + );
                            break;
                        case MODIFIED:
//                            Log.d(TAG, "Modified city: " + dc.getDocument().getData());
                            break;
                        case REMOVED:
//                            Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                            break;
                    }
                }
            }
        });

    }
}