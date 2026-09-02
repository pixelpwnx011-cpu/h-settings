package com.geneo.hiddensettings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.geneo.hiddensettings.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * A shortcut launcher straight into specific Android system settings screens, for
 * boards whose custom launcher/settings app hides the normal navigation (e.g. no
 * visible "Apps" section). Each shortcut tries its intended intent first, then
 * falls back through alternates, and finally falls back to the general Settings
 * screen if nothing more specific is available on this device/OS version.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private data class Shortcut(val title: String, val desc: String, val actions: List<Intent>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildShortcutList()

        binding.btnQuickClock.setOnClickListener { binding.etPackageName.setText("com.geneo.clockoverlay") }
        binding.btnQuickTools.setOnClickListener { binding.etPackageName.setText("com.geneo.classroomtools") }
        binding.btnQuickBooks.setOnClickListener { binding.etPackageName.setText("com.geneo.ncertbooks") }

        binding.btnCheckRestrictions.setOnClickListener { checkDeviceRestrictions() }

        binding.btnAppInfo.setOnClickListener {
            withPackage { pkg ->
                tryLaunch(listOf(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                ))
            }
        }
        binding.btnAppOverlay.setOnClickListener {
            withPackage { pkg ->
                tryLaunch(listOf(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg")),
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                ))
            }
        }
        binding.btnAppBattery.setOnClickListener {
            withPackage { pkg ->
                tryLaunch(listOf(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, Uri.parse("package:$pkg")),
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                ))
            }
        }
    }

    private fun withPackage(action: (String) -> Unit) {
        val pkg = binding.etPackageName.text.toString().trim()
        if (pkg.isEmpty()) {
            Toast.makeText(this, "Enter a package name first", Toast.LENGTH_SHORT).show()
            return
        }
        action(pkg)
    }

    // Human-readable descriptions for the restriction flags most relevant to
    // diagnosing an overlay/kiosk-mode problem. Any OTHER restriction the OS
    // reports gets shown too (by its raw key), just without a description --
    // this isn't a hardcoded allowlist, it's a lookup table for nicer wording.
    private val restrictionDescriptions = mapOf(
        UserManager.DISALLOW_CREATE_WINDOWS to "Blocks apps from creating overlay windows -- this is the one that would directly explain 'Display over other apps' being blocked or reset.",
        UserManager.DISALLOW_INSTALL_APPS to "Blocks installing new apps.",
        UserManager.DISALLOW_UNINSTALL_APPS to "Blocks uninstalling apps.",
        UserManager.DISALLOW_DEBUGGING_FEATURES to "Blocks USB debugging / developer options.",
        UserManager.DISALLOW_FACTORY_RESET to "Blocks factory reset from Settings.",
        UserManager.DISALLOW_SAFE_BOOT to "Blocks booting into Android's safe mode.",
        UserManager.DISALLOW_CONFIG_VPN to "Blocks VPN configuration.",
        UserManager.DISALLOW_APPS_CONTROL to "Blocks changing app settings (force stop, clear data, etc.) for other apps.",
        UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA to "Blocks mounting USB drives / physical media -- would affect the pendrive features in other Geneo apps.",
        UserManager.DISALLOW_USB_FILE_TRANSFER to "Blocks USB file transfer.",
        UserManager.DISALLOW_CONFIG_DATE_TIME to "Blocks changing date & time settings.",
        UserManager.DISALLOW_ADD_USER to "Blocks adding new user profiles.",
        UserManager.DISALLOW_SET_WALLPAPER to "Blocks changing the wallpaper.",
    )

    /** Reads Android's standard user-restriction flags -- available to any app,
     *  no special permission needed, since these are policy facts about the
     *  current user/profile rather than another app's private data. This is the
     *  most a regular sideloaded app can find out about device management
     *  without being the managing app itself: it can see WHICH restrictions are
     *  active, but not WHO set them (that's deliberately hidden by Android). */
    private fun checkDeviceRestrictions() {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        val restrictions: Bundle = userManager.userRestrictions
        val activeKeys = restrictions.keySet().filter { restrictions.getBoolean(it, false) }

        val message = if (activeKeys.isEmpty()) {
            "No standard Android device-restriction flags are active on this profile.\n\nIf overlay permission is still being blocked or reset, it's likely enforced some other way -- a vendor-specific security layer that doesn't go through this standard Android API -- rather than a normal Android device-management policy."
        } else {
            activeKeys.joinToString("\n\n") { key ->
                val desc = restrictionDescriptions[key]
                if (desc != null) "• $key\n$desc" else "• $key\n(active, but no description available for this one)"
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (activeKeys.isEmpty()) "No restrictions found" else "${activeKeys.size} restriction(s) active")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun buildShortcutList() {
        val shortcuts = listOf(
            Shortcut(
                "All Apps",
                "Full list of installed apps and their permissions/storage/battery settings",
                listOf(
                    Intent(Settings.ACTION_APPLICATION_SETTINGS),
                    Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                )
            ),
            Shortcut(
                "Default Apps",
                "Change default browser, home app, and other default handlers",
                listOf(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            ),
            Shortcut(
                "Home App / Launcher",
                "Switch which launcher is used as the Home screen",
                listOf(Intent(Settings.ACTION_HOME_SETTINGS))
            ),
            Shortcut(
                "Display Over Other Apps",
                "See and change which apps can draw overlays on top of others",
                listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            ),
            Shortcut(
                "Battery Optimization List",
                "See and change which apps are exempt from battery optimization",
                listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            ),
            Shortcut(
                "Security Settings",
                "Device admin apps, screen lock, and other security options",
                listOf(Intent(Settings.ACTION_SECURITY_SETTINGS))
            ),
            Shortcut(
                "Accessibility Settings",
                "Screen readers, magnification, and other accessibility services",
                listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            ),
            Shortcut(
                "Date & Time Settings",
                "Check whether the clock/date is correct and syncing properly",
                listOf(Intent(Settings.ACTION_DATE_SETTINGS))
            ),
            Shortcut(
                "Wi-Fi Settings",
                "Manage Wi-Fi networks and connection",
                listOf(Intent(Settings.ACTION_WIFI_SETTINGS))
            ),
            Shortcut(
                "Storage Settings",
                "See used/free storage space",
                listOf(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            ),
            Shortcut(
                "Developer Options",
                "USB debugging and other developer settings (must be enabled first by tapping the build number 7 times in About Phone, if not already visible)",
                listOf(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            ),
            Shortcut(
                "All Settings",
                "The general Settings home screen, as a fallback",
                listOf(Intent(Settings.ACTION_SETTINGS))
            ),
        )

        val inflater = LayoutInflater.from(this)
        for (shortcut in shortcuts) {
            val row = inflater.inflate(R.layout.row_shortcut, binding.shortcutContainer, false)
            row.findViewById<TextView>(R.id.tvShortcutTitle).text = shortcut.title
            row.findViewById<TextView>(R.id.tvShortcutDesc).text = shortcut.desc
            row.setOnClickListener { tryLaunch(shortcut.actions) }
            binding.shortcutContainer.addView(row)
        }
    }

    /** Tries each intent in order, falling back to the next on failure, and
     *  finally to the general Settings screen if none of them work on this
     *  device/OS version. */
    private fun tryLaunch(actions: List<Intent>) {
        for (intent in actions) {
            try {
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                continue
            } catch (e: Exception) {
                continue
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            Toast.makeText(this, "That specific screen isn't available here — opened general Settings instead", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open Settings on this device", Toast.LENGTH_LONG).show()
        }
    }
}
