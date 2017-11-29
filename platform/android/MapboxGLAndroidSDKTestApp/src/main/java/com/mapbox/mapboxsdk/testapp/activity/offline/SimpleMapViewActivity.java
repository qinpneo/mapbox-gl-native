package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.testapp.R;

/**
 * The most basic example of adding a map to an activity.
 */
public class SimpleMapViewActivity extends AppCompatActivity {

  private static final String TAG = "TEST-OFFLINE";

  public static final String BOUNDS_ARG = "BOUNDS";
  public static final String STYLE_ARG = "STYLE";

  private MapView mapView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String styleUrl = null;
    LatLng cameraPos = null;
    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      if (bundle.containsKey(STYLE_ARG)) {
        styleUrl = bundle.getString(STYLE_ARG);
      }

      if (bundle.containsKey(BOUNDS_ARG)) {
        cameraPos = ((LatLngBounds) bundle.getParcelable(BOUNDS_ARG)).getCenter();
      }
    }

    if (styleUrl == null) {
      styleUrl = Style.MAPBOX_STREETS;
    }

    if (cameraPos == null) {
      cameraPos = new LatLng(45.520486, -122.673541);
    }

    Log.d(TAG, " >>>> StyleURl: =" + styleUrl);
    Log.d(TAG, " >>>> Camera pos: =" + cameraPos);

    // configure inital map state
    MapboxMapOptions options = new MapboxMapOptions()
      .styleUrl(styleUrl)
      .camera(new CameraPosition.Builder()
        .target(cameraPos)
        .zoom(11)
        .build());

    // create map
    mapView = new MapView(this, options);
    mapView.setId(R.id.mapView);
    mapView.onCreate(savedInstanceState);

    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(MapboxMap mapboxMap) {

      }
    });

    // This contains the MapView in XML and needs to be called after the access token is configured.
    // Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
    //setContentView(R.layout.activity_offline_simple_map);
    setContentView(mapView);

  }



  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}