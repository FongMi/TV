name: Test Build

on:
#  push:
#    branches:
#      - main
#  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      - name: Build With Gradle
        run: |
          chmod +x gradlew
          ./gradlew assemblerelease --build-cache --parallel --daemon --warning-mode all
      - name: Prepare App
        run: |
          mkdir -p ${{ github.workspace }}/apk/
          for file in `find ~ -name "*.apk" -print`; do
            mv "$file" ${{ github.workspace }}/apk/
          done
      - name: Upload App To Artifact
        uses: actions/upload-artifact@v3
        with:
          name: com.github.tvbox.osc
          path: ${{ github.workspace }}/apk/*
