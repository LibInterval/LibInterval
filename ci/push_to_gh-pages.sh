#!/bin/bash

targetRepo=github.com/jrybak2312/siderian.git
cd docs
git init
git config user.name "travis@travis-ci.org"
git config user.email "Travis CI"
git add *.html
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "https://${GH_TOKEN}@${targetRepo}" master:gh-pages > /dev/null 2>&1