#!/usr/bin/python

import argparse
import glob
import os
import subprocess
import tempfile
import zipfile

from shutil import copytree
from shutil import make_archive
from shutil import rmtree


parser = argparse.ArgumentParser(description="Change apk to include its policy module.")
parser.add_argument("signer", metavar="SIGNER", help="keystore to sign the apk")
parser.add_argument("app", metavar="APP", help="app project directory")
parser.add_argument("-i", "--input", help="original apk")
parser.add_argument("-p", "--policy", help="policy module directory")
parser.add_argument("-o", "--output", help="modified apk")

args = parser.parse_args()

input = args.input if args.input else args.app + "/app/build/outputs/apk/release/app-release-unsigned.apk"
policy = args.policy if args.policy else args.app + "/policy"
output = args.output if args.output else args.app + "/app/build/outputs/apk/release/app-release.apk"

tmp = tempfile.gettempdir()
tmpdir = tmp + "/unzipped"
tmpzip = tmp + "/" + os.path.splitext(os.path.basename(output))[0] + ".zip"

try:
    # clean up policy directory
    rm_list = glob.glob(policy + "/*~")
    for rm_path in rm_list:
        os.remove(rm_path)
    # unzip archive
    with zipfile.ZipFile(args.input, 'r') as zip:
        zip.extractall(tmpdir)
    # copy policy module within the unzipped archive
    copytree(policy, tmpdir + "/policy/")
    # zip it back
    make_archive(os.path.splitext(tmpzip)[0], "zip", tmpdir)
    rmtree(tmpdir)
    # align the apk file
    subprocess.run(["/home/matt/android/sdks/sdk/build-tools/28.0.3/zipalign", "-f", "-p", "4", tmpzip, output])
    os.remove(tmpzip)
    # sign apk using the given signer
    subprocess.run(["/home/matt/android/sdks/sdk/build-tools/28.0.3/apksigner", "sign", "--ks", args.signer, output])
except OSError as err:
    print(err)

print("Success")
