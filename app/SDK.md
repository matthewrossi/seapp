# SEApp Software Development Kit

## Why you may need it?

When the `ext4` filesystem deals with SELinux security contexts, the default
behavior for new files is to inherit the security context of the parent folder.

However, as described in [SEApp Policy Module](README.md), we want to give you,
the developer, control over the security context of your application files.
To grant this, we reuse the SELinux scheme and let the developer specify the
security context of its application files in the `file_contexts` file.

The `file_contexts` specification still is not enough to overwrite the default
behavior, therefore we implemented a system service that interpose file
creation to make sure the security context given to the file on creation
corresponds to the one specified by the developer.
To offer this _alternative_ file creation to developers we extended the
standard Android API with `android.os.File`, they can access our extended
API by building the SEApp SDK and using it within their IDE.

## Build it

Follow the instruction in [Enstablishing a Build Environment](https://github.com/matthewrossi/seapp#enstablishing-a-build-environment)
and then the instructions in [How To Build SDK](https://android.googlesource.com/platform/sdk/+/master/docs/howto_build_SDK.txt),
skipping those steps to download the source, as you already have the AOSP source tree.

## Include it in Android Studio

Open Android Studio, from the Menu Bar select Tools and SDK Manager.
Finally, from the SDK Manager window change the Android SDK Location to match
the SEApp SDK previously built.
