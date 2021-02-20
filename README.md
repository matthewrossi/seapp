# SEApp: Bringing Mandatory Access Control to Android Apps

SEApp enables developers to define ad-hoc Mandatory Access Control policies for
their apps. This repository provides a set of modifications to the
[Android Open Source Project](https://source.android.com/).

## Motivation

When SELinux was introduced into Android 4.3 in 2013, it used a limited set of
system domains and it was mainly aimed at separating system resources from user
apps.
In the next releases, the configuration of SELinux has progressively become
more complex, with a growing set of domains isolating different services and
resources, so that a bug or vulnerability in some system components does not
lead to a direct compromise of the whole system.
The introduction of SELinux into Android has been a clear success.
Unfortunately, the stronger protection benefits do not extend to regular apps
which are assigned with a single domain named `untrusted_app`.
Since Android 9, isolation of apps is enforced also with the use of categories,
which guarantees that distinct apps operate with incompatible security contexts.
Our proposal, SEApp, builds upon the observation that giving app developers the
ability to apply MAC to the internal structure of the app would provide a stronger
protection against a number of common [internal vulnerabilities](https://static.googleusercontent.com/media/www.google.com/en//about/appsecurity/play-rewards/Android_app_vulnerability_classes.pdf) (see the [Showcase app](./app/ShowcaseApp) to have a glimpse of SEApp capabilities).

The following image depicts the evolution of SELinux since its introduction in
Android, and the changes introduced by SEApp.

<p align="center">
    <img src="https://user-images.githubusercontent.com/15113769/94135458-c9dad280-fe63-11ea-901d-94c47fb20d59.jpg"
        alt="Evolution of the MAC policy in Android" width="75%">
</p>

## Design and objectives

SEApp enables developers to define ad-hoc Mandatory Access Control policies for their apps.

While developing SEApp, dedicated attention was paid to:

- preserve system security assumptions (e.g., do not alter the default behavior of key system services, do not provide to `untrusted_app` potentially dangerous SELinux permissions, etc.)
- provide a solution with negligible performance impact at runtime and limited performance impact at install time
- give to the developers an easy-to-use solution, that does not require the developer to understand system security internals
- provide a solution that is fully backward compatible

The latest version of this set of modifications satisfies the previous requirements, though further improvements
may be necessary to facilitate the use of SEApp to a wider range of application developers.

## Enstablishing a Build Environment

Ensure your system meets the [hardware and software requirements](https://source.android.com/setup/build/requirements).

Set up your local work environment to build the Android source files.
[Establishing a Build Environment](https://source.android.com/setup/build/initializing) details all the required steps,
according to your operating system.

Download the source tree for the specific `android-9.0.0_r39` code-line, which is what this branch is based on.
See [Downloading the Source](https://source.android.com/setup/build/downloading) for the step-by-step instructions.

Finally, overwrite the AOSP files with the ones provided in this repository to
add SEApp functionality within the AOSP source tree.

## Build and run it

Follow the [Building Android](https://source.android.com/setup/build/building) guide.

## Tested on

### Devices

- Pixel 3 ([binaries](https://developers.google.com/android/drivers#bluelineqq3a.200805.001))
- Pixel 2 XL ([binaries](https://developers.google.com/android/drivers#taimenqq3a.200805.001))

### Emulator

Follow the instruction in [Establishing a Build Environment](#enstablishing-a-build-environment) and [Build and run it](#build-and-run-it) with the 
exception that when choosing the build target you should choose `sdk_phone_x86_64` (as suggested in [building AVD images](https://source.android.com/setup/create/avd#building_avd_images)).

To improve emulator performance we recommend to enable CPU virtualization extensions and build an x86 64-bit AVD.

## Supported Android versions

- Android 10 ([branch](https://github.com/matthewrossi/seapp/tree/android-10.0.0))
- Android 9 ([branch](https://github.com/matthewrossi/seapp/tree/android-9.0.0))
- Android 8.1 ([branch](https://github.com/matthewrossi/seapp/tree/android-8.1.0))

# Paper

The work is described by the publication:

- title `SEApp: Bringing Mandatory Access Control to Android Apps`
- authors: `Matthew Rossi, Dario Facchinetti, Enrico Bacis, Marco Rosa` and `Stefano Paraboschi`

(to appear in the 30th USENIX Security Symposium)
