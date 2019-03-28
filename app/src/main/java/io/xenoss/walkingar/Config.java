package io.xenoss.walkingar;

import android.location.Location;

import java.util.concurrent.TimeUnit;

/**
 * Created by Tourdyiev Roman on 3/23/19.
 */
public class Config {
    public final static String TAG = "walkingAR";
    public final static float LOCATION_THRESHOLD = 50f;
    public final static int LOCATION_RADIUS = 3000;
    public final static int LOCATION_RADIUS_CHANGES = 50;
    public final static long GPS_FREQUENCY = TimeUnit.SECONDS.toMillis(1);
    public final static int GPS_MIN_DISTANCE = 1;
    public final static int MAX_DISTANCE = 150;
    public final static int MIN_DISTANCE = 20;
    public final static int UI_UPDATE_PERIOD = 100;
    public final static long LOCATION_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(5);
    public final static long LOCATION_UPDATE_FASTEST_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    public final static String TITLE = "title";
    public final static String DISCOUNT = "discount";
    public final static String ICON = "icon";
    public static final float ALPHA = 0.25f;
}
