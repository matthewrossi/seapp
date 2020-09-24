package com.android.server.pm.sepolicy;

import android.util.Slog;

import java.util.ArrayList;
import java.util.Comparator;

public class SeappParser {

    private static final String TAG = "SeappParser";

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
    private static final String VALUE_LEVELFROM_USER = "user";
    private static final String VALUE_USER = "_app";

    protected class SeappContext{
        /* input selectors */
        boolean isV2App;
        boolean isOwner;
        int minTargetSdkVersion = 0;
        boolean userSet;
        boolean seInfoSet;
        boolean nameSet;
        /* outputs */
        String domain;
        String levelFrom;    // Forced to user to simplify file_context checking
    }

    private String pkgName;
    private String seInfo;
    private ArrayList<SeappContext> seappContexts;

    public SeappParser(String pkgName, String seInfo) {
        this.pkgName = pkgName;
        this.seInfo = seInfo;
        seappContexts = new ArrayList<>();
    }

    public boolean addSeappContext(String line) {

        int pos;
        String[] kvps = line.split("([ \t])");
        String key, value;
        SeappContext newSeappContext = new SeappContext();

        for(String kvp : kvps) {
            pos = kvp.indexOf("=");
            if (pos == -1)
                return false;
            key = kvp.substring(0, pos);
            value = kvp.substring(pos + 1);

            if (KEY_IS_SYSTEM_SERVER.equals(key) || KEY_IS_EPHEMERAL_APP.equals(key)
                    || KEY_IS_PRIV_APP.equals(key)) {
                // 3rd-party apps can't be system_server
                // ephemeral apps must run in ephemeral domain
                // 3rd-party apps can't be preinstalled in /system/priv-app
                if (!VALUE_FALSE.equals(value))
                    return false;
            } else if (KEY_IS_V2_APP.equals(key)) {
                // no restriction on apk signing method
                if (VALUE_TRUE.equals(value))
                    newSeappContext.isV2App = true;
                else if (VALUE_FALSE.equals(value))
                    newSeappContext.isV2App = false;
                else
                    return false;
            } else if (KEY_IS_OWNER.equals(key)) {
                // no restriction on user type (primary/secondary)
                if (VALUE_TRUE.equals(value))
                    newSeappContext.isOwner = true;
                else if (VALUE_FALSE.equals(value))
                    newSeappContext.isOwner = false;
                else
                    return false;
            } else if (KEY_MIN_TARGET_SDK_VERSION.equals(key)) {
                newSeappContext.minTargetSdkVersion = getMinTargetSdkVersion(value);
                if (newSeappContext.minTargetSdkVersion < 0)
                    return false;
            } else if (KEY_USER.equals(key)) {
                if (newSeappContext.userSet)
                    return false;
                newSeappContext.userSet = true;
                if (!VALUE_USER.equals(value))
                    return false;
            } else if (KEY_SEINFO.equals(key)) {
                if (newSeappContext.seInfoSet)
                    return false;
                newSeappContext.seInfoSet = true;
                if (!seInfo.equals(value) || value.contains(":"))
                    return false;
            } else if (KEY_NAME.equals(key)) {
                if (newSeappContext.nameSet)
                    return false;
                newSeappContext.nameSet = true;
                if (!pkgName.equals(value))
                    return false;
            } else if (KEY_DOMAIN.equals(key)) {
                if (newSeappContext.domain != null)
                    return false;
                newSeappContext.domain = value;
            } else if (KEY_TYPE.equals(key)) {
                Slog.e(TAG, "'type' is not supported in third-party apps' seapp_contexts, use file_contexts instead.");
                return false;
            } else if (KEY_LEVEL_FROM.equals(key)) {
                if (newSeappContext.levelFrom != null)
                    return false;
                if (VALUE_LEVELFROM_USER.equals(value))
                    newSeappContext.levelFrom = VALUE_LEVELFROM_USER;
                else
                    return false;
            } else if (KEY_PATH.equals(key)) {
                Slog.e(TAG, "'path' is not supported in third-party apps' seapp_contexts, use file_contexts instead.");
                return false;
            } else {
                return false;
            }
        }

        if(!newSeappContext.nameSet || !newSeappContext.seInfoSet || newSeappContext.domain == null
                || newSeappContext.levelFrom == null)
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

        for (SeappContext seappContext : seappContexts) {
            Slog.d(TAG, "isV2App=" + seappContext.isV2App + " isOwner=" + seappContext.isOwner
                + " user=_app seinfo=" + seInfo + " name=" + pkgName
                + " minTargetSdkVersion=" + seappContext.minTargetSdkVersion
                + " -> domain=" + seappContext.domain + " levelFrom=" + seappContext.levelFrom);
        }

        SeappContext a, b;
        for (int i = 0; i < seappContexts.size(); i++) {
            for (int j = i + 1; j < seappContexts.size(); j++) {
                a = seappContexts.get(i);
                b = seappContexts.get(j);
                if (a.isV2App == b.isV2App && a.isOwner == b.isOwner &&
                        a.minTargetSdkVersion == b.minTargetSdkVersion) {
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
