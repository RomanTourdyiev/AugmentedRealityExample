package io.xenoss.walkingar.migrateCamera;

public interface CameraSupport {
	CameraSupport open(int cameraId);

	int getOrientation(int cameraId);
}
