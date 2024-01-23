git clone --mirror https://github.com/FongMi/Release.git
java -jar bfg.jar --delete-files *.apk Release.git
java -jar bfg.jar --delete-files *.json Release.git
cd Release.git
git reflog expire --expire=now --all && git gc --prune=now --aggressive
git push
git gc