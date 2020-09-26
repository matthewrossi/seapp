# Scripts

## [attach-policy-to-apk.py](script/attach-policy-to-apk.py)

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
with the SEApp policy module, we provide the [attach-policy-to-apk.py](script/attach-policy-to-apk.py)
script.

### Prerequisites

Download the Android SDK Build Tools revision correspondant to the Android
version you are targeting and add it to your path.

### Usage

```
usage: attach-policy-to-apk.py [OPTIONS] SIGNER APP

Change apk to include its policy module.

positional arguments:
  SIGNER                keystore to sign the apk
  APP                   app project directory

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        original apk
  -p POLICY, --policy POLICY
                        policy module directory
  -o OUTPUT, --output OUTPUT
                        modified apk
```
