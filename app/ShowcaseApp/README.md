# Showcase app

Here we give a tour of SEApp capabilities using a Showcase app.
At a high level perspective, SEApp enables:

- fine-granularity in access to files

- fine-granularity in access to services

- isolation of vulnerability prone components


The Showcase app implements a set of use cases, each of them related to one or more common vulnerability classes.
We demonstrate that:

- the Showcase app is perfectly working using a stock version of the system and is thus subject to the vulnerabilities

- if we enable the enforcement of the app policy module, the app is working but is no longer subject to the vulnerabilities.

To test it, just install the app on with/without the policy module and compare the execution at runtime. 

## Prerequisites

UC2 - To build the apk create the folder `/app/libs` and save into it an unzipped copy of the [UnityAdsLibrary](https://github.com/Unity-Technologies/unity-ads-android/releases/download/3.6.0/UnityAds.aar.zip).

UC3 - First you need to install in Android Studio the NDK toolkit. Go to Tools > SDK Manager and ensure you are using the modified SDK. Move to the SDK Tools tab, and install the NDK (Side by side), the Android SDK Command-line Tools and CMake. Restart Android Studio. Then create the folder `/app/src/main/jniLibs`. Open the terminal and go to the folder `$YOUR-PATH-TO-THE-MODIFIED-SKD/ndk/$NDK-VERSION/`. From that location execute the bash command `ndk-build; cp -r ../libs/arm64-v8a ../src/main/jniLibs; cp -r ../libs/x86_64 ../src/main/jniLibs/` to build the `.so` shared library files and move it to the right location. Now you can build the APK with Android Studio.

Injecting the policy module - If you want to equip the Showcase app with the policy module follow the instructions shown [here](../../script).

## Demo

You can either use a physical device (Pixel 2 XL / Pixel 3) or the emulator (`sdk_phone_x86_64`) to run the Showcase app.
There are some slightly differences between them. Please reference to the specific use case section.

### Use Case 2 - Services

In this use case we show how to support the execution of an Ads Library having guarantees that the library cannot abuse the access privileges granted by the
user to the whole application sandbox. To give you an example, we prevent the library to access some system services sush as the location. 
In our demonstration we confine the library into an ad-hoc process and show that a malicious component running inside the same process is prevented to access to 
the location service. 
In this case the malicious component is directly invoked by the library when the show ads routine is executed.
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

We initially create a shared library which is shipped within the application. Then we call the library via JNI. We imagine a scenario in which the shared library is used to process media, so we connect to the camera service (that might be useful for the duty) in the C-side of the code. The shared library returns the handle to the camera service to the current use case activity, and it is then called (again) to carry out some basic algebra computation. Finally, we get to our vulnerability. The rationale is that we don't want shared libraries (that may be subject to memory corruption errors and so on) to be executed in a process that has access to the network. But, access to network state is granted at install time to the whole application sandbox, so the shared library code will still be able to get a reference to the connectivity manager. We demonstrate that access to the network can be restricted in the sepolicy file by using the macros in an easy way. The potentially vulnerable library would still be able to connect to the connectivity service, but when the policy is enforcing, the current process won't be able to bind itself to the network, as SELinux denies the `create` on `udp_socket`.
