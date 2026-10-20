package io.github.tatooinoyo.star.badge.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.text.Collator
import java.util.Locale

/** Keeps primary channel discovery separate from the on-demand launcher app picker. */
object InstalledChannelProvider {
    private const val PROBE_LINK = "https://sky.thatg.co/?s=discovery-only"

    fun discover(context: Context): List<BadgeChannelTarget> {
        val packageManager = context.packageManager
        val intent = Intent("android.nfc.action.NDEF_DISCOVERED", Uri.parse(PROBE_LINK))
            .addCategory(Intent.CATEGORY_DEFAULT)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val collator = Collator.getInstance(Locale.getDefault())
        val detectedGameTargets = resolved.asSequence()
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val application = activity.applicationInfo ?: return@mapNotNull null
                if (!activity.exported || !activity.enabled || !application.enabled || activity.permission != null) {
                    return@mapNotNull null
                }
                val packageName = activity.packageName
                if (packageName == context.packageName) return@mapNotNull null
                // Generic browsers and NFC utilities can also resolve HTTPS/NDEF intents.
                // Only Sky game package families belong in the primary channel dropdown;
                // everything else remains available behind "choose another app".
                if (!isSkyGamePackage(packageName)) return@mapNotNull null
                val known = BadgeChannel.fromPackageName(packageName)
                BadgeChannelTarget(
                    channel = known ?: BadgeChannel.CUSTOM,
                    packageName = packageName,
                    activityName = activity.name,
                    label = known?.getLabel(context)
                        ?: info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifBlank { packageName },
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<BadgeChannelTarget> { it.channel == BadgeChannel.CUSTOM }
                    .thenComparator { left, right -> collator.compare(left.label, right.label) }
            )
            .toList()

        val detectedByPackage = detectedGameTargets.associateBy { it.packageName }
        val presetTargets = BadgeChannel.entries
            .filterNot { it == BadgeChannel.CUSTOM }
            .map { channel ->
                val detected = detectedByPackage[channel.packageName]
                val installed = isPackageInstalled(packageManager, channel.packageName)
                val launcherActivity = if (detected == null && installed) {
                    packageManager.getLaunchIntentForPackage(channel.packageName)?.component?.className
                } else null
                BadgeChannelTarget(
                    channel = channel,
                    packageName = channel.packageName,
                    activityName = detected?.activityName ?: launcherActivity ?: channel.className,
                    label = channel.getLabel(context),
                    isInstalled = installed,
                )
            }
        val customGameTargets = detectedGameTargets.filter { it.channel == BadgeChannel.CUSTOM }
        return presetTargets + customGameTargets
    }

    fun discoverOtherApps(context: Context, excludedPackages: Set<String>): List<BadgeChannelTarget> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchers = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return launchers.asSequence()
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val application = activity.applicationInfo ?: return@mapNotNull null
                val packageName = activity.packageName
                if (packageName == context.packageName || packageName in excludedPackages ||
                    !activity.exported || !activity.enabled ||
                    !application.enabled || activity.permission != null) return@mapNotNull null
                BadgeChannelTarget(
                    channel = BadgeChannel.fromPackageName(packageName) ?: BadgeChannel.CUSTOM,
                    packageName = packageName,
                    activityName = activity.name,
                    label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifBlank { packageName },
                    acceptsBadgeLinks = false,
                )
            }
            .distinctBy { it.packageName }
            .sortedWith { left, right -> collator.compare(left.label, right.label) }
            .toList()
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }.enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun isSkyGamePackage(packageName: String): Boolean =
        BadgeChannel.fromPackageName(packageName) != null ||
            packageName.startsWith("com.netease.sky.") ||
            packageName.startsWith("com.tgc.sky.")
}
