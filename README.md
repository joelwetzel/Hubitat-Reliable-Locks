# Reliable Locks app for Hubitat
An app that will create virtual locks that 'wrap' your physical lock devices, making them more reliable.

## Purpose
Some smart locks are not reliable about reporting their status changes.  For example, my Kwikset z-wave locks will sometimes accept a lock command from Hubitat, lock themselves, but never report the 'locked' state back to Hubitat.  This app 'wraps' the locks inside a virtual lock that tries to make them more reliable.  Then you expose the virtual reliable lock to your dashboards, homekit, Alexa, or Google Home user interfaces.

## Installation

The best way to install this code is by using [Hubitat Package Manager](https://community.hubitat.com/t/beta-hubitat-package-manager).

However, if you must install manually:

1. Go to the "Drivers Code" page in Hubitat
2. Click "+ New Driver"
3. Paste in the contents of ReliableLockVirtualDevice.groovy
4. Click Save.  You've now set up the driver for the virtual device.
5. Go to the "Apps Code" page in Hubitat
6. Click "+ New App"
7. Paste in the contents of ReliableLocks.groovy
8. Click Save
9. Click "+ New App" again
10. Paste in the contents of ReliableLocksInstance.groovy
11. Click Save.  You've now set up the parent and child apps.
12. Go to the "Apps" page in Hubitat
13. Click "+ Add User App"
14. Choose "Reliable Locks"
15. Click "Done"  You've now activated the parent app.  Next, we'll use it to wrap our locks in the virtual locks.
16. Click on "Reliable Locks"
17. Click the "Add a new Reliable Lock" button
18. Select a lock device and click "Done"

Now if you go to your "Devices" page, you'll find a new virtual device for your reliable lock.  If you lock or unlock this virtual device, it will lock/unlock the real device, and then do a refresh afterwards.  It can also do periodic refreshes.  And, if you physically lock/unlock the real door, those state changes will still be reflected in the virtual device.

This virtual device is the one you should expose in your dashboards and other user interfaces, such as Alexa or HomeKit.
