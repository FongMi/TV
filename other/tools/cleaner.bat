git clone --mirror https://github.com/FongMi/TV.git
java -jar bfg.jar --delete-files *.apk TV.git
cd TV.git
git reflog expire --expire=now --all && git gc --prune=now --aggressive
git push
git gc