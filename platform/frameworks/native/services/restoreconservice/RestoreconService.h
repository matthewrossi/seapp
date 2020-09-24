#ifndef ANDROID_RESTORECONSERVICE_RESTORECONSERVICE_H
#define ANDROID_RESTORECONSERVICE_RESTORECONSERVICE_H

#include <unistd.h>

#include "android/os/BnRestoreconService.h"

namespace android {
namespace os {

class RestoreconService : public BnRestoreconService {

public:
    RestoreconService();
    void publish(const sp<RestoreconService>& service);
    binder::Status restoreFileContext(const std::string& pathname);
    binder::Status restoreFileContextRecursive(const std::string& pathname);

private:
    bool isSELinuxDisabled;
    bool native_restorecon(const std::string& pathname, int flags);

};

}  // namespace os
}  // namespace android

#endif // ANDROID_RESTORECONSERVICE_RESTORECONSERVICE_H