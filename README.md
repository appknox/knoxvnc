# KnoxVNC

This is an Android VNC server using contemporary Android 5+ APIs. It therefore does not require
root access. Forked from https://github.com/bk138/droidVNC-NG

## How to use

1. Download the application from the github releases section or build yourself
2. Get it all the permissions required.
3. Set a good password and consider turning the `Start on Boot` off.
4. Connect to your local Wi-Fi. For accepting a connection your device should be connected to some Local Area Network that you can control, normally it is a router. Connections via data networks (i.e. your mobile provider) are not supported.
5. Click `Start` and connect to your device.
6. If keyboard is required, please provide root/SU access to the application via a root solution (Magisk, KernelSU etc). If **Progisk** is being used, add KnoxVNC to the `Superuser Isolation` as an exemption for granting root access.

### Keyboard Shortcuts From a VNC Viewer

* **Ctrl-Shift-Esc** triggers 'Recent Apps' overview
* **Home/Pos1** acts as Home button
* **Escape** acts as Back button
* Supports full keyboard input via ADB input wrapper service (requires superuser access)

##### Start Server

```shell
adb shell am start-foreground-service \
 -n com.appknox.vnc/.VNCService \
 -a com.appknox.vnc.ACTION_START \
 --ei com.appknox.vnc.EXTRA_PORT 5900
```

##### Stop Server

```shell
 adb shell am start-foreground-service \
 -n com.appknox.vnc/.VNCService \
 -a com.appknox.vnc.ACTION_STOP
```

## Building

* After cloning the repo, make sure you have the required git submodules set up via `git submodule update --init`.
* Then simply build via Android Studio or `gradlew`.

## Notes

* Requires at least Android 7.

* [Since Android 10](https://developer.android.com/about/versions/10/privacy/changes#screen-contents),
the permission to access the screen contents has to be given on each start and is not saved. You can,
however, work around this by installing ADB
(or simply Android Studio) on a PC, connecting the device running droidVNC-NG to that PC and running
`adb shell cmd appops set com.appknox.vnc PROJECT_MEDIA allow` once.

* You can also use adb to manually give input permission prior to app start via `adb shell settings put secure enabled_accessibility_services com.appknox.vnc/.InputService:$(adb shell settings get secure enabled_accessibility_services)`.

* Persistant notification will be displayed when the application is running the VNC service as it uses `startForeground()` method which needs a handle to a notification object (requirement by android).
 
