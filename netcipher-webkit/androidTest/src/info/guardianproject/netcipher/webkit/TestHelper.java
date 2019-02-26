package info.guardianproject.netcipher.webkit;

import android.os.Build;

import java.util.Arrays;

public class TestHelper {

    /**
     * in ARM based emulators everything is pretty slow, so we need to increase timeouts to make
     * sure tests have a chance to finish successfully.
     */
    public static long timeoutScale() {
        if (isEmulator() && isARM()) {
            return 25L;
        } else {
            return 1L;
        }
    }

    public static boolean isARM() {
        String osArch = System.getProperty("os.arch", "").trim().toLowerCase();
        String roProductCpuAbi = System.getProperty("ro.product.cpu.abi", "").trim().toLowerCase();
        boolean ret = Arrays.asList(new String[]{"armv7l", "aarch64"}).contains(osArch) ||
                Arrays.asList(new String[]{"armeabi", "armeabi-v7a", "arm64-v8a"}).contains(roProductCpuAbi);
        return ret;
    }

    /**
     * <h3>values on avd emulator api 22 arm:</h3>
     * <pre>
     *     Build.FINGERPRINT: Android/sdk_phone_armv7/generic:5.1.1/LMY48X/3079158:userdebug/test-keys
     *     Build.MODEL: sdk_phone_armv7
     *     Build.MANUFACTURER: unknown
     *     Build.BRAND: Android
     *     Build.DEVICE: generic
     *     Build.PRODUCT: sdk_phone_armv7
     * </pre>
     * <h3>values on avd emulator api 24 arm64:</h3>
     * <pre>
     *     Build.FINGERPRINT: Android/sdk_google_phone_arm64/generic_arm64:7.0/NYC/5071208:userdebug/dev-keys
     *     Build.MODEL:Android SDK built for arm64
     *     Build.MANUFACTURER: unknown
     *     Build.BRAND: Android
     *     Build.DEVICE: generic_arm64
     * </pre>
     */
    public static boolean isEmulator() {
        boolean ret = Build.FINGERPRINT.startsWith("Android/sdk_phone_armv7/generic:5.1.1/")
                && Build.DEVICE.equals("generic")
                && Build.BRAND.equals("Android");
        ret = ret || (Build.BRAND.equals("Android")
                && Build.MODEL.startsWith("Android SDK built for arm")
                && Build.DEVICE.startsWith("generic_arm"));
        return ret;
    }

}
