package itzgonza.hollyshit.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Native bridge for MinHook and ImGui integration.
 * This class will load the hollyshit_native.dll to perform low-level operations.
 */
public interface NativeLibrary extends Library {
    NativeLibrary INSTANCE = Native.load("hollyshit_native", NativeLibrary.class);

    // Placeholder for MinHook initialization
    void initializeHooks();

    // Placeholder for ImGui UI Display
    void showPremiumUI();

    // Helper to check if native DLL is available
    default boolean isAvailable() {
        try {
            return INSTANCE != null;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
