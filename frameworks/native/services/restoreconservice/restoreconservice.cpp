#define LOG_TAG "restoreconservice"

#include <android-base/logging.h>
#include <binder/IPCThreadState.h>

#include "RestoreconService.h"

namespace android {
namespace os {

int restoreconserviced_main(int /*argc*/, char** argv) {
    setenv("ANDROID_LOG_TAGS", "*:v", 1);
    android::base::InitLogging(argv);

    LOG(INFO) << "restoreconservice firing up";

    // Binder IRestoreconService startup
    sp<RestoreconService> restoreconService = new android::os::RestoreconService();
    restoreconService->publish(restoreconService);
    IPCThreadState::self()->joinThreadPool();

    LOG(INFO) << "restoreconservice shutting down";

    return 0;
}

}  // namespace os
}  // namespace android

int main(const int argc, char *argv[]) {
    return android::os::restoreconserviced_main(argc, argv);
}
