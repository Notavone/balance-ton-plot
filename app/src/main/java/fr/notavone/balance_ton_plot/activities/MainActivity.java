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
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.location.LocationListenerCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.android.clustering.ClusterManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.BuildConfig;
import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.Plot;
import fr.notavone.balance_ton_plot.entities.PlotClusterItem;
import fr.notavone.balance_ton_plot.utils.CustomClusterRenderer;
import fr.notavone.balance_ton_plot.utils.UiChangeListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListenerCompat {
    private static final int MAP_ZOOM = 17;

    private final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private final CollectionReference plotsCollection = FirebaseFirestore.getInstance().collection("plots");
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private FusedLocationProviderClient locationService;
    private GoogleMap map;

    private ClusterManager<PlotClusterItem> clusterManager;
    private Uri fileUri;
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (perms) -> {
        }).launch(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                }
        );

        this.locationService = LocationServices.getFusedLocationProviderClient(this);
        this.intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), handleCameraResult);

        View view = getWindow().getDecorView();
        view.setOnSystemUiVisibilityChangeListener(new UiChangeListener(view));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (auth.getCurrentUser() == null) {
            signInLauncher.launch(signInIntent);
        }
    }

    @SuppressLint("MissingPermission")
    private final ActivityResultCallback<ActivityResult> handleCameraResult = (result) -> {
        if (result.getResultCode() != Activity.RESULT_OK) return;
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        this.locationService.getLastLocation()
                .addOnSuccessListener(this, this::onLocationGatheringSuccess)
                .addOnFailureListener((exception) -> logger.warning(exception.getMessage()));
    };

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        this.clusterManager = new ClusterManager<>(this, googleMap);

        clusterManager.setRenderer(new CustomClusterRenderer<>(this, googleMap, clusterManager));
        clusterManager.setOnClusterItemClickListener(handleClusterItemClick());
        clusterManager.setOnClusterClickListener(handleClusterClick());
        clusterManager.setAnimation(true);

        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        map.setIndoorEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            map.setMyLocationEnabled(true);

            this.plotsCollection.get()
                    .addOnSuccessListener(addPlotsToMap)
                    .addOnFailureListener(e -> logger.warning(e.getMessage()));

            locationService.getLastLocation()
                    .addOnSuccessListener(this, this::moveCameraToSelf)
                    .addOnFailureListener(e -> logger.warning(e.getMessage()));
        }
    }

    private void moveCameraToSelf(Location location) {
        if (location != null && map.isMyLocationEnabled()) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MAP_ZOOM));
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private final OnSuccessListener<QuerySnapshot> addPlotsToMap = (queryDocumentSnapshots) -> {
        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
        Collection<PlotClusterItem> plots = new ArrayList<>(documents.size());
        for (DocumentSnapshot document : documents) {
            Plot plot = document.toObject(Plot.class);
            if (plot != null) plots.add(new PlotClusterItem(plot));
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        moveCameraToSelf(location);
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

        storage.child(fileUri.getLastPathSegment()).putBytes(byteArray)
                .addOnSuccessListener((taskSnapshot) -> {
                    if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null) {
                        logger.info("Image uploaded successfully " + taskSnapshot.getMetadata().getReference().getDownloadUrl());
                    }

                    Plot plot = new Plot(fileUri.getLastPathSegment(), location.getLatitude(), location.getLongitude(), auth.getCurrentUser().getUid());
                    plotsCollection.add(plot)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Image sauvegardée", Toast.LENGTH_SHORT).show();

                                if (this.clusterManager != null) {
                                    PlotClusterItem plotClusterItem = new PlotClusterItem(plot);
                                    if (clusterManager.addItem(plotClusterItem))
                                        clusterManager.cluster();
                                }
                            })
                            .addOnFailureListener(e -> logger.warning(e.getMessage()));
                })
                .addOnFailureListener((exception) -> logger.warning(exception.getMessage()));

    }

    public void handleAddImageClick(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file = null;
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            file = File.createTempFile("plot_", ".jpg", storageDir);
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }

        if (file != null) {
            this.fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            intentActivityResultLauncher.launch(intent);
        }
    }

    @SuppressLint("MissingPermission")
    public void handleMyPositionClick(View view) {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            locationService.getLastLocation()
                    .addOnSuccessListener(this, this::moveCameraToSelf)
                    .addOnFailureListener(e -> logger.warning(e.getMessage()));
        }
    }

    private final Intent signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.EmailBuilder().build()
            ))
            .setIsSmartLockEnabled(false)
            .build();

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            logger.info("User signed in");
        } else {
            logger.warning("User not signed in");
        }
    }
}