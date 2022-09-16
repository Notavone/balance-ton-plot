package fr.notavone.balance_ton_plot.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.Plot;

public class PlotsGridAdapter extends RecyclerView.Adapter<PlotsGridAdapter.ViewHolder> {
    private final Logger logger = Logger.getLogger(PlotsGridAdapter.class.getName());
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final StorageReference storage = FirebaseStorage.getInstance().getReference().child("plots");
    private final CollectionReference collection = FirebaseFirestore.getInstance().collection("plots");
    private final ArrayList<Plot> plots;

    public PlotsGridAdapter(ArrayList<Plot> plots) {
        this.plots = plots;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plot_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Plot plot = plots.get(position);
        File file = null;

        try {
            file = File.createTempFile(plot.getStorePath(), "", holder.itemView.getContext().getCacheDir());
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }

        if (file == null) return;
        String path = file.getAbsolutePath();
        storage.child(plot.getStorePath()).getFile(file)
                .addOnSuccessListener(taskSnapshot -> {
                    InputStream stream = null;
                    try {
                        stream = new FileInputStream(path);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    holder.plotImage.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> logger.severe(e.getMessage()));

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String date = dateFormat.format(plot.getCreatedAt());
        holder.plotCreatedAt.setText(date);

        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Object admin = task.getResult().getClaims().get("admin");
                    if (admin != null && admin.equals(true)) {
                        holder.plotDelete.setVisibility(View.VISIBLE);
                        holder.plotDelete.setOnClickListener(v -> deletePlot(v, position));
                    }
                }
            });
        }
    }

    private void deletePlot(View view, int position) {
        Plot plot = plots.get(position);
        logger.info("Deleting plot " + plot.getId());
        collection.document(plot.getId()).delete()
                .addOnSuccessListener(aVoid1 -> {
                    Toast.makeText(view.getContext(), "Plot supprimé !", Toast.LENGTH_SHORT).show();
                    logger.info("Plot deleted");
                    view.findViewById(R.id.plotDelete).setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> logger.severe(e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return plots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView plotImage;
        private final TextView plotCreatedAt;
        private final FloatingActionButton plotDelete;

        public ViewHolder(View view) {
            super(view);

            plotImage = view.findViewById(R.id.plotImage);
            plotCreatedAt = view.findViewById(R.id.plotCreatedAt);
            plotDelete = view.findViewById(R.id.plotDelete);
        }
    }
}
