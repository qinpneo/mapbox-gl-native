package com.mapbox.mapboxsdk.testapp.activity.offline;


import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLngBounds;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiRegion implements Region, Region.OnStatusChangeListener {

  private static final String TAG = "TEST-OFFLINE";

  private final String name;
  private final List<Region> regions;
  private LatLngBounds bounds = null;
  private OnStatusChangeListener listener;

  public MultiRegion(List<Region> regionsList) {
    if (regionsList == null && regionsList.size() > 0) {
      name = "";
      regions = new ArrayList<>();
    } else {
      this.name = (regionsList.get(0).getName());
      regions = new ArrayList<>(regionsList.size());
      regions.addAll(regionsList);
    }
  }

  public MultiRegion(String name, List<Region> regionsList) {
    this.name = name;
    regions = new ArrayList<>(regionsList == null ? 0 : regionsList.size());
    regions.addAll(regionsList);
  }


  @Override
  public String getName() {
    return name;
  }

  @Override
  public void startDownload() {
    for (Region region : regions) {
      region.startDownload();
    }
  }

  @Override
  public void stopDownload() {
    for (Region region : regions) {
      region.stopDownload();
    }
  }

  @Override
  public void onStatusChanged(Region region) {
    MultiRegion.this.listener.onStatusChanged(MultiRegion.this);
  }

  @Override
  public void startTrackingStatus(OnStatusChangeListener listener) {
    this.listener = listener;
    for (Region region : regions) {
      region.startTrackingStatus(this);
    }
  }

  @Override
  public void stopTrackingStatus() {
    this.listener = null;
    for (Region region : regions) {
      region.stopTrackingStatus();
    }
  }

  public boolean isComplete() {
    for (Region region : regions) {
      if (!region.isComplete()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isDownloadStarted() {
    for (Region region : regions) {
      if (region.isDownloadStarted()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public long getRequiredResourceCount() {
    long resCount = 0;
    for (Region region : regions) {
      resCount += region.getRequiredResourceCount();
    }
    return resCount;
  }

  @Override
  public long getCompletedResourceCount() {
    long resCount = 0;
    for (Region region : regions) {
      resCount += region.getCompletedResourceCount();
    }
    return resCount;
  }

  @Override
  public long getCompletedResourceSize() {
    long resCount = 0;
    for (Region region : regions) {
      resCount += region.getCompletedResourceSize();
    }
    return resCount;
  }

  @Override
  public LatLngBounds getBounds() {
    if (bounds == null) {
      LatLngBounds bounds = regions.get(0).getBounds();
      bounds = LatLngBounds.from(bounds.getLatNorth(), bounds.getLonEast(),
        bounds.getLatSouth(), bounds.getLonWest());

      for(int i = 1; i < regions.size(); i++) {
        bounds.union(regions.get(i).getBounds());
      }
    }
    return bounds;
  }


}
