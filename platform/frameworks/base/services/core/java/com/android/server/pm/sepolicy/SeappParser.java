package com.android.server.pm.sepolicy;

import android.util.Slog;

import java.util.ArrayList;

public class SeappParser {

    private static final String TAG = "SeappParser";
    private static final boolean DEBUG_SEAPP = false;

    /* input selectors */
    private static final String KEY_IS_SYSTEM_SERVER = "isSystemServer";
    private static final String KEY_IS_EPHEMERAL_APP = "isEphemeralApp";
    private static final String KEY_IS_V2_APP = "isV2App";
    private static final String KEY_IS_OWNER = "isOwner";
    private static final String KEY_IS_PRIV_APP = "isPrivApp";
    private static final String KEY_MIN_TARGET_SDK_VERSION = "minTargetSdkVersion";
    private static final String KEY_USER = "user";
    private static final String KEY_SEINFO = "seinfo";
    private static final String KEY_NAME = "name";
    private static final String KEY_PATH = "path";
    /* output selectors */
    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_TYPE = "type";
    private static final String KEY_LEVEL_FROM = "levelFrom";

    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";
    private static final String VALUE_LEVELFROM_ALL = "all";
    private static final String VALUE_APP = "_app";
    private static final String VALUE_ISOLATED = "_isolated";
    private static final String VALUE_ISOLATED_APP = "isolated_app";

    protected class SeappContext{
        /* input selectors */
        boolean isV2App;
        boolean isOwner;
        int minTargetSdkVersion = 0;
        String user;
        boolean seInfoSet;
        String name;
        /* outputs */
        String domain;
        boolean levelFromSet;
    }

    private String seInfo;
    private String pkgName;
    private ArrayList<SeappContext> seappContexts;

    public SeappParser(String seInfo, String pkgName) {
        this.seInfo = seInfo;
        this.pkgName = pkgName;
        seappContexts = new ArrayList<>();
    }

    public boolean addSeappContext(String line) {

        int pos;
        String[] kvps = line.split("([ \t])");
        String key, value;
        SeappContext newSeappContext = new SeappContext();

        for (String kvp : kvps) {
            pos = kvp.indexOf("=");
            if (pos == -1)
                return false;
            key = kvp.substring(0, pos);
            value = kvp.substring(pos + 1);

            switch (key) {
                case KEY_IS_SYSTEM_SERVER:
                case KEY_IS_EPHEMERAL_APP:
                case KEY_IS_PRIV_APP:
                    // 3rd-party apps can't be system_server
                    // ephemeral apps must run in ephemeral domain
                    // 3rd-party apps can't be preinstalled in /system/priv-app
                    if (!VALUE_FALSE.equals(value))
                        return false;
                    break;
                case KEY_IS_V2_APP:
                    // no restriction on apk signing method
                    if (VALUE_TRUE.equals(value))
                        newSeappContext.isV2App = true;
                    else if (VALUE_FALSE.equals(value))
                        newSeappContext.isV2App = false;
                    else
                        return false;
                    break;
                case KEY_IS_OWNER:
                    // no restriction on user type (primary/secondary)
                    if (VALUE_TRUE.equals(value))
                        newSeappContext.isOwner = true;
                    else if (VALUE_FALSE.equals(value))
                        newSeappContext.isOwner = false;
                    else
                        return false;
                    break;
                case KEY_MIN_TARGET_SDK_VERSION:
                    newSeappContext.minTargetSdkVersion = getMinTargetSdkVersion(value);
                    if (newSeappContext.minTargetSdkVersion < 0)
                        return false;
                    break;
                case KEY_USER:
                    if (newSeappContext.user != null)
                        return false;
                    if (!VALUE_APP.equals(value) && !VALUE_ISOLATED.equals(value))
                        return false;
                    newSeappContext.user = value;
                    break;
                case KEY_SEINFO:
                    if (newSeappContext.seInfoSet)
                        return false;
                    newSeappContext.seInfoSet = true;
                    if (!seInfo.equals(value) || value.contains(":"))
                        return false;
                    break;
                case KEY_NAME:
                    if (newSeappContext.name != null)
                        return false;
                    newSeappContext.name = value;
                    break;
                case KEY_DOMAIN:
                    if (newSeappContext.domain != null)
                        return false;
                    newSeappContext.domain = value;
                    break;
                case KEY_TYPE:
                    Slog.e(TAG, "'type' is not supported in third-party apps' seapp_contexts, use file_contexts instead.");
                    return false;
                case KEY_LEVEL_FROM:
                    if (newSeappContext.levelFromSet)
                        return false;
                    if (!VALUE_LEVELFROM_ALL.equals(value))
                        return false;
                    newSeappContext.levelFromSet = true;
                    break;
                case KEY_PATH:
                    Slog.e(TAG, "'path' is not supported in third-party apps' seapp_contexts, use file_contexts instead.");
                    return false;
                default:
                    return false;
            }
        }

        if (newSeappContext.user == null || newSeappContext.name == null ||
                !newSeappContext.seInfoSet || newSeappContext.domain == null ||
                !newSeappContext.levelFromSet)
            return false;

        // _isolated run in isolated_app domain and processName = pkgName
        if (VALUE_ISOLATED.equals(newSeappContext.user) &&
                (!VALUE_ISOLATED_APP.equals(newSeappContext.domain) ||
                !pkgName.equals(newSeappContext.name)))
            return false;

        // _app can't run in isolated_app domain
        if (VALUE_APP.equals(newSeappContext.user) &&
                VALUE_ISOLATED_APP.equals(newSeappContext.domain))
            return false;

        seappContexts.add(newSeappContext);
        return true;
    }

    private int getMinTargetSdkVersion(String value) {
        long intValue = Long.parseLong(value);
        if (value.length() > 10 || intValue < 0 || intValue > Integer.MAX_VALUE)
            return -1;
        return (int) intValue;
    }

    public boolean hasDuplicateContext() {

        if (DEBUG_SEAPP) {
            for (SeappContext seappContext : seappContexts) {
                Slog.d(TAG, "isV2App=" + seappContext.isV2App + " isOwner=" + seappContext.isOwner
                        + " user=" + seappContext.user + "seinfo=" + seInfo + " name=" + seappContext.name
                        + " minTargetSdkVersion=" + seappContext.minTargetSdkVersion
                        + " -> domain=" + seappContext.domain + " levelFrom=all");
            }
        }

        SeappContext a, b;
        for (int i = 0; i < seappContexts.size(); i++) {
            for (int j = i + 1; j < seappContexts.size(); j++) {
                a = seappContexts.get(i);
                b = seappContexts.get(j);
                if (a.isV2App == b.isV2App && a.isOwner == b.isOwner && a.user.equals(b.user) &&
                        a.name.equals(b.name)  && a.minTargetSdkVersion == b.minTargetSdkVersion) {
                    return true;
                }
            }
        }

        return false;
    }

    public ArrayList<String> getContextsDomains() {
        ArrayList<String> domains = new ArrayList<>();
        for (SeappContext seappContext : seappContexts) {
            domains.add(seappContext.domain);
        }
        return domains;
    }

}
