package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.v2ray.ang.R

/** Every section the old drawer menu had; the Settings tab of the main screen lists them all. */
enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    // Xfinder tabs, shown first in the drawer.
    XfinderConfigs(R.drawable.ic_cloud_download_24dp, R.string.xf_nav_configs),
    IpScanner(R.drawable.ic_search_24dp, R.string.xf_nav_scanner),
    XfinderLogs(R.drawable.ic_description_24dp, R.string.xf_nav_logs),
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.xf_nav_settings),
    Language(R.drawable.ic_translate_24dp, R.string.mx_nav_language),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}
