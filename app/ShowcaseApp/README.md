# Showcase app

Here we give a tour of SEApp capabilities using a Showcase app.
At a high level perspective, SEApp enables:

- fine-granularity in access to files

- fine-granularity in access to services

- isolation of vulnerability prone components


The Showcase app implements a set of use cases, each of them related to one or more common vulnerability classes.
We show how that:

- the Showcase app is perfectly working using a stock version of the system and is thus subject to the vulnerabilities

- if we enable the enforcement of the app policy module, the app is working but is no longer subject to the vulnerabilities.

To test it, just install the app on with/without the policy module and compare the execution at runtime. 

## Prerequisites

To build the apk create the folder `/app/libs` and save into it an unzipped copy of the [UnityAdsLibrary](https://github.com/Unity-Technologies/unity-ads-android/releases/download/3.6.0/UnityAds.aar.zip).
If you want to equip the Showcase app with the policy module follow the instructions shown [here](../../script).

## Demo

You can either use a physical device (Pixel 2 XL / Pixel 3) or the emulator (`sdk_phone_x86_64`) to run the Showcase app.
There are some slightly differences between them. Please reference to the specific use case section.

### Use Case 2 - Services

In this use case we show how to support the execution of an Ads Library having guarantees that the library cannot abuse the access privileges granted by the
user to the whole application sandbox. To give you an example, we prevent the library to access some system services sush as the location. 
In our demonstration we confine the library into an ad-hoc process and show that a malicious component running inside the same process is prevented to access to the
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

