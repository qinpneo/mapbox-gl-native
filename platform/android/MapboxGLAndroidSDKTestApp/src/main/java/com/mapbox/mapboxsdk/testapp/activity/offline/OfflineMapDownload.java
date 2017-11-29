package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.testapp.R;
import com.mapbox.mapboxsdk.testapp.utils.OfflineUtils;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by osanababayan on 11/28/17.
 */

public class OfflineMapDownload extends AppCompatActivity
  implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {


  private String styleUrl = Style.MAPBOX_STREETS;

  private EditText regionNameView;
  private TextView minZoomView, maxZoomView;
  private SeekBar minZoomSeekBar, maxZoomSeekBar;
  private EditText latNorthView, latSouthView, lonEastView, lonWestView;
  private TextView downloadProgressView;
  private Button actionButton;

  private OfflineRegion offlineRegion = null;
  private OfflineRegionStatus status = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_offline_map_form);

    initUI();
  }

  @Override
  protected void onDestroy() {
    cleanUI();

    stopDownload();

    if (offlineRegion != null) {
      offlineRegion.setObserver(null);
      offlineRegion = null;
    }

    super.onDestroy();
  }

  private void initUI() {
    regionNameView = (EditText)findViewById(R.id.name);

    minZoomView = (TextView)findViewById(R.id.minzoom);
    maxZoomView = (TextView)findViewById(R.id.maxzoom);

    minZoomSeekBar = (SeekBar) findViewById(R.id.minzoom_slider);
    minZoomSeekBar.setOnSeekBarChangeListener(this);
    maxZoomSeekBar = (SeekBar) findViewById(R.id.maxzoom_slider);
    maxZoomSeekBar.setOnSeekBarChangeListener(this);

    latNorthView = (EditText)findViewById(R.id.lat_north);
    lonEastView = (EditText)findViewById(R.id.lon_east);

    latSouthView = (EditText)findViewById(R.id.lat_south);
    lonWestView = (EditText)findViewById(R.id.lon_west);

    downloadProgressView = (TextView)findViewById(R.id.download_progress);

    actionButton = (Button)findViewById(R.id.action_button);
    actionButton.setOnClickListener(this);

    // Set Default values;
    minZoomSeekBar.setProgress(0);
    maxZoomSeekBar.setProgress(15);

    // New York
    latNorthView.setText("40.7589372691904");
    lonEastView.setText("-73.96024123810196");
    latSouthView.setText("40.740763489055496");
    lonWestView.setText("-73.97569076188057");

    // Berlin
//    latNorthView.setText("52.6780473464");
//    lonEastView.setText("13.7603759766");
//    latSouthView.setText("52.3305137868");
//    lonWestView.setText("13.0627441406");
    // styleView.setText(style);
  }

  private void cleanUI() {
    actionButton.setOnClickListener(null);
    minZoomSeekBar.setOnSeekBarChangeListener(null);
    maxZoomSeekBar.setOnSeekBarChangeListener(null);
  }

  public void onClick(View button) {

    Timber.e(">>>>> complete=" +isDownloadComplete()+ " isDownloading=" +isDownloading());

    if (isDownloadComplete()) {
     // Display offline map in a new Activity

      showRegion(offlineRegion);

    } else if (isDownloading()) {
      actionButton.setText("Pause download");
      stopDownload();

    } else {

      actionButton.setText("Downloading...");
      // Create offline Region and
      // start download it once it is created
      createOfflineRegionAndStartDownload();
    }

  }


  public void createOfflineRegionAndStartDownload() {
    // get data from UI
    String regionName = regionNameView.getText().toString();
    double latitudeNorth = Double.parseDouble(latNorthView.getText().toString());
    double longitudeEast = Double.parseDouble(lonEastView.getText().toString());
    double latitudeSouth = Double.parseDouble(latSouthView.getText().toString());
    double longitudeWest = Double.parseDouble(lonWestView.getText().toString());

    float pixelDensity = getResources().getDisplayMetrics().density;

    // String styleUrl = (String) styleUrlView.getSelectedItem();
    float maxZoom = maxZoomSeekBar.getProgress();
    float minZoom = minZoomSeekBar.getProgress();

    // create offline definition from data
    OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
      styleUrl,
      new LatLngBounds.Builder()
        .include(new LatLng(latitudeNorth, longitudeEast))
        .include(new LatLng(latitudeSouth, longitudeWest))
        .build(),
      minZoom,
      maxZoom,
      pixelDensity
    );


    OfflineManager.getInstance(this.getApplicationContext())
      .createOfflineRegion(definition,
        OfflineUtils.convertRegionName(regionName),
        new OfflineManager.CreateOfflineRegionCallback() {
          @Override
          public void onCreate(OfflineRegion offlineRegion) {
            Timber.e(">>>> Region created >>> start Download");
            startDownLoad(offlineRegion);
          }

          @Override
          public void onError(String error) {
            Timber.e("Failed to create offline Region");
          }
        }
      );
  }


  private void startDownLoad(OfflineRegion offlineRegion) {
    if (offlineRegion != null && !isDownloading()) {
      Timber.e(">>>> Start Download offlineRegion=" +offlineRegion);
      this.offlineRegion = offlineRegion;

      // Get observing offline region's status.
      offlineRegion.getStatus(new OfflineRegion.OfflineRegionStatusCallback() {
        @Override
        public void onStatus(OfflineRegionStatus status) {
          onDownloadStatusChanged(status);
        }

        @Override
        public void onError(String error) {
          Timber.e("Failed to get status");
        }
      });

      //Start observing offline region's status
      offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
        @Override
        public void onStatusChanged(OfflineRegionStatus status) {

          // Stop downlaod !
          if (status.isComplete()) {

            stopMeasuringDownload();
            Toast.makeText(OfflineMapDownload.this,
              "Download is complete - turn off WiFi to Test",
              Toast.LENGTH_SHORT)
              .show();
            actionButton.setText("Show Offline Region");
          }

          onDownloadStatusChanged(status);
        }

        @Override
        public void onError(OfflineRegionError error) {
          Timber.e("Failed to report status " +error.getMessage());
        }

        @Override
        public void mapboxTileCountLimitExceeded(long limit) {

        }
      });

      startMeasureDownload();
      this.offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }
  }

  private void stopDownload() {
    if (isDownloading() && offlineRegion != null) {
      offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE);
      stopMeasuringDownload();
    }
  }

  private void showRegion(OfflineRegion region) {
    Bundle bundle = new Bundle();
    bundle.putParcelable(SimpleMapViewActivity.BOUNDS_ARG, region.getDefinition().getBounds());
    bundle.putString(SimpleMapViewActivity.STYLE_ARG, Style.MAPBOX_STREETS);

    Intent intent = new Intent(this, SimpleMapViewActivity.class);
    intent.putExtras(bundle);
    startActivity(intent);
  }

  private void onDownloadStatusChanged(OfflineRegionStatus status) {

    this.status = status;

    // Compute a percentage
    final int percentage = status.getRequiredResourceCount() >= 0 ?
      (int)(100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) : 0;
    final String progressStr = getSize(status.getCompletedResourceSize()) + ", " + percentage + " %";
      downloadProgressView.setText(progressStr);

    Timber.e(String.format("REGION STATUS CHANGED: %s - %s/%s resources; %s bytes downloaded.",
      status.isComplete() ? " COMPLETE " : (isDownloading() ? " DOWNLOADING " : " AVAILABLE"),
      String.valueOf(status.getCompletedResourceCount()),
      String.valueOf(status.getRequiredResourceCount()),
      String.valueOf(status.getCompletedResourceSize())));
  }

  private boolean isDownloading() {
    return status != null && status.getDownloadState() == OfflineRegion.STATE_ACTIVE;
  }

  private boolean isDownloadComplete() {
    return status != null && status.isComplete();
  }

  static String getSize(long size) {
    if (size == 0) {
      return "0 B";
    } else if (size < 1024) {
      return size + " B";
    } else if (size < 1048576){
      return size / 1024 + " KB";
    } else {
      return size /1048576 + " MB";
    }
  }

  //https://en.wikipedia.org/wiki/Haversine_formula
  static double latLongToMeters(LatLngBounds bounds) {
    double lat1 = bounds.getLatNorth();
    double lon1 = bounds.getLonEast();
    double lat2 = bounds.getLatSouth();
    double lon2 = bounds.getLonWest();

    double R = 6378.137; // Radius of earth in KM
    double dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
    double dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    double d = R * c;
    return d * 1000; // meters
  }

  private long time = 0;
  private void startMeasureDownload() {
    time = System.currentTimeMillis();
  }

  private void stopMeasuringDownload() {

    if (time > 0) {
      time = System.currentTimeMillis() - time;
    }

    LatLngBounds bounds = offlineRegion.getDefinition().getBounds();

    Timber.e(" >>>>> It took " + TimeUnit.MILLISECONDS.toMinutes(time) + " minutes to load " +
      getSize(status.getCompletedResourceSize()) + " the map of " +
      OfflineUtils.convertRegionName(offlineRegion.getMetadata()) +
      " with bounds span:" +bounds.getLatitudeSpan() +", "
      +bounds.getLongitudeSpan());
    time = 0;
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    if (seekBar == minZoomSeekBar) {
      minZoomView.setText(String.valueOf(i));
    } else {
      maxZoomView.setText(String.valueOf(i));
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {

  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {

  }
}
