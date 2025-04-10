# Basic app for making a Bluetooth connection to a Raspberry Pi Pico W

## Current capabilities-  
* Will only connect to devices the app has connected to previously  
    * Working on being able to find new devices
* Send a message to the Pico W  
* Receive a message from the Pico W  

## Notes-  
* The gradle is set for an ______  
    * Connect the Android device to the computer via USB, select the correct device and run
    * You can simply build it to find any build issues (faster than running)
    * To change device SDK stuff, go into ```Aroma/app/build.gradle.kts```
    * Warning- Find specific phone models and they're SDK versions
* The code is written to use Bluetooth Classic  
* Most permissions are being ignored for the time being
* The bulk of the code is located at ```Aroma/app/src/main/java/com/example/myapplication/MainActivity.kt``` of the repo
    * Future work- clean up
* Work to be done- modify and add the Bluetooth functionality to the actual app

## Code adapted from Android Studio guides-
* https://developer.android.com/develop/connectivity/bluetooth/setup 
