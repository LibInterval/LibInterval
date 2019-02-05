#!/bin/bash

if [ "$TRAVIS_BRANCH" != "master" ]; then exit 0; fi

targetRepo=github.com/LibInterval/LibInterval.git
cd target/generated-docs
git init
git config user.name "travis@travis-ci.org"
git config user.email "Travis CI"
git add *.html
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "https://${GH_TOKEN}@${targetRepo}" master:gh-pages > /dev/null 2>&1
cd ../..