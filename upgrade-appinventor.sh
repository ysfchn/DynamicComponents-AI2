#!/bin/bash

# Run this command to update the App Inventor dependencies to reflect
# the latest version of App Inventor. You can optionally include a
# version as an argument to the script to target that specific version
# of App Inventor, e.g.:
#     ./upgrade-appinventor.sh v185
# will load the libraries for v185.

COMMIT=${1:-origin/master}

if [ ! -d "lib/appinventor" ]; then
    git submodule add https://github.com/mit-cml/extension-deps.git lib/appinventor/
    git submodule update --init lib/appinventor/
fi

cd lib/appinventor
git fetch origin
git checkout $COMMIT
TAG=`git describe --tag --abbrev=0`
cd ../..
git add lib/appinventor
git commit -m "Update to App Inventor version $TAG"
