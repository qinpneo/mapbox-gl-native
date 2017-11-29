package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.testapp.R;
import com.mapbox.mapboxsdk.testapp.utils.OfflineUtils;

import java.util.ArrayList;
import java.util.List;

public class OfflineRegionsListActivity extends AppCompatActivity implements Region.OnStatusChangeListener {

  private static final String TAG = "TEST-OFFLINE";

  private static final String MAP_STYLE_URL = Style.MAPBOX_STREETS;
  private static final double MAP_MIN_ZOOM =  0;
  private static final double MAP_MAX_ZOOM =  15;
  private static final float MAP_PIXEL_DENSITY = 4;

//  Setup as in OfflineActivity test
//  private static final String MAP_STYLE_URL = "mapbox://styles/mapbox/light-v9";
//  private static final double MAP_MIN_ZOOM =  15;
//  private static final double MAP_MAX_ZOOM =  25;
//  private static final float MAP_PIXEL_DENSITY = 2.65f;

  private List<Region> regions = new ArrayList<>();
  private ListView regionsList;
  private OfflineRegionListAdapter regionsAdapter;

  private OfflineManager offlineManager;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_offline_region_list);

    // Set up a list to hold available offline regions
    regionsAdapter = new OfflineRegionListAdapter(this, 0);
    regionsList = (ListView)findViewById(R.id.areas);
    regionsList.setAdapter(regionsAdapter);
    regionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        Region region = regionsAdapter.getItem(i);

        if (region != null) {
          if (region.isComplete()) {
            Log.d(TAG, "Region +" +region.getName()+ " clicked - download is complete -> show the region");

            Bundle bundle = new Bundle();
            bundle.putParcelable(SimpleMapViewActivity.BOUNDS_ARG, region.getBounds());
            bundle.putString(SimpleMapViewActivity.STYLE_ARG, MAP_STYLE_URL);

            Intent intent = new Intent(OfflineRegionsListActivity.this, SimpleMapViewActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);

          } else if (region.isDownloadStarted()) {
            Log.d(TAG, "Region +" +region.getName()+ " clicked - download is in progress");

          } else {

            Log.d(TAG, "Region +" +region.getName()+ " clicked - start download");
            region.startDownload();
          }
        }
      }
    });

    // Set up the offlineManager
    offlineManager = OfflineManager.getInstance(this);

    Log.d(TAG, ">>>>>>>>>>  onCreate: listOfflineRegions");
    // Download available regions from the OfflineManager
    // If there are no regions available -> add default ones (Berlin and Hessen)
    offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
      @Override
      public void onList(OfflineRegion[] regions) {

        if (regions == null || regions.length == 0) {
          requestAddDefaultRegions();

        } else {

          // Offline Regions that were included in one of the MultiRegions
          List<OfflineRegion> added = new ArrayList<>();

          for (int i = 0; i < regions.length; i++) {
            OfflineRegion region = regions[i];
            if (added.contains(region)) {
              continue;
            }

            // Find all regions with the same name after this one
            // Add them all together as MultiRegion
            // If there is only one OfflineRegion with this name - add as SingleRegion
            String name = OfflineUtils.convertRegionName(region.getMetadata());
            if (name != null) {
              List<Region> sameNameRegions = new ArrayList<>();
              for (int j = i + 1; j < regions.length; j++) {
                if (name.equals(OfflineUtils.convertRegionName(regions[j].getMetadata()))) {
                  if (sameNameRegions.size() == 0) {
                    sameNameRegions.add(new SingleRegion(region));
                  }
                  sameNameRegions.add(new SingleRegion(regions[j]));
                  added.add(regions[j]);
                }
              }
              if (sameNameRegions.size() > 0) {
                addToList(new MultiRegion(sameNameRegions));
                sameNameRegions.clear();
                continue;
              }
            }
            addToList(new SingleRegion(region));
          }
        }
      }

      @Override
      public void onError(String error) {
        Log.d(TAG, " Error getting regions error=" +error);

      }
    });
  }


  @Override
  protected void onDestroy() {

    Log.d(TAG, ">>>>>>>>>>  onDestroy");

    //  TODO: Should we clean offlineManager listAll callback?


    if (regions != null) {
      for (Region region : regions) {
        region.stopTrackingStatus();
        region.stopDownload();
      }
      regions.clear();
      regions = null;
    }
    if (regionsAdapter != null) {
      regionsAdapter.clear();
      regionsAdapter = null;
    }
    if (regionsList != null) {
      regionsList.setOnItemClickListener(null);
      regionsList.setAdapter(null);
      regionsList = null;
    }
    super.onDestroy();
  }

  @Override
  public void onStatusChanged(Region region) {
    onOfflineRegionStatusChanged(region, region.isComplete(),region.isDownloadStarted(),
      region.getCompletedResourceCount(), region.getRequiredResourceCount(), region.getCompletedResourceSize());
  }

  /**
   * Add a region to regions list and to the list adapter.
   * Start observing offline region's status.
   * When status changes list needs to be updated.
   * Given that getting status is an async operation, we will store last reported status in the SingleRegion instance
   */
  private void addToList(Region newRegion) {

    regions.add(newRegion);
    regionsAdapter.add(newRegion);
    newRegion.startTrackingStatus(this);

  }

  /**
   * Request SingleRegion with given name and bounds to be added to Offline Map Regions
   * using default style, minZoom, maxZoom and pixelDensity.
   * @param regionName
   * @param bounds
   */
  private void requestOfflineRegionAdd(String regionName, LatLngBounds bounds) {
    requestOfflineRegionAdd(regionName, MAP_STYLE_URL, bounds, MAP_MIN_ZOOM, MAP_MAX_ZOOM, MAP_PIXEL_DENSITY);
  }


  /**
   * Request SingleRegion with given name, bounds, default style, minZoom, maxZoom and pixelDensity to be added
   * to Offline Map Regions.
   * @param regionName
   * @param styleUrl
   * @param bounds
   * @param minZoom
   * @param maxZoom
   * @param pixelDensity
   */
  private void requestOfflineRegionAdd(final String regionName, String styleUrl,
                                       LatLngBounds bounds, double minZoom , double maxZoom, float pixelDensity) {

    Log.d(TAG, "Request offlineRegion regionName=" +regionName);

    OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
      styleUrl, bounds, minZoom, maxZoom, pixelDensity);


    // Create the offline region and launch the download
    offlineManager.createOfflineRegion(definition, OfflineUtils.convertRegionName(regionName),
      new OfflineManager.CreateOfflineRegionCallback() {
        @Override
        public void onCreate(OfflineRegion offlineRegion) {
          Log.d(TAG, "Offline region created: " + regionName +
            " offlineRegion: id=" + offlineRegion.getID() + " defenition.bounds=" +offlineRegion.getDefinition().getBounds()+
            " nameFromMetadata=" + OfflineUtils.convertRegionName(offlineRegion.getMetadata()));
          addToList(new SingleRegion(regionName, offlineRegion));
        }

        @Override
        public void onError(String error) {
          Log.e(">>>>", "Error: " + error);
        }
      });
  }

  private void requestOfflineMultiRegionAdd(String regionName, List<LatLngBounds> boundsList) {
    requestOfflineMultiRegionAdd(regionName, MAP_STYLE_URL, boundsList,
      MAP_MIN_ZOOM, MAP_MAX_ZOOM, MAP_PIXEL_DENSITY);
  }

  /**
   * Request SingleRegion with given name, bounds, default style, minZoom, maxZoom and pixelDensity to be added
   * to Offline Map Regions.
   * @param regionName
   * @param styleUrl
   * @param boundsList
   * @param minZoom
   * @param maxZoom
   * @param pixelDensity
   */
  private void requestOfflineMultiRegionAdd(final String regionName, String styleUrl,
                                            final List<LatLngBounds> boundsList,
                                            double minZoom , double maxZoom, float pixelDensity) {

    if (boundsList == null) {
      return;
    }

    if (styleUrl == null) {
      styleUrl = Style.MAPBOX_STREETS;
    }

    final List<Region> offlineRegionList = new ArrayList<>();
    for(LatLngBounds bounds : boundsList) {

      OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
        styleUrl, bounds, minZoom, maxZoom, pixelDensity);


      // Create the offline region and launch the download
      offlineManager.createOfflineRegion(definition, OfflineUtils.convertRegionName(regionName),
        new OfflineManager.CreateOfflineRegionCallback() {
          @Override
          public void onCreate(OfflineRegion offlineRegion) {

            offlineRegionList.add(new SingleRegion(regionName, offlineRegion));

            // Create and add a new Region that consists of muliple OfflineRegions
            // once all bounds were added to
            if (boundsList.size() == offlineRegionList.size()) {
              addToList(new MultiRegion(regionName, offlineRegionList));
            }
          }

          @Override
          public void onError(String error) {
            Log.e(">>>>", "Error: " + error);
          }
        });
    }
  }

  private void onOfflineRegionStatusChanged(Region region, boolean isComplete, boolean isBeingDownloaded,
                                            long completedCount, long allCount, long completedSize) {

    Log.d(TAG, String.format("REGION STATUS CHANGED: Name=%s  %s - %s/%s resources; %s bytes downloaded.",
      region.getName(), isComplete ? " COMPLETE " : (isBeingDownloaded ? " ACTIVE " : " AVAILABLE"),
      String.valueOf(completedCount),
      String.valueOf(allCount),
      String.valueOf(completedSize)));

    if (regionsAdapter != null) {
      regionsAdapter.notifyDataSetChanged();
    }
  }


  static List<LatLngBounds> hessenRegions = new ArrayList<>();
  static {
    hessenRegions.add(LatLngBounds.from(51.3983488624, 9.0362548828, 51.0923105489, 8.5528564453));
    hessenRegions.add(LatLngBounds.from(51.52000000000001, 9.10499999999999, 51.399, 8.882000000000005));
    hessenRegions.add(LatLngBounds.from(51.399, 9.10499999999999, 51.16900000000001, 9.037000000000006));
    hessenRegions.add(LatLngBounds.from(51.16900000000001, 9.424000000000007, 51.119, 9.102000000000004));
    hessenRegions.add(LatLngBounds.from(51.52199999999999, 9.424000000000007, 51.52099999999999, 9.102000000000004));
    hessenRegions.add(LatLngBounds.from(51.52099999999999, 9.424000000000007, 51.16999999999999, 9.105999999999995));
    hessenRegions.add(LatLngBounds.from(51.65899999999999, 9.695999999999998, 51.522999999999996, 9.264999999999986));
    hessenRegions.add(LatLngBounds.from(51.522999999999996, 9.695999999999998, 51.22399999999999, 9.425000000000011));
    hessenRegions.add(LatLngBounds.from(51.22399999999999, 10.086000000000013, 51.17500000000001, 9.686000000000007));
    hessenRegions.add(LatLngBounds.from(51.42500000000001, 10.086000000000013, 51.224999999999994, 9.697000000000003));
    hessenRegions.add(LatLngBounds.from(51.22399999999999, 9.686000000000007, 50.95400000000001, 9.626000000000005));
    hessenRegions.add(LatLngBounds.from(51.17500000000001, 10.241000000000014, 50.95400000000001, 9.687000000000012));
    hessenRegions.add(LatLngBounds.from(51.260999999999996, 10.241000000000014, 51.17599999999999, 10.086999999999989));
    hessenRegions.add(LatLngBounds.from(51.010999999999996, 9.626000000000005, 50.67400000000001, 9.454000000000008));
    hessenRegions.add(LatLngBounds.from(50.95400000000001, 10.068999999999988, 50.67400000000001, 9.62700000000001));
    hessenRegions.add(LatLngBounds.from(50.67500000000001, 9.454000000000008, 50.39699999999999, 9.449999999999989));
    hessenRegions.add(LatLngBounds.from(50.67400000000001, 10.086000000000013, 50.39699999999999, 9.455000000000013));
    hessenRegions.add(LatLngBounds.from(51.09200000000001, 9.449999999999989, 50.21199999999999, 8.939999999999998));
    hessenRegions.add(LatLngBounds.from(51.16900000000001, 9.102000000000004, 51.09299999999999, 9.037000000000006));
    hessenRegions.add(LatLngBounds.from(51.119, 9.626000000000005, 51.09299999999999, 9.103000000000009));
    hessenRegions.add(LatLngBounds.from(51.22399999999999, 9.626000000000005, 51.120000000000005, 9.425000000000011));
    hessenRegions.add(LatLngBounds.from(50.39699999999999, 9.795999999999992, 50.21199999999999, 9.450999999999993));
    hessenRegions.add(LatLngBounds.from(51.09299999999999, 9.454000000000008, 50.67599999999999, 9.450999999999993));
    hessenRegions.add(LatLngBounds.from(51.09299999999999, 9.626000000000005, 51.012, 9.455000000000013));
    hessenRegions.add(LatLngBounds.from(50.345, 8.939999999999998, 50.078, 8.925999999999988));
    hessenRegions.add(LatLngBounds.from(50.21199999999999, 9.682999999999993, 50.078, 8.941000000000003));
    hessenRegions.add(LatLngBounds.from(51.20099999999999, 8.551999999999992, 50.837999999999994, 8.437999999999988));
    hessenRegions.add(LatLngBounds.from(51.09200000000001, 8.939999999999998, 50.837999999999994, 8.552999999999997));
    hessenRegions.add(LatLngBounds.from(50.88900000000001, 8.437999999999988, 50.531000000000006, 8.116000000000014));
    hessenRegions.add(LatLngBounds.from(50.837999999999994, 8.685000000000002, 50.531000000000006, 8.438999999999993));
    hessenRegions.add(LatLngBounds.from(50.56299999999999, 8.116000000000014, 50.202, 7.961000000000013));
    hessenRegions.add(LatLngBounds.from(50.531000000000006, 8.530000000000001, 50.202, 8.11699999999999));
    hessenRegions.add(LatLngBounds.from(50.27600000000001, 7.961000000000013, 49.91300000000001, 7.765999999999991));
    hessenRegions.add(LatLngBounds.from(50.202, 8.544999999999987, 49.91300000000001, 7.961999999999989));
    hessenRegions.add(LatLngBounds.from(50.27600000000001, 8.544999999999987, 50.203, 8.531000000000006));
    hessenRegions.add(LatLngBounds.from(49.91300000000001, 9.151999999999987, 49.52199999999999, 8.305000000000007));
    hessenRegions.add(LatLngBounds.from(50.94900000000001, 8.437999999999988, 50.889999999999986, 8.305000000000007));
    hessenRegions.add(LatLngBounds.from(50.531000000000006, 8.925999999999988, 50.27699999999999, 8.531000000000006));
    hessenRegions.add(LatLngBounds.from(50.27699999999999, 8.925999999999988, 49.91399999999999, 8.545999999999992));
    hessenRegions.add(LatLngBounds.from(50.837999999999994, 8.939999999999998, 50.53200000000001, 8.686000000000007));
    hessenRegions.add(LatLngBounds.from(50.078, 9.151999999999987, 49.91399999999999, 8.926999999999992));
    hessenRegions.add(LatLngBounds.from(50.53200000000001, 8.939999999999998, 50.346000000000004, 8.926999999999992));
    hessenRegions.add(LatLngBounds.from(49.52199999999999, 8.806000000000012, 49.497000000000014, 8.346000000000004));
    hessenRegions.add(LatLngBounds.from(49.497000000000014, 9.140999999999991, 49.387, 8.661000000000001));
    hessenRegions.add(LatLngBounds.from(49.52199999999999, 9.140999999999991, 49.49799999999999, 8.806999999999988));

  }

  private void requestAddDefaultRegions() {

    Log.d(TAG, " Add defaults:");
    LatLngBounds firstBounds = hessenRegions.get(0);
    LatLngBounds hessenBounds = LatLngBounds.from(firstBounds.getLatNorth(), firstBounds.getLonEast(),
      firstBounds.getLatSouth(), firstBounds.getLonWest());


    for(int i = 1; i < hessenRegions.size(); i++) {
      hessenBounds.union(hessenRegions.get(i));
    }

    LatLngBounds berlinBounds = new LatLngBounds.Builder()
      .include(new LatLng(52.6780473464, 13.7603759766))
      .include(new LatLng(52.3305137868, 13.0627441406))
      .build();

    LatLngBounds houstonBounds = new LatLngBounds.Builder()
      .include(new LatLng(29.724658720686932, -95.3383383161871))
      .include(new LatLng(29.7153330657912, -95.34565979479373))
      .build();

    Log.d(TAG, " Bounds berlin: " +berlinBounds);
    Log.d(TAG, " Bounds Houston: " +houstonBounds);
    Log.d(TAG, " Bounds Hennsen: " +hessenBounds);

    // float pixelRatio = this.getResources().getDisplayMetrics().density;
    requestOfflineRegionAdd("Hessen", hessenBounds);
    requestOfflineRegionAdd("Berlin", berlinBounds);
    requestOfflineRegionAdd("Houston", houstonBounds);
    requestOfflineMultiRegionAdd("Multi Hessen", hessenRegions);

  };

}
