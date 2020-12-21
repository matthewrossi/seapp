# Scripts

## [attach-policy-to-apk.py](attach-policy-to-apk.py)

Developers interested in taking advantage of our approach to improve the
security of their apps are required to load the policy into their Android
Pacakge (APK).
A predefined directory, policy, at the root of the archive, is where the
SEApp-aware package installer will be looking for the policy module.

<p align="center">
    <img src="https://user-images.githubusercontent.com/15113769/94331700-1393fc00-ffcf-11ea-8079-0950bb7a4163.png"
        alt="SEApp policy structure" width="30%">
</p>

To automate the procedure required to modify the apk and enrich it
with the SEApp policy module, we provide the [attach-policy-to-apk.py](attach-policy-to-apk.py)
script.

### Prerequisites

Download the Android SDK Build Tools revision correspondant to the Android
version you are targeting and add it to your path.

### Usage

```
usage: attach-policy-to-apk.py [-h] [-i INPUT] [-o OUTPUT] [-p POLICY] [-s SIGNER]

Utility to attach a policy module to an app

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        unsigned apk location
  -o OUTPUT, --output OUTPUT
                        signed apk location
  -p POLICY, --policy POLICY
                        policy module directory location
  -s SIGNER, --signer SIGNER
                        location to the keystore to sign the APK
```

To avoid the specification of every parameter everytime the script is run
it is possible to specify their default values within the configuration files:

- `.unsigned_apk_path`:   path to the unsigned apk
- `.signed_apk_path`:     path to the signed apk
- `.policy_default_path`: path to the policy module directory
- `.signer`:              path to the keystore used to sign the APK

NOTE: the configuration files need to be within the directory where the
script is run.

## [backup.sh](backup.sh)

Automates the creation of a backup by making a copy of all those files with
committed changes among two code states (i.e., commits, branches, tags) over
multiple git repositories.

[backup.sh](backup.sh) is the script we use to extract our changes from the
root of the AOSP source tree. You can find these set of "patches" under the
[platform](../platform) directory.

### Prerequisites

- [Install Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [Install the Repo Launcher](https://source.android.com/setup/develop#installing-repo)
- [Download the Android source tree](https://source.android.com/setup/build/downloading)

### Usage

```
Usage:
  backup.sh [-h] [-s source] [-b backup] from to

  From the root of the AOSP source tree, backup files with committed changes
  among 'from' and 'to' commits.

  Options:
    -b  backup directory (defaults to 'backup' directory within source directory)
    -h  display help
    -s  source tree directory (defaults to current directory)
```

## [post_process_mac_perms.py](post_process_mac_perms.py)

A tool to help modify an existing `mac_permissions.xml` with additional app
certs not already found in that policy. This becomes useful when a directory
containing apps is searched and the certs from those apps are added to the
policy not already explicitly listed.

### mac_permissions.xml

The mac_permissions.xml file is used for controlling the Middleware MAC
solutions, as well as mapping a public base16 signing key with an arbitrary
`seinfo` string. Details of the files contents can be found in a comment at 
the top of the platform [mac_permissions.xml](https://android.googlesource.com/platform/system/sepolicy/+/refs/tags/android-9.0.0_r39/private/mac_permissions.xml) file.
The seinfo string, previously mentioned, is the same string that is
referenced in `seapp_contexts`.
### Usage

```
Usage:
  post_process_mac_perms [-h] -s SEINFO -d DIR -f POLICY

  -s SEINFO, --seinfo SEINFO  seinfo tag for each generated stanza
  -d DIR, --dir DIR           Directory to search for apks
  -f POLICY, --file POLICY    mac_permissions.xml policy file
```

NOTE: to highlight the need to create a valid mac_permissions.xml in the
SEApp policy module based on your app signer, we substituted our version of
the mac_permissions.xml with a [template](../app/SEPolicyTestApp/policy/mac_permissions_template.xml) you can enrich by using the [post_process_mac_perms.py](post_process_mac_perms.py) tool.
