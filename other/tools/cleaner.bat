git clone --mirror https://github.com/FongMi/TV.git
java -jar bfg.jar --delete-files *.apk TV.git
java -jar bfg.jar --delete-files libp*p.so TV.git
java -jar bfg.jar --delete-files libxl*.so TV.git
java -jar bfg.jar --delete-files libjpa*.so TV.git
java -jar bfg.jar --delete-files libp2p*.so TV.git
java -jar bfg.jar --delete-files libmitv.so TV.git
java -jar bfg.jar --delete-files libtvcore.so TV.git
cd TV.git
git reflog expire --expire=now --all && git gc --prune=now --aggressive
git push
git gc