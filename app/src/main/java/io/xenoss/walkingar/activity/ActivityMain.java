package io.xenoss.walkingar.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.xenoss.walkingar.DataObject;
import io.xenoss.walkingar.R;
import io.xenoss.walkingar.util.Utils;

import static android.view.Gravity.CENTER;
import static io.xenoss.walkingar.Config.*;

@SuppressWarnings("MissingPermission")
public class ActivityMain extends AppCompatActivity
        implements SensorEventListener,
        LocationListener,
        GpsStatus.Listener {

    private SensorManager sensorManager;
    private Sensor sensorAccel;
    private Sensor sensorMagnet;
    private Timer timer;
    //    private FusedLocationProviderClient fusedLocationClient;
    private LocationManager locationManager;
    //    private LocationRequest locationRequest;
//    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private StringBuilder sb = new StringBuilder();
    private OrientationEventListener orientationEventListener;
    private SurfaceHolder previewHolder;
    private Camera camera;
    protected GoogleMap mMap;
    private LatLngBounds.Builder builder;

    private TextView debugInfo;
    private TextView pois;
    private FrameLayout ARViewsContainer;
    private SurfaceView cameraPreview;
    private MapView gmapView;

    private boolean calibrated = false;
    private boolean canRotate = true;
    private boolean inPreview = false;
    private float[] valuesAccel = new float[3];
    private float[] valuesMagnet = new float[3];
    private float[] currentOrientation = new float[3];
    private float[] r = new float[9];
    private float[] inR = new float[9];
    private float[] outR = new float[9];
    private float angleWithNorth;
    private float zoom = 15;
    private float scale;
    private int orientation = -1;
    private DataObject dataObject;
    private List<Location> ARObjects = new ArrayList<>();


    /*----------------------------------------------------------------------*/
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                valuesAccel = Utils.lowPass(event.values.clone(), valuesAccel);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                valuesMagnet = Utils.lowPass(event.values.clone(), valuesMagnet);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*----------------------------------------------------------------------*/
    @Override
    public void onLocationChanged(Location location) {
        if (currentLocation.distanceTo(location) < LOCATION_RADIUS_CHANGES)
            initARObjects();
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    //    private LocationCallback mLocationCallback = new LocationCallback() {
//        @Override
//        public void onLocationResult(LocationResult locationResult) {
//            if (currentLocation != null)
//                if (currentLocation.distanceTo(locationResult.getLastLocation()) >= LOCATION_THRESHOLD) {
//                    currentLocation = locationResult.getLastLocation();
//                }
//        }
//    };
    /*----------------------------------------------------------------------*/
    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            try {
//                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(previewHolder);
            } catch (Throwable t) {
                Log.e(TAG, "Exception in setPreviewDisplay()", t);
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = Utils.getBestPreviewSize(width, height, parameters);

            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                camera.setParameters(parameters);
                camera.startPreview();
                inPreview = true;
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // not used
        }

    };
    /*----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        findViews();
        initViews();
        init();
        initARObjects();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkAndRequestPermissions()) {
//            if (fusedLocationClient != null) {
//                fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
//            }
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAltitudeRequired(true);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setBearingRequired(true);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
            locationManager.addGpsStatusListener(this);
            locationManager.requestLocationUpdates(GPS_FREQUENCY, GPS_MIN_DISTANCE, criteria, this, null);
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            previewHolder = cameraPreview.getHolder();
            previewHolder.addCallback(surfaceCallback);
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            camera = Camera.open();

            initMap();
        }

        sensorManager.registerListener(this, sensorAccel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorMagnet, SensorManager.SENSOR_DELAY_NORMAL);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getDeviceOrientation();
                        showDebugInfo();
                        if (currentLocation != null)
                            updateARObjects();
                    }
                });
            }
        };
        timer.schedule(task, 0, UI_UPDATE_PERIOD);

        orientationEventListener.enable();

        gmapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (orientationEventListener != null) orientationEventListener.disable();
        timer.cancel();
        if (camera != null) {
            if (inPreview) {
                camera.stopPreview();
            }
            camera.release();
            camera = null;
        }
        inPreview = false;
        locationManager.removeUpdates(this);
        gmapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gmapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        gmapView.onLowMemory();
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        if (hasNoPermissions()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA

            }, 0);
            return false;
        }

        return true;
    }

    private boolean hasNoPermissions() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.permissions));
                builder.setMessage(getResources().getString(R.string.permissions_messages));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        checkAndRequestPermissions();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return;
            }
        }

    }

    private void findViews() {
        debugInfo = findViewById(R.id.debug_info);
        ARViewsContainer = findViewById(R.id.ar_views_container);
        cameraPreview = findViewById(R.id.camera_preview);
        gmapView = findViewById(R.id.google_map_view);
        pois = findViewById(R.id.pois);
    }

    private void initViews() {

    }

    private void init() {

        scale = getResources().getDisplayMetrics().density;
        dataObject = new DataObject(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorMagnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//
//        googleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
//                    @Override
//                    public void onConnected(@Nullable Bundle bundle) {
//                        currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
//                        setLocationRequest();
//                    }
//
//                    @Override
//                    public void onConnectionSuspended(int i) {
//
//                    }
//                })
//                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
//                    @Override
//                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//                        setLocationRequest();
//                    }
//                })
//                .addApi(LocationServices.API)
//                .build();
//        googleApiClient.connect();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                Log.d("onOrientationChanged", orientation + "");
                ActivityMain.this.orientation = orientation;
                gmapView.setVisibility(orientation == -1 ? View.VISIBLE : View.GONE);
            }
        };

        gmapView.onCreate(null);

        pois.setText("POIS: " + ARObjects.size());
    }

    private void initMap() {
        gmapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressWarnings("MissingPermission")
            @Override
            public void onMapReady(GoogleMap map) {
                builder = new LatLngBounds.Builder();
                mMap = map;
                map.clear();
                map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View v = getLayoutInflater().inflate(R.layout.layout_snippet, null);
                        TextView title = v.findViewById(R.id.title);
                        TextView discount = v.findViewById(R.id.discount);
                        TextView distance = v.findViewById(R.id.distance);
                        ImageView icon = v.findViewById(R.id.icon);
                        title.setText(marker.getTitle());
                        Location location = (Location) marker.getTag();
                        String discountString = location.getExtras().getString(DISCOUNT);
                        discount.setText(discountString.length() > 0 ? discountString + "\ndiscount" : "");
                        if (currentLocation != null) {
                            distance.setText(Utils.readableDistance(currentLocation.distanceTo(location)));
                        }
                        icon.setImageResource(location.getExtras().getInt(ICON));
                        return v;
                    }
                });
                map.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
                    @Override
                    public void onInfoWindowClose(Marker marker) {
                        canRotate = true;
                    }
                });
                map.setMyLocationEnabled(true);
                map.setBuildingsEnabled(true);
                map.setPadding(0, getResources().getDimensionPixelSize(R.dimen.xsmall_marker_padding), getResources().getDimensionPixelSize(R.dimen.small_marker_padding), 0);

                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setCompassEnabled(true);

                for (int i = 0; i < ARObjects.size(); i++) {
                    Location location = ARObjects.get(i);
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    builder.include(latLng);
                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));
                    marker.setIcon(Utils.bitmapDescriptorFromVector(ActivityMain.this, R.drawable.ic_location_on_primary_36dp));
//                    marker.setIcon(BitmapDescriptorFactory.fromResource(location.getExtras().getInt(ICON)));
                    marker.setTitle(location.getExtras().getString(TITLE));
                    marker.setSnippet(location.getExtras().getString(DISCOUNT));
                    marker.setTag(location);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                        builder.build(),
                        getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels,
                        getResources().getDimensionPixelSize(R.dimen.double_action_bar_height)));
//
                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        canRotate = false;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), zoom));
                        marker.showInfoWindow();
                        return true;
                    }
                });

                map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        zoom = mMap.getCameraPosition().zoom;
                    }
                });

                map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        canRotate = true;
                        return true;
                    }
                });

                map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                    @Override
                    public void onCameraMoveStarted(int i) {
                        switch (i) {
                            case (REASON_GESTURE):
                                Log.d("onCameraListener", "REASON_GESTURE");
                                canRotate = false;
                            case (REASON_API_ANIMATION):
                                Log.d("onCameraListener", "REASON_API_ANIMATION");
//                                canRotate = true;
                                break;
                            case (REASON_DEVELOPER_ANIMATION):
                                Log.d("onCameraListener", "REASON_DEVELOPER_ANIMATION");
                                break;
                        }
                    }
                });

            }
        });
    }

    private void initARObjects() {
        ARViewsContainer.removeAllViews();

        ARObjects.clear();
        for (Location location : dataObject.getDataObjects()) {
            if (currentLocation.distanceTo(location) <= LOCATION_RADIUS)
                ARObjects.add(location);
        }

        for (Location dataObject : ARObjects) {
            View view = getLayoutInflater().inflate(R.layout.layout_data_object, null);

            TextView title = view.findViewById(R.id.title);
            TextView markerTitle = view.findViewById(R.id.marker_title);
            TextView discount = view.findViewById(R.id.discount);
            TextView distance = view.findViewById(R.id.distance);
            TextView markerDistance = view.findViewById(R.id.marker_distance);
            ImageView icon = view.findViewById(R.id.icon);

            Bundle bundle = dataObject.getExtras();

            title.setText(bundle.getString(TITLE));
            markerTitle.setText(bundle.getString(TITLE));
            String discountString = bundle.getString(DISCOUNT);
            discount.setText(discountString.length() > 0 ? discountString + "\ndiscount" : "");
            if (currentLocation != null) {
                distance.setText(Utils.readableDistance(currentLocation.distanceTo(dataObject)));
                markerDistance.setText(Utils.readableDistance(currentLocation.distanceTo(dataObject)));
            }
            icon.setImageResource(bundle.getInt(ICON));

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = CENTER;
            view.setLayoutParams(layoutParams);

            view.setScaleX(0.1f);
            view.setScaleY(0.1f);

            ARViewsContainer.addView(view);
        }
    }

    private void updateARObjects() {

        float distanceFactor = 1900;
        float angleX = -currentOrientation[2] - 90;
        float angleZ = -orientation - 90;

        for (int i = 0; i < ARObjects.size(); i++) {

            View view = ARViewsContainer.getChildAt(i);

            Location location = ARObjects.get(i);
            float distance = currentLocation.distanceTo(location);
            float bearing = (currentLocation.bearingTo(location) + 360) % 360;
            float angleY = angleWithNorth - bearing;
            int Y = (int) (distanceFactor * Math.tan(Math.toRadians(angleX)));
            int X = -(int) (distanceFactor * Math.tan(Math.toRadians(angleY)));
            float scaleFactor = 1f;
            if (distance < MAX_DISTANCE) {
                scaleFactor = (float) MIN_DISTANCE / distance;
            } else {
                scaleFactor = 0.15f;
            }

            view.findViewById(R.id.marker_icon).setVisibility(distance > MAX_DISTANCE ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.marker_card).setVisibility(distance > MAX_DISTANCE ? View.GONE : View.VISIBLE);


            view.setCameraDistance(distanceFactor * scale);
            view.setRotation(angleZ);
            view.setRotationX(angleX);
            view.setRotationY(angleY);
            view.setVisibility(Math.abs(angleY) > 90 ? View.GONE : View.VISIBLE);
            view.setTranslationX(X);
            view.setTranslationY(Y);
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            ((TextView) view.findViewById(R.id.distance)).setText(Utils.readableDistance(distance));
            ((TextView) view.findViewById(R.id.marker_distance)).setText(Utils.readableDistance(distance));
        }
    }

    private void showDebugInfo() {
        sb.setLength(0);
        sb.append("Orientation : " + Utils.format(currentOrientation));
        sb.append("\n");
        if (currentLocation != null)
            sb.append("Location : " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude()
                    + "; accuracy: " + currentLocation.getAccuracy());
        sb.append("\n");
        sb.append("angleWithNorth : " + angleWithNorth);
        debugInfo.setText(sb);
    }

    private void getDeviceOrientation() {
        SensorManager.getRotationMatrix(r, null, valuesAccel, valuesMagnet);
        SensorManager.getOrientation(r, currentOrientation);
        currentOrientation[0] = (float) Math.toDegrees(currentOrientation[0]);
        currentOrientation[1] = (float) Math.toDegrees(currentOrientation[1]);
        currentOrientation[2] = (float) Math.toDegrees(currentOrientation[2]);
        angleWithNorth = (currentOrientation[0] + 360 + 90) % 360;
        updateCamera(angleWithNorth);
    }

    public void updateCamera(float bearing) {
        if (canRotate) {
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                    .bearing(bearing)
                    .zoom(zoom)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        }
    }
}