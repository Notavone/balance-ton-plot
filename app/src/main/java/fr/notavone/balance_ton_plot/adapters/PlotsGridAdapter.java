package fr.notavone.balance_ton_plot.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.Plot;

public class PlotsGridAdapter extends RecyclerView.Adapter<PlotsGridAdapter.ViewHolder> {
    private final Logger logger = Logger.getLogger(PlotsGridAdapter.class.getName());
    private final StorageReference storage;
    private final ArrayList<Plot> plots;

    public PlotsGridAdapter(ArrayList<Plot> plots, StorageReference storage) {
        this.plots = plots;
        this.storage = storage;
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
    }

    @Override
    public int getItemCount() {
        return plots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView plotImage;

        public ViewHolder(View view) {
            super(view);

            plotImage = view.findViewById(R.id.plotImage);
        }
    }
}
