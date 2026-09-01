package com.geneo.hiddensettings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.geneo.hiddensettings.databinding.ActivityMainBinding

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
