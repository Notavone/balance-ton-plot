package fr.notavone.balance_ton_plot.entities;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Plot implements Parcelable {
    private final String storePath;
    private final Date createdAt;
    private final String createdBy;
    private final GeoPoint location;
    private String id;

    public Plot() {
        this(null, null, null);
    }

    public Plot(
            String storePath,
            GeoPoint location,
            String userUID
    ) {
        this.storePath = storePath;
        this.createdAt = new Date(System.currentTimeMillis());
        this.createdBy = userUID;
        this.location = location;
        this.id = null;
    }

    public Plot(Parcel in) {
        this.location = new GeoPoint(in.readDouble(), in.readDouble());
        this.storePath = in.readString();
        this.createdAt = new Date(in.readLong());
        this.createdBy = in.readString();
        this.id = in.readString();
    }

    public static final Creator<Plot> CREATOR = new Creator<Plot>() {
        @Override
        public Plot createFromParcel(Parcel in) {
            return new Plot(in);
        }

        @Override
        public Plot[] newArray(int size) {
            return new Plot[size];
        }
    };

    public String getStorePath() {
        return storePath;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(location.getLatitude());
        dest.writeDouble(location.getLongitude());
        dest.writeString(storePath);
        dest.writeLong(createdAt.getTime());
        dest.writeString(createdBy);
        dest.writeString(id);
    }
}
