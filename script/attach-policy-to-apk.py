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

# default params
def readParam(pname):
    with open(pname, 'r') as f:
        return f.readline().rstrip("\n")
    
unsigned_apk_path = readParam(".unsigned_apk_path")
signed_apk_path = readParam(".signed_apk_path")
policy_default_path = readParam(".policy_default_path")
signer = readParam(".signer")

# command line helper configuration
parser = argparse.ArgumentParser(description="Utility to attach a policy module to an app")
parser.add_argument("-i", "--input", help="unsigned apk location")
parser.add_argument("-o", "--output", help="signed apk location")
parser.add_argument("-p", "--policy", help="policy module directory location")
parser.add_argument("-s", "--signer", help="location to the keystore to sign the APK")
args = parser.parse_args()

# read input
in_path = args.input if args.input else unsigned_apk_path
out_path = args.output if args.output else signed_apk_path
policy_path = args.policy if args.policy else policy_default_path
signer_path = args.signer if args.signer else signer

# script
tmp = tempfile.gettempdir()
tmpdir = tmp + "/unzipped"
tmpzip = tmp + "/" + os.path.splitext(os.path.basename(out_path))[0] + ".zip"

try:
    # clean up policy directory
    rm_list = glob.glob(policy_path + "/*~")
    for rm_path in rm_list:
        os.remove(rm_path)
    # unzip archive
    with zipfile.ZipFile(in_path, 'r') as zip:
        zip.extractall(tmpdir)
    # copy policy module within the unzipped archive
    copytree(policy_path, tmpdir + "/policy/")
    # zip it back
    make_archive(os.path.splitext(tmpzip)[0], "zip", tmpdir)
    rmtree(tmpdir)
    # align the apk file
    subprocess.run(["zipalign", "-f", "-p", "4", tmpzip, out_path])
    os.remove(tmpzip)
    # sign apk using the given signer
    subprocess.run(["apksigner", "sign", "--ks", signer_path, out_path])
except OSError as err:
    print(err)

print("Success")
