package fr.notavone.balance_ton_plot.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class PlotClusterItem implements ClusterItem {
    private final Plot plot;

    public PlotClusterItem(Plot plot) {
        this.plot = plot;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return new LatLng(plot.getLocation().getLatitude(), plot.getLocation().getLongitude());
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }

    public Plot getPlot() {
        return this.plot;
    }
}
