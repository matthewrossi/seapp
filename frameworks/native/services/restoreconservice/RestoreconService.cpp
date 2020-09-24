#include "RestoreconService.h"

#include <android-base/logging.h>
#include <android-base/stringprintf.h>
#include <binder/IServiceManager.h>
#include <log/log.h>
#include <selinux/selinux.h>
#include <selinux/android.h>

#ifndef LOG_TAG
#define LOG_TAG "restoreconservice"
#endif

using android::base::StringPrintf;

namespace android {
namespace os {

namespace {

    static binder::Status ok() {
        return binder::Status::ok();
    }

    static binder::Status error(const std::string& msg) {
        PLOG(ERROR) << msg;
        return binder::Status::fromServiceSpecificError(errno, String8(msg.c_str()));
    }

} // namespace

static int log_callback(int type, const char *fmt, ...) {
    va_list ap;
    int priority;

    switch (type) {
    case SELINUX_WARNING:
        priority = ANDROID_LOG_WARN;
        break;
    case SELINUX_INFO:
        priority = ANDROID_LOG_INFO;
        break;
    default:
        priority = ANDROID_LOG_ERROR;
        break;
    }
    va_start(ap, fmt);
    LOG_PRI_VA(priority, "SELinux", fmt, ap);
    va_end(ap);
    return 0;
}

RestoreconService::RestoreconService() {
    union selinux_callback cb;
    cb.func_log = log_callback;
    selinux_set_callback(SELINUX_CB_LOG, cb);

    isSELinuxDisabled = (is_selinux_enabled() != 1) ? true : false;
}

// Publish the supplied RestoreconService to servicemanager.
void RestoreconService::publish(const sp<RestoreconService>& service) {
    defaultServiceManager()->addService(String16("restorecon"), service);
}

bool RestoreconService::native_restorecon(const std::string& pathname, int flags) {
    if (isSELinuxDisabled) {
        return true;
    }

    if (pathname.c_str() == NULL) {
        return false;
    }

    int ret = selinux_android_restorecon(pathname.c_str(), flags);
    return (ret == 0);
}

binder::Status RestoreconService::restoreFileContext(const std::string& pathname) {
    if (!native_restorecon(pathname, SELINUX_ANDROID_RESTORECON_DATADATA)) {
        return error(StringPrintf("restoreFileContext(%s) failed", pathname.c_str()));
    }
    return ok();
}

binder::Status RestoreconService::restoreFileContextRecursive(const std::string& pathname) {
    if (!native_restorecon(pathname, SELINUX_ANDROID_RESTORECON_RECURSE |
                                    SELINUX_ANDROID_RESTORECON_DATADATA)) {
        return error(StringPrintf("restoreFileContextRecursive(%s) failed", pathname.c_str()));
    }
    return ok();
}

};
};