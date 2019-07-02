#!/bin/bash -ex
#
# https://sangsoonam.github.io/2019/02/08/using-git-worktree-to-deploy-github-pages.html

if [[ $(git status -s) ]]
then
    echo "The working directory is dirty. Please commit any pending changes."
#    exit 1;
fi

echo "Deleting old publication"
rm -rf public
mkdir public
git worktree prune || true
rm -rf .git/worktrees/public/

echo "Checking out gh-pages branch into public"
git worktree add -B gh-pages public upstream/gh-pages

echo "Removing existing files"
rm -rf public/* */build/docs/javadoc/

echo "Generating site"
./gradlew javadoc
printf '<!DOCTYPE html>\n\n<html><body><h1>NetCipher Javadoc</h1><ul>' > public/index.html
for f in */build/docs/javadoc; do
    libname=`echo $f | cut -d / -f 1`
    printf "<li><a href=\"$libname\/\">$libname</a></li>" >> public/index.html
    cp -a $f public/$libname
done
printf '</ul></body></html>' >> public/index.html

echo "Updating gh-pages branch"
cd public
git add --all
git commit -m "Publishing to gh-pages ($0)"

echo "Do not forget to: git push upstream gh-pages"
