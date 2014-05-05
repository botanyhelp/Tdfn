"""Standalone program to find, edit, and rename .obj files for copying to facebase hub. 
Requirements to run properly:
-must be on Linux, Unix or OSX because of os-specific filenaming and heavy shell usage. 
-must properly specify SRCDIR variable (i.e. obj files must be underneath and readable)
-notice especially the escaped nature of SRCDIR, (i.e spaces are escaped)....python does 
---not need the escaping but the shell does!!!
-must have writable access to /tmp
-must have python2 and these modules: subprocess, fnmatch, os, re, hashlib
-must have enough local free disk space to keep two copies of all obj files
"""

import subprocess
from fnmatch import fnmatch
import os
import re
import hashlib

SRCDIR = "/Volumes/ResearchData/"

class filesanddirs(object):
    """object for getting files and dirs"""
    def __init__(self, path):
        self.path = path

    def getAllFiles(self):
        fileList = []
        for rootpath, dirs, files in os.walk(self.path):
            for file in files:
                fileList.append(file)

        return fileList

#remove the remmants of the last run:
ret = subprocess.call("rm -rf /tmp/threedfinal/", shell=True, stdout=open('/dev/null', 'w'), stderr=subprocess.STDOUT)
if(ret == 0):
    print("SUCCESS: removed final files and dir from last run")
else:
    print("FAILED: could not remove final files and dir from last run")

#make two writable directories...one to hold source files and another to hold renamed and edited files:a 
ret = subprocess.call("mkdir /tmp/threed/", shell=True, stdout=open('/dev/null', 'w'), stderr=subprocess.STDOUT)
if(ret == 0):
    print("SUCCESS: made /tmp/threed/")
else:
    print("FAILED: couldn't create /tmp/threed/")
ret = subprocess.call("mkdir /tmp/threedfinal/", shell=True, stdout=open('/dev/null', 'w'), stderr=subprocess.STDOUT)
if(ret == 0):
    print("SUCCESS: made /tmp/threedfinal/")
else:
    print("FAILED: couldn't create /tmp/threedfinal/")

#find all of the important files and copy them to local area for editing/renaming:
#cmdfind = 'find /var/tmp/junk/ -name "*.obj" -exec cp {} /tmp/threed/ \;'
cmdfind = 'find ' + SRCDIR + ' -name "*.obj" -exec cp {} /tmp/threed/ \;'
ret = subprocess.call(cmdfind, shell=True, stderr=subprocess.STDOUT)
if(ret == 0):
    print("SUCCESS: found some files.")
else:
    print("FAILED: find failed" + str(ret))

#extract subjectid from filenames, calculate md5(subjectid), rename files to md5(subjectid).obj, edit files to replace subid with md5:
fad = filesanddirs("/tmp/threed/")
files = fad.getAllFiles()
for file in files:
    if fnmatch(str(file),"*.obj"):
        subid = re.sub('_1_Clean.obj', '', str(file))
        mdfive = hashlib.md5(str(subid)).hexdigest()
        cmdtwo = "cat /tmp/threed/" + str(file) + "|sed 's/" + str(subid) + "/" + mdfive + "/' >> /tmp/threedfinal/" + mdfive + ".obj"
        print("about to run: " + cmdtwo)
        ret = subprocess.call(cmdtwo, shell=True, stdout=open('/dev/null', 'w'), stderr=subprocess.STDOUT)
        if(ret == 0):
            print("SUCCESS: ran sed to make files")
        else:
            print("FAILED: sed failed to make files")
        
ret = subprocess.call("rm -rf /tmp/threed/", shell=True, stdout=open('/dev/null', 'w'), stderr=subprocess.STDOUT)
if(ret == 0):
    print("SUCCESS: removed source files and dir")
else:
    print("FAILED: could not remove source files and dir")

print("SUCCESS?: Look in /tmp/threedfinal/ to find your renamed and edited obj files")
