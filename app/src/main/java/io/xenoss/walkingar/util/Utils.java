package io.xenoss.walkingar.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.google.android.libraries.maps.model.BitmapDescriptor;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;

import java.util.Locale;

import static io.xenoss.walkingar.Config.*;

/**
 * Created by Tourdyiev Roman on 3/22/19.
 */
public class Utils {

	public static String readableDistance(float distance) {
		String distanceFormatted = "";
		//metric
		if (distance >= 1000) {
			int km = (int) (distance / 1000);
			int m = (int) distance - km * 1000;
			distanceFormatted = km + "km " + m + "m";
		} else {
			distanceFormatted = (int) Math.ceil(distance) + "m";
		}
		return distanceFormatted;
	}

	public static String format(float[] values) {
		//PP LocalUS to conserve decimal dot
		return String.format(Locale.US,"Z%1$.1f\t\tX%2$.1f\t\tY%3$.1f", values[0], values[1], values[2]);
	}

	public static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	public static BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
		Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
		vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
		Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		vectorDrawable.draw(canvas);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	public static float[] lowPass(float[] input, float[] output) {
		if (output == null) return input;

		for (int i = 0; i < input.length; i++) {
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
		}
		return output;
	}
}
