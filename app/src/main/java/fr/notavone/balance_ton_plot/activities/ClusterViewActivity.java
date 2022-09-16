package fr.notavone.balance_ton_plot.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.adapters.PlotsGridAdapter;
import fr.notavone.balance_ton_plot.entities.Plot;
import fr.notavone.balance_ton_plot.utils.UiChangeListener;

public class ClusterViewActivity extends AppCompatActivity {
    private final Logger logger = Logger.getLogger(PlotActivity.class.getName());
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_view);

        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (perms) -> {
        }).launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
        });

        View view = getWindow().getDecorView();
        view.setOnSystemUiVisibilityChangeListener(new UiChangeListener(view));

        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<Plot> plots = (ArrayList<Plot>) intent.getSerializableExtra("plots");
            if (plots != null) {
                RecyclerView plotsGrid = findViewById(R.id.clusterView);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
                PlotsGridAdapter adapter = new PlotsGridAdapter(plots, storage);

                plotsGrid.setLayoutManager(gridLayoutManager);
                plotsGrid.setAdapter(adapter);
            }
        }
    }

    public void finish() {
        super.finish();
    }
}