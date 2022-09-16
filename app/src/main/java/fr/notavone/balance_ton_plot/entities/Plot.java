package fr.notavone.balance_ton_plot.entities;

import java.io.Serializable;
import java.util.Date;

public class Plot implements Serializable {
    private final String storePath;
    private final double latitude;
    private final double longitude;
    private final Date createdAt;
    private final String createdBy;

    public Plot() {
        this.storePath = null;
        this.latitude = 0;
        this.longitude = 0;
        this.createdAt = null;
        this.createdBy = null;
    }

    public Plot(
            String storePath,
            double latitude,
            double longitude,
            String userUID
    ) {
        this.storePath = storePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = new Date(System.currentTimeMillis());
        this.createdBy = userUID;
    }

    public String getStorePath() {
        return storePath;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
