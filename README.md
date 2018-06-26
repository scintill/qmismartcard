# qmismartcard

An experiment in building a Java-compatible smartcard interface to a SIM card, on top of QMI over CDC-WDM, mainly aiming to work with SRLabs' [SIMtester](https://opensource.srlabs.de/projects/simtester).

# Caveats/TODO

* SIM is assumed to be present in the phone
* Device path is hardcoded in QmiSmartcardProvider
* Have not thoroughly checked against the results on a PCSC reader
* App DeSelect does not seem to work - QMI blocks it?
* UIM indications pile up in duplicates from previous runs of the progarm - clean up somehow?
* Other TODOs in code
* Currently hangs at the end of SIMTester scan
* Linux-only - it's probably not too hard on Windows though

# Usage

From `adb shell` (over wifi):

```
su
stop ril-daemon
stop qmuxd
echo -n 0 > /sys/class/android_usb/android0/enable
echo -n 05C6 > /sys/class/android_usb/android0/idVendor
echo -n 9025 > /sys/class/android_usb/android0/idProduct
echo -n diag > /sys/class/android_usb/android0/f_diag/clients
echo -n smd,tty > /sys/class/android_usb/android0/f_serial/transports
echo -n smd,bam > /sys/class/android_usb/android0/f_rmnet/transports
echo -n diag,adb,serial,rmnet > /sys/class/android_usb/android0/functions
echo -n 1 > /sys/class/android_usb/android0/enable
```

With java VM, launch net.scintill.qmi.smartcard.SIMTesterMain, with classpath pointing to its dependencies (Maven pom.xml) and SIMTester.jar in the classpath.
Pass arguments you want SIMtester to use.

# Notes

If things get stuck, try unplug and replug USB.
