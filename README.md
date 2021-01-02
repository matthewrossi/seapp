# SEApp: Bringing Mandatory Access Control to Android Apps

SEApp enables developers to define ad-hoc Mandatory Access Control policies for
their apps.
To embed such functionality in Android this repository provides a patch to the
[Android Open Source Project](https://source.android.com/).

## Motivation

When SELinux was introduced into Android 4.3 in 2013, it used a limited set of
system domains and it was mainly aimed at separating system resources from user
apps.
In the next releases, the configuration of SELinux has progressively become
more complex, with a growing set of domains isolating different services and
resources, so that a bug or vulnerability in some system component does not
lead to a direct compromise of the whole system.
The introduction of SELinux into Android has been a clear success.
Unfortunately, the stronger protection benefits do not extend to regular apps
which are assigned with a single domain named _untrusted_app_.
Since Android 9, isolation of apps has increased with the use of categories,
which guarantees that distinct apps operate on separate security contexts.
Our proposal, SEApp, builds upon the observation that giving app developers the
ability to apply MAC to the internal structure of the app would provide more
robust protection against other apps and internal vulnerabilities.

The following image depicts the evolution of SELinux since its introduction in
Android and shows how SEApp represents a natural evolution of the security
mechanisms already available in Android.

<p align="center">
    <img src="https://user-images.githubusercontent.com/15113769/94135458-c9dad280-fe63-11ea-901d-94c47fb20d59.jpg"
        alt="Evolution of the MAC policy in Android" width="85%">
</p>

## Objective

SEApp accomplishes the extension of Android enabling developers to define
ad-hoc Mandatory Access Control policies for their apps.

While at it we aim at:

- preserving the security of system components
- limiting the performance impact
- providing developers with an easy to use solution

The latest version satisfy system components security assumptions, while
keeping its performance footprint to a minimum.
However further improvements may be necessary to facilitate the use of
SEApp to whider range of application developers.

## Enstablishing a Build Environment

Ensure your system meets the [hardware and software requirements](https://source.android.com/setup/build/requirements).

Set up your local work environment to build the Android source files.
See [Establishing a Build Environment](https://source.android.com/setup/build/initializing) for installation instructions by operating system.

Download the source tree for the specific `android-9.0.0_r39` code-line, which is what this branch is based on.
See [Downloading the Source](https://source.android.com/setup/build/downloading) for the step-by-step instructions.

Finally, overwrite the AOSP files with the ones provided in this repository to
add SEApp functionality within the AOSP source tree.

## Build and run it

Follow the [Building Android](https://source.android.com/setup/build/building) guide.

## Tested on

- Pixel 2 XL ([binaries](https://developers.google.com/android/drivers#taimenpq3a.190505.001))
- Pixel 3 ([binaries](https://developers.google.com/android/drivers#bluelinepq3a.190505.002))

## Supported Android versions

- Android 8.1 ([branch](https://github.com/matthewrossi/seapp/tree/android-8.1.0))
- Android 9 ([branch](https://github.com/matthewrossi/seapp/tree/android-9.0.0))
- Android 10 ([branch](https://github.com/matthewrossi/seapp/tree/android-10.0.0))
