package com.fongmi.hook;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import java.util.List;

public class PackageManager extends android.content.pm.PackageManager {

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags) {
        return new PackageInfo();
    }

    @Override
    @TargetApi(26)
    public PackageInfo getPackageInfo(VersionedPackage versionedPackage, int flags) {
        return getPackageInfo(versionedPackage.getPackageName(), flags);
    }

    @Override
    public String[] currentToCanonicalPackageNames(String[] packageNames) {
        return new String[0];
    }

    @Override
    public String[] canonicalToCurrentPackageNames(String[] packageNames) {
        return new String[0];
    }

    @Override
    public Intent getLaunchIntentForPackage(String packageName) {
        return null;
    }

    @Override
    public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        return null;
    }

    @Override
    public int[] getPackageGids(String packageName) {
        return new int[0];
    }

    @Override
    public int[] getPackageGids(String packageName, int flags) {
        return new int[0];
    }

    @Override
    public int getPackageUid(String packageName, int flags) {
        return 0;
    }

    @Override
    public PermissionInfo getPermissionInfo(String permName, int flags) {
        return null;
    }

    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String permissionGroup, int flags) {
        return null;
    }

    @Override
    public PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags) {
        return null;
    }

    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags) {
        return null;
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName component, int flags) {
        return null;
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName component, int flags) {
        return null;
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName component, int flags) {
        return null;
    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName component, int flags) {
        return null;
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return null;
    }

    @Override
    public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
        return null;
    }

    @Override
    public int checkPermission(String permName, String packageName) {
        return android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean isPermissionRevokedByPolicy(String permName, String packageName) {
        return false;
    }

    @Override
    public boolean addPermission(PermissionInfo info) {
        return false;
    }

    @Override
    public boolean addPermissionAsync(PermissionInfo info) {
        return false;
    }

    @Override
    public void removePermission(String permName) {
    }

    @Override
    public int checkSignatures(String packageName1, String packageName2) {
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        return new String[0];
    }

    @Override
    public String getNameForUid(int uid) {
        return null;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return null;
    }

    @Override
    public boolean isInstantApp() {
        return false;
    }

    @Override
    public boolean isInstantApp(String packageName) {
        return false;
    }

    @Override
    public int getInstantAppCookieMaxBytes() {
        return 0;
    }

    @Override
    public byte[] getInstantAppCookie() {
        return new byte[0];
    }

    @Override
    public void clearInstantAppCookie() {
    }

    @Override
    public void updateInstantAppCookie(byte[] cookie) {
    }

    @Override
    public String[] getSystemSharedLibraryNames() {
        return new String[0];
    }

    @Override
    public List<SharedLibraryInfo> getSharedLibraries(int flags) {
        return null;
    }

    @Override
    public ChangedPackages getChangedPackages(int sequenceNumber) {
        return null;
    }

    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        return new FeatureInfo[0];
    }

    @Override
    public boolean hasSystemFeature(String featureName) {
        return false;
    }

    @Override
    public boolean hasSystemFeature(String featureName, int version) {
        return false;
    }

    @Override
    public ResolveInfo resolveActivity(Intent intent, int flags) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        return null;
    }

    @Override
    public ResolveInfo resolveService(Intent intent, int flags) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        return null;
    }

    @Override
    public ProviderInfo resolveContentProvider(String authority, int flags) {
        return null;
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        return null;
    }

    @Override
    public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) {
        return null;
    }

    @Override
    public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return null;
    }

    @Override
    public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        return null;
    }

    @Override
    public Drawable getActivityIcon(ComponentName activityName) {
        return null;
    }

    @Override
    public Drawable getActivityIcon(Intent intent) {
        return null;
    }

    @Override
    public Drawable getActivityBanner(ComponentName activityName) {
        return null;
    }

    @Override
    public Drawable getActivityBanner(Intent intent) {
        return null;
    }

    @Override
    public Drawable getDefaultActivityIcon() {
        return null;
    }

    @Override
    public Drawable getApplicationIcon(ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationIcon(String packageName) {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(String packageName) {
        return null;
    }

    @Override
    public Drawable getActivityLogo(ComponentName activityName) {
        return null;
    }

    @Override
    public Drawable getActivityLogo(Intent intent) {
        return null;
    }

    @Override
    public Drawable getApplicationLogo(ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationLogo(String packageName) {
        return null;
    }

    @Override
    public Drawable getUserBadgedIcon(Drawable drawable, UserHandle user) {
        return null;
    }

    @Override
    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        return null;
    }

    @Override
    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return null;
    }

    @Override
    public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
        return null;
    }

    @Override
    public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
        return null;
    }

    @Override
    public CharSequence getApplicationLabel(ApplicationInfo info) {
        return null;
    }

    @Override
    public Resources getResourcesForActivity(ComponentName activityName) {
        return null;
    }

    @Override
    public Resources getResourcesForApplication(ApplicationInfo app) {
        return null;
    }

    @Override
    public Resources getResourcesForApplication(String packageName) {
        return null;
    }

    @Override
    public void verifyPendingInstall(int id, int verificationCode) {
    }

    @Override
    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
    }

    @Override
    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
    }

    @Override
    public String getInstallerPackageName(String packageName) {
        return null;
    }

    @Override
    public void addPackageToPreferred(String packageName) {
    }

    @Override
    public void removePackageFromPreferred(String packageName) {
    }

    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        return null;
    }

    @Override
    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
    }

    @Override
    public void clearPackagePreferredActivities(String packageName) {
    }

    @Override
    public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        return 0;
    }

    @Override
    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
    }

    @Override
    public int getComponentEnabledSetting(ComponentName componentName) {
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
    }

    @Override
    public int getApplicationEnabledSetting(String packageName) {
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public boolean isSafeMode() {
        return false;
    }

    @Override
    public void setApplicationCategoryHint(String packageName, int categoryHint) {
    }

    @Override
    public PackageInstaller getPackageInstaller() {
        return null;
    }

    @Override
    public boolean canRequestPackageInstalls() {
        return false;
    }
}
