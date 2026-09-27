package dev.retza.mak.update

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import org.koin.core.annotation.Single

data class InstalledAppInfo(
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val deviceSdk: Int
)

fun interface InstalledAppInfoProvider {
    fun get(): InstalledAppInfo
}

@Single(binds = [InstalledAppInfoProvider::class])
class AndroidInstalledAppInfoProvider(
    private val context: Context
) : InstalledAppInfoProvider {
    override fun get(): InstalledAppInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return InstalledAppInfo(
            versionName = packageInfo.versionName.orEmpty(),
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            packageName = context.packageName,
            deviceSdk = Build.VERSION.SDK_INT
        )
    }
}
