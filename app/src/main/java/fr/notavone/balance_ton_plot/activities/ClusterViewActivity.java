package fr.notavone.balance_ton_plot.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.adapters.PlotsGridAdapter;
import fr.notavone.balance_ton_plot.entities.Plot;
import fr.notavone.balance_ton_plot.utils.UiChangeListener;

public class ClusterViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_view);

        View view = getWindow().getDecorView();
        view.setOnSystemUiVisibilityChangeListener(new UiChangeListener(view));

        FloatingActionButton fab = findViewById(R.id.clusterViewBackButton);
        fab.setOnClickListener(this::retour);

        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<Plot> plots = intent.getParcelableArrayListExtra("plots");

            if (plots != null) {
                RecyclerView plotsGrid = findViewById(R.id.clusterView);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
                PlotsGridAdapter adapter = new PlotsGridAdapter(plots);

                plotsGrid.setLayoutManager(gridLayoutManager);
                plotsGrid.setAdapter(adapter);
            }
        }
    }

    public void retour(View view) {
        finish();
    }
}