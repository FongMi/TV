package tv.cjump.jni;

import android.os.Build;

public class DeviceUtils {

    public static boolean isMiBox2Device() {
        String manufacturer = Build.MANUFACTURER;
        String productName = Build.PRODUCT;
        return manufacturer.equalsIgnoreCase("Xiaomi") && productName.equalsIgnoreCase("dredd");
    }

    public static boolean isMagicBoxDevice() {
        String manufacturer = Build.MANUFACTURER;
        String productName = Build.PRODUCT;
        return manufacturer.equalsIgnoreCase("MagicBox") && productName.equalsIgnoreCase("MagicBox");
    }

    public static boolean isProblemBoxDevice() {
        return isMiBox2Device() || isMagicBoxDevice();
    }
}
