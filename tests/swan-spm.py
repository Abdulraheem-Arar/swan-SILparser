# FOR TESTING ONLY
# Call 'swift package clean' beforehand

import sys
import os
import shutil
import time
import re
import subprocess
import platform

SWAN_DIR="swan-dir/"
SRC_DIR=SWAN_DIR + "src/"
SOURCES_DIR="Sources/"
SPM_LOG=SWAN_DIR + "spm.log"
OUTPUT_SIL=SWAN_DIR + "test.sil"
COMMAND=["swift", "build"]

# As of Xcode 12.5, dumping SIL with unsafeFlags requires using --use-integrated-swift-driver
if platform.system() == "Darwin":
  xcode_version = subprocess.check_output(["/usr/bin/xcodebuild", "-version"])
  version_search = re.search("Xcode ([0-9]+)\\.([0-9]+)", xcode_version.decode())
  xcode_version = float(version_search.group(1) + "." + version_search.group(2))
  if version_search:
    if xcode_version >= 12.5:
      COMMAND.append("--use-integrated-swift-driver")
else: # Linux requires this flag, too
  COMMAND.append("--use-integrated-swift-driver")

with open("Package.swift") as f:
  if "unsafeFlags" not in f.read():
    sys.exit("Package.swift is not configured for SIL dumping. See tests/README.md")

if not os.path.exists(SWAN_DIR):
  os.mkdir(SWAN_DIR)
if os.path.exists(SRC_DIR):
  shutil.rmtree(SRC_DIR)

file_set = set()

for dir_, _, files in os.walk(SOURCES_DIR):
    for file_name in files:
        rel_dir = os.path.relpath(dir_, SOURCES_DIR)
        rel_file = os.path.join(rel_dir, file_name)
        file_set.add(rel_file)

found = False
for i in file_set:
  if i.endswith(".swift"):
    if found is True:
      sys.exit("swan-spm only supports one source Swift file")
    dstdir = (SRC_DIR+SOURCES_DIR+i).rsplit("/", 1)[0]
    if not os.path.exists(dstdir):
      os.makedirs(dstdir)
    shutil.copy(SOURCES_DIR+i, SRC_DIR+SOURCES_DIR+i)
    found = True

print("Running " + " ".join(COMMAND))
start_time = time.time()
output = subprocess.run(COMMAND, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
txt = output.stdout.decode('utf-8')

print("swift build finished in %ds" % (time.time() - start_time))

with open(SPM_LOG, "w") as f:
  f.writelines(txt)

print("swift build output written to %s" % SPM_LOG)

if output.returncode != 0:
  print("swift build failed. Please see %s " % SPM_LOG)

print("")

sil_search = re.search('(.|\\s)*?\\n\\n\\n\\n', txt.split("\nsil_stage canonical")[1])
if sil_search:
  with open(OUTPUT_SIL, "w") as f:
    f.writelines("sil_stage canonical"+sil_search.group(0))
else:
  sys.exit("Unexpected sil output. See %s " % SPM_LOG)

print("SIL written to " + OUTPUT_SIL)
