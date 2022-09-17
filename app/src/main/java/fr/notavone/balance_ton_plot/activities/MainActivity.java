package fr.notavone.balance_ton_plot.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.android.clustering.ClusterManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.Plot;
import fr.notavone.balance_ton_plot.entities.PlotClusterItem;
import fr.notavone.balance_ton_plot.utils.CustomClusterRenderer;
import fr.notavone.balance_ton_plot.utils.UiChangeListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    final int MAP_ZOOM = 17;

    private final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private final CollectionReference plotsCollection = FirebaseFirestore.getInstance().collection("plots");
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private FusedLocationProviderClient locationService;
    private SupportMapFragment mapFragment;
    private GoogleMap map;

    private ClusterManager<PlotClusterItem> clusterManager;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View view = getWindow().getDecorView();
        view.setOnSystemUiVisibilityChangeListener(new UiChangeListener(view));

        this.locationService = LocationServices.getFusedLocationProviderClient(this);
        this.mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            if (hasLocationPermissions()) {
                mapFragment.getMapAsync(this);
            } else {
                handleRequestLocation.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                });
            }
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        this.clusterManager = new ClusterManager<>(this, googleMap);

        clusterManager.setRenderer(new CustomClusterRenderer<>(this, googleMap, clusterManager));
        clusterManager.setOnClusterItemClickListener(handleClusterItemClick());
        clusterManager.setOnClusterClickListener(handleClusterClick());
        clusterManager.setAnimation(true);

        map.setOnCameraIdleListener(clusterManager);
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        map.setIndoorEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        map.setMyLocationEnabled(true);

        this.plotsCollection.get()
                .addOnSuccessListener(addPlotsToMap)
                .addOnFailureListener(e -> logger.warning(e.getMessage()));

        locationService.getLastLocation()
                .addOnSuccessListener(this, location -> moveCameraToSelf(location, false))
                .addOnFailureListener(e -> logger.warning(e.getMessage()));
    }

    private void moveCameraToSelf(Location location) {
        moveCameraToSelf(location, true);
    }

    private void moveCameraToSelf(Location location, boolean animate) {
        if (location != null && map.isMyLocationEnabled()) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            if (animate) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MAP_ZOOM));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MAP_ZOOM));
            }
        }
    }

    private final OnSuccessListener<QuerySnapshot> addPlotsToMap = (queryDocumentSnapshots) -> {
        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
        Collection<PlotClusterItem> plots = new ArrayList<>(documents.size());
        for (DocumentSnapshot document : documents) {
            Plot plot = document.toObject(Plot.class);
            if (plot != null) {
                plot.setId(document.getId());
                plots.add(new PlotClusterItem(plot));
            }
        }

        if (clusterManager.addItems(plots)) {
            clusterManager.cluster();
        }
    };

    private ClusterManager.OnClusterItemClickListener<PlotClusterItem> handleClusterItemClick() {
        return (item) -> {
            Intent intent = new Intent(this, PlotActivity.class);
            intent.putExtra("plot", item.getPlot());
            startActivity(intent);
            return true;
        };
    }

    private ClusterManager.OnClusterClickListener<PlotClusterItem> handleClusterClick() {
        return (cluster) -> {
            Intent intent = new Intent(this, ClusterViewActivity.class);
            ArrayList<Plot> plots = new ArrayList<>(cluster.getItems().size());
            for (PlotClusterItem item : cluster.getItems()) {
                plots.add(item.getPlot());
            }
            intent.putExtra("plots", plots);
            startActivity(intent);
            return true;
        };
    }

    private void onLocationGatheringSuccess(Location location) {
        if (location == null) return;
        if (auth.getCurrentUser() == null) return;

        byte[] byteArray = new byte[0];

        try {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), fileUri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
            byteArray = stream.toByteArray();
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }

        if (byteArray.length <= 0) return;

        storage.child(fileUri.getLastPathSegment()).putBytes(byteArray).addOnSuccessListener((taskSnapshot) -> {
            if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null) {
                logger.info("Image uploaded successfully " + taskSnapshot.getMetadata().getReference().getDownloadUrl());
            }

            Plot plot = new Plot(fileUri.getLastPathSegment(), new GeoPoint(location.getLatitude(), location.getLongitude()), auth.getCurrentUser().getUid());
            plotsCollection.add(plot).addOnSuccessListener(documentReference -> {
                plot.setId(documentReference.getId());
                Toast.makeText(this, "Image sauvegardée", Toast.LENGTH_SHORT).show();

                if (this.clusterManager != null) {
                    PlotClusterItem plotClusterItem = new PlotClusterItem(plot);
                    if (clusterManager.addItem(plotClusterItem)) clusterManager.cluster();
                }
            }).addOnFailureListener(e -> logger.warning(e.getMessage()));
        }).addOnFailureListener((exception) -> logger.warning(exception.getMessage()));

    }

    public void handleAddImageClick(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        handleCameraResult.launch(intent);
    }

    @SuppressLint("MissingPermission")
    public void handleMyPositionClick(View view) {
        if (hasLocationPermissions()) {
            locationService.getLastLocation().addOnSuccessListener(this, this::moveCameraToSelf).addOnFailureListener(e -> logger.warning(e.getMessage()));
        }
    }

    @SuppressLint("MissingPermission")
    private final ActivityResultLauncher<Intent> handleCameraResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK) return;

        Intent intent = result.getData();
        if (intent == null) return;

        Uri uri = intent.getData();
        if (uri == null) return;

        this.fileUri = uri;

        if (hasLocationPermissions()) {
            this.locationService.getLastLocation()
                    .addOnSuccessListener(this, this::onLocationGatheringSuccess)
                    .addOnFailureListener((exception) -> logger.warning(exception.getMessage()));
        }
    });

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private final ActivityResultLauncher<String[]> handleRequestLocation = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (hasLocationPermissions()) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Vous devez accepter les permissions pour utiliser l'application", Toast.LENGTH_LONG).show();
            finish();
        }
    });
}