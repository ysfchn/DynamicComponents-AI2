#!/bin/bash

# This is a script that updates the App Inventor repository in
# lib/appinventor with a build of the latest version of App
# Inventor. In general, extension developers do not need to run this
# script. The MIT team will run this script to update the
# dependencies. Extension developers should run upgrade-appinventor.sh
# to retrieve the latest version of the dependencies as needed.

if [ ! -d '.appinventor' ]; then
    git clone https://github.com/mit-cml/appinventor-sources.git .appinventor
fi

# Build the latest version of App Inventor
cd .appinventor/appinventor
git pull
TAG=`git describe --tags --abbrev=0`
git checkout $TAG
ant clean PlayApp
cd ../..

# Update the libraries in the dependency repo
cp .appinventor/appinventor/build/components/deps/* \
   .appinventor/appinventor/components/build/AnnotationProcessor.jar \
   .appinventor/appinventor/build/components/AndroidRuntime.jar \
   lib/appinventor/
cd lib/appinventor
git add .
MSG="App Inventor release $TAG"
git commit -m "$MSG"
git tag -s $TAG -m "$MSG"
cd ../..

# Point to the new version
git add lib/appinventor
git commit -m "Update to $MSG"
git tag -s $TAG -m "Update to $MSG"
