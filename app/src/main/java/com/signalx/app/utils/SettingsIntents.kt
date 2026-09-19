package com.signalx.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Handles launching system settings and the hidden "Phone info" (RadioInfo) screen.
 * Walks an OEM fallback chain so that devices from Google, Samsung, Xiaomi, OnePlus,
 * Motorola, etc. can open the appropriate radio/network configuration screen.
 */
object SettingsIntents {

    enum class Target(val actions: List<String>) {
        RADIO_INFO(emptyList()),
        PREFERRED_NETWORK(listOf("android.settings.NETWORK_OPERATOR_SETTINGS", Settings.ACTION_DATA_ROAMING_SETTINGS)),
        MOBILE_NETWORK(listOf(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)),
        SIM(listOf("android.settings.SIM_PREFERENCE_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS)),
        DATA_USAGE(listOf(Settings.ACTION_DATA_USAGE_SETTINGS)),
        ROAMING(listOf(Settings.ACTION_DATA_ROAMING_SETTINGS)),
        APN(listOf(Settings.ACTION_APN_SETTINGS)),
        OPERATORS(listOf(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)),
        APP_DETAILS(listOf(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
    }

    /**
     * Opens the hidden "Phone info" (RadioInfo) testing screen where the user
     * can view cellular radio details and select "NR only" under "Set Preferred Network Type".
     *
     * Walks an extensive OEM fallback chain (AOSP, Pixel, OnePlus, Xiaomi, Samsung, Motorola, etc.).
     */
    fun openRadioInfo(context: Context): Boolean {
        val radioIntents = listOf(
            // 1. Standard AOSP / Pixel / OnePlus / Motorola RadioInfo
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.RadioInfo")),
            Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.android.settings", "com.android.settings.RadioInfo")),
            // 2. Phone / Telephony settings RadioInfo
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.settings.RadioInfo")),
            Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.android.phone", "com.android.phone.settings.RadioInfo")),
            // 3. Testing Settings activity (which contains "Phone information" as the top item)
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TestingSettings")),
            Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.android.settings", "com.android.settings.TestingSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$TestingSettingsActivity")),
            // 4. Xiaomi / HyperOS CIT Phone Info
            Intent().setComponent(ComponentName("com.miui.cit", "com.miui.cit.cit_phone_info")),
            // 5. Samsung ServiceMode
            Intent().setComponent(ComponentName("com.sec.android.app.servicemodeapp", "com.sec.android.app.servicemodeapp.ServiceModeApp")),
            // 6. Dialer with *#*#4636#*#* code
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode("*#*#4636#*#*"))),
            // 7. General mobile network operator settings fallback
            Intent("android.settings.NETWORK_OPERATOR_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in radioIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Keep trying next fallback
            }
        }
        return false
    }

    fun open(context: Context, target: Target): Boolean {
        if (target == Target.RADIO_INFO) {
            return openRadioInfo(context)
        }

        val actions = target.actions + Settings.ACTION_SETTINGS   // last-resort fallback
        for (action in actions) {
            try {
                val intent = Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Try next action
            }
        }
        return false
    }
}
