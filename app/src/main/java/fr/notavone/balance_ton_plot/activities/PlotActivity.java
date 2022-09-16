package fr.notavone.balance_ton_plot.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.Plot;
import fr.notavone.balance_ton_plot.utils.UiChangeListener;

public class PlotActivity extends AppCompatActivity {
    private final Logger logger = Logger.getLogger(PlotActivity.class.getName());
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        Intent intent = getIntent();
        Plot plot = (Plot) intent.getSerializableExtra("plot");

        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (perms) -> {
        }).launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
        });

        View view = getWindow().getDecorView();
        view.setOnSystemUiVisibilityChangeListener(new UiChangeListener(view));

        if (plot == null) {
            finish();
            return;
        }


        File file = null;
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String date = dateFormat.format(plot.getCreatedAt());

        TextView textView = findViewById(R.id.plotCreatedAt);
        textView.setText(date);

        try {
            file = File.createTempFile(plot.getStorePath(), "", getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file == null) return;
        String path = file.getAbsolutePath();

        storage.child(plot.getStorePath()).getFile(file)
                .addOnSuccessListener(taskSnapshot -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> logger.warning(e.getMessage()));
    }

    public void finish(View view) {
        super.finish();
    }
}