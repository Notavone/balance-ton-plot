package fr.notavone.balance_ton_plot.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private final CollectionReference plotsCollection = FirebaseFirestore.getInstance().collection("plots");
    private final CollectionReference usersCollection = FirebaseFirestore.getInstance().collection("users");
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private Plot plot;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        FloatingActionButton deleteButton = findViewById(R.id.plotViewDeleteButton);
        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Object admin = task.getResult().getClaims().get("admin");
                    if (admin != null && admin.equals(true)) {
                        deleteButton.setVisibility(View.VISIBLE);
                        deleteButton.setOnClickListener(this::deletePlot);
                    }
                }
            });
        }

        deleteButton.setOnClickListener(this::deletePlot);

        FloatingActionButton fab = findViewById(R.id.plotViewBackButton);
        fab.setOnClickListener(this::retour);

        Intent intent = getIntent();
        plot = intent.getParcelableExtra("plot");

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

        usersCollection.document(plot.getCreatedBy()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    if (username != null) {
                        textView.setText(username + " : " + textView.getText());
                    }
                })
                .addOnFailureListener(e -> logger.severe(e.getMessage()));

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

    private void deletePlot(View view) {
        plotsCollection.document(plot.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Plot supprimé !", Toast.LENGTH_SHORT).show();
                    logger.info("Plot deleted");
                    retour(view);
                })
                .addOnFailureListener(e -> logger.warning(e.getMessage()));
    }

    public void retour(View view) {
        finish();
    }
}