#!/bin/bash
rm output.apks
./gradlew bundleDonationDebug && java -jar ~/bundletool-all.jar build-apks --bundle=app/build/outputs/bundle/donationDebug/app-donation-debug.aab --output=output.apks --local-testing && java -jar ~/bundletool-all.jar install-apks --apks=output.apks
