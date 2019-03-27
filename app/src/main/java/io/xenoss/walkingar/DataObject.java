package io.xenoss.walkingar;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import static io.xenoss.walkingar.Config.*;

/**
 * Created by Tourdyiev Roman on 3/23/19.
 */
public class DataObject {

    private Context context;
    private List<Location> locations = new ArrayList<>();

    public DataObject(Context context) {
        this.context = context;

//        locations.add(createDataObject(50.920916, 34.834163, "EpicentrK", "70%", R.drawable.epicentrk));
//        locations.add(createDataObject(50.904100, 34.804956, "Manufactura", "70%", R.drawable.mnfktr));
//        locations.add(createDataObject(50.902592, 34.814539, "Glamour", "70%", R.drawable.glamour));
//        locations.add(createDataObject(50.882803, 34.777157, "Church of the Holy Martyr Valentina", "", R.drawable.church));
//        locations.add(createDataObject(50.881798, 34.777790, "СНАУ", "", R.drawable.snau));
//        locations.add(createDataObject(50.916217, 34.825997, "B-Tone", "", R.drawable.btone));
//        locations.add(createDataObject(50.915243, 34.808009, "Kazka Fortress", "", R.drawable.park_castle));


        locations.add(createDataObject(50.448175, 30.425661, "BMW Бавария Киев", "80%", R.drawable.bmw));
        locations.add(createDataObject(50.447644, 30.422396, "Workshop фитнесс клуб", "10%", R.drawable.work));
        locations.add(createDataObject(50.449009, 30.424052, "ТОВ «Гладіус Сервіс»", "10%", R.drawable.gladius));
        locations.add(createDataObject(50.451570, 30.421165, "Top-Shop", "15%", R.drawable.topshop));
    }

    private Location createDataObject(double lat, double lon, String title, String discount, int icon) {
        Location location = new Location("");
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        bundle.putString(DISCOUNT, discount);
        bundle.putInt(ICON, icon);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setExtras(bundle);
        return location;
    }

    public List<Location> getDataObjects() {
        return locations;
    }


}
