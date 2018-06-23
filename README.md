# qmismartcard

An experiment in building a Java-compatible smartcard interface on top of QMI over CDC-WDM.

# Usage

From `adb shell` (over wifi):

```
su
echo -n 0 > /sys/class/android_usb/android0/enable
echo -n 05C6 > /sys/class/android_usb/android0/idVendor
echo -n 9025 > /sys/class/android_usb/android0/idProduct
echo -n diag > /sys/class/android_usb/android0/f_diag/clients
echo -n smd,tty > /sys/class/android_usb/android0/f_serial/transports
echo -n smd,bam > /sys/class/android_usb/android0/f_rmnet/transports
echo -n diag,adb,serial,rmnet > /sys/class/android_usb/android0/functions
echo -n 1 > /sys/class/android_usb/android0/enable
```

The code currently just dumps MSISDN for a test.
