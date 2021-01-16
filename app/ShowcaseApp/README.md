# Showcase app

Here we give a tour of SEApp capabilities using a Showcase app.
At a high level perspective, SEApp enables:

- fine-granularity in access to files
- fine-granularity in access to services
- isolation of vulnerability prone components

The Showcase app implements a set of use cases, each of them related to one or more common vulnerability classes.

We demonstrate that:

- the Showcase app is perfectly working without a policy module and, thus, subject to the vulnerabilities
- the Showcase app is working but the vulnerabilities are no longer exploitable when we enable the enforcement of the policy module

To test it, just install the app with/without the policy module and compare the app behavior at runtime.

## Prerequisites

- Ensure you are using our modified version of the Android SDK by following instructions in [our SDK specific readme](../SDK.md)

- Create the `app/libs` folder and store into it a copy of the unzipped [UnityAdsLibrary](https://github.com/Unity-Technologies/unity-ads-android/releases/download/3.6.0/UnityAds.aar.zip) (unity-ads.aar).

- Install in Android Studio the following SDK extensions:

  - NDK (Side by side)
  - Android SDK Command-line Tools
  - CMake

  By going under Tools > SDK Manager, selecting the SDK extensions previously mentioned and applying changes.

- Create the `app/src/main/jniLibs` folder, open a terminal and execute the following commands to build the `.so` shared library files and move them to the right location.

  ```bash
  cd $SHOWCASEAPP_ROOT_DIRECTORY/app
  $PATH_TO_SDK/ndk/$NDK_VERSION/ndk-build
  cp -r libs/arm64-v8a libs/x86_64 src/main/jniLibs
  ```

- This last step is not something required to make the application work per se, but it is the step required to "inject" the SEApp policy module into the ShowcaseApp APK and, therefore, check how the app behavior changes between not using SEApp features and using them. You can see the instructions [here](../../script/README.md), where we describe the prerequisites and usage of the [attach-policy-to-apk.py](../../script/attach-policy-to-apk.py)

## Demo

You can either use a physical device (Pixel 2 XL / Pixel 3) or the emulator (`sdk_phone_x86_64`) to run the Showcase app.
There are some slightly differences between them. Please reference to the specific use case section.

### Use Case 1 - Files

This Use Case focuses on the benefits that a SEApp has over a normal app when
dealing with access to its internal storage.

An app is built of multiple components, each one focusing on its subset
of features. Each component as part of the same sandbox have full access
to the app internal storage, however due to the high diversity of
functionalities an app provides, not all components are born equal.
Some may manage sensitive user information, while others may be exposed to
untrusted interactions either from the user or other applications.

Exploiting this components diversity with SEApp, we can compartimentalize
components and control their access to the app internal storage.

As a demonstration we implemented an activity vulnerable to path traversal.
The activity is quite straightforward, it displays the content of the file
given its relative path through an intent. While this may not be exploitable
when the intent is given by trusted components within the same app, the
activity also supports implicit intents coming from untrusted sources.

By sending this specifically crafted intent, therefore, we can exploit the
vulnerable activity and see the content of any target file within the
application internal storage.
```bash
adb shell am start -n com.example.showcaseapp/.UseCase1Activity -a "com.example.showcaseapp.intent.action.SHOW" --es "com.example.showcaseapp.intent.extra.PATH" "../confidential/data"
```

However, with the use of SEApp we can give the untrusted component access to
only a subset of the application internal storage, and by doing so we can
ensure the `confidential` directory cannot be accessed even when a path traversal
vulnerability is exploited.

### Use Case 2 - Services

In this use case we show how to support an Ads Library execution and, at the
same time, guarantee that it cannot abuse access privileges granted to the
whole application by the user.
To give you an example, we prevent the library to access some system services,
such as the location.

In our demonstration we confine the library into an ad-hoc process and show
that a malicious component, running inside the same process, is prevented to access
the location service.
In this case the malicious component is directly invoked by the library when the
show ads routine is executed.
In our demonstration we used the UnityAds library only as it is a well known non-platform framework;
the policy violating component is specifically injected by us for demo purposes.

To test this use case, we need to access the location. Since the AOSP is not typically equipped with real location providers, we need in some way to simulate it.

If you are using a physical device follow these steps:

1. enable the developer options on your device

2. enable the location

3. install a mock location app, for example [FakeTraveler](https://github.com/mcastillof/FakeTraveler)

4. in system settings, select the mock location app

5. open the mock location app and enable (repeated) location changes

6. start the Showcase app

If you are using the emulator we recommend to send GPS location following these steps:

1. start the emulator

2. send GPS location changes via `adb` using the command `adb emu geo fix <longitude> <latitude>`

3. start the Showcase app

If you are using the emulator you could also connect to it via telnet and then send updates via console. Please keep in mind that
the emulator is sometimes subject to unexpected behavior; if the showcase app is not retrieving the position correctly, restarting the emulator fixes the problem.

### Use Case 3 - Isolation of media

In this use case we demonstrate how native components, that are relying on shared/static libraries that may be prone to vulnerabilites, can be isolated from the rest of the appliacation. Isolation works again at process level.

We initially create a shared library which is shipped within the application. Then we call the library via JNI. We imagine a scenario in which the shared library is used to process media, so we connect to the camera service (that might be useful for the duty) in the C-side of the code. The shared library successfully returns the handle to the camera service to the current use case activity (true also when the policy is active, since `find` on camera service is granted). The shared library is then called (again) to carry out some basic algebra computation. Finally, we get to our vulnerability. The rationale is that we don't want shared libraries (that may be subject to memory corruption errors and so on) to be executed in a process that has access to the network. But, access to network state (state) is granted at install time to the whole application sandbox, so the shared library code running in a stock version of the system will still be able to get a reference to the connectivity manager and succesfully bind the current process to the network. We demonstrate that this is no longer possible with our approach, as network access can be restricted in the sepolicy file by using the macros in an easy way. The potentially vulnerable library would still be able to connect to the connectivity service, but when the policy is enforcing, the current process won't be able to bind itself to the network, as SELinux denies the `create` on `udp_socket`.
