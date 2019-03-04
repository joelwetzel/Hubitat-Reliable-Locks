/**
 *  Reliable Lock Virtual Device  (Do not use outside of the Reliable Locks app!!!)
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

	
metadata {
	definition (name: "Reliable Lock Virtual Device", namespace: "joelwetzel", author: "Joel Wetzel") {
		capability "Refresh"
		capability "Sensor"
        capability "Actuator"
		capability "Lock"
		
		command "markAsLocked"
		command "markAsUnlocked"
	}
	
	preferences {
		section {
		}
	}
}


def log (msg) {
	if (getParent().enableDebugLogging) {
		log.debug msg
	}
}


def installed () {
	log.info "${device.displayName}.installed()"
    updated()
}


def updated () {
	log.info "${device.displayName}.updated()"
}


// Tell the parent app to reliably refresh the physical lock
def refresh() {
	log "${device.displayName}.refresh()"
	
	def parent = getParent()
	if (parent == null) {
		return
	}
	
	parent.refreshWrappedLock()
}


// Tell the parent app to reliably lock the physical lock
def lock() {
	log "${device.displayName}.lock()"
	
	def parent = getParent()
	if (parent == null) {
		return
	}
	
	parent.lockWrappedLock()
}


// Tell the parent app to reliably unlock the physical lock
def unlock() {
	log "${device.displayName}.unlock()"
	
	def parent = getParent()
	if (parent == null) {
		return
	}

	parent.unlockWrappedLock()
}


// Mark as locked without sending the event back to the parent app.  Called when the physical lock has locked, to prevent cyclical firings.
def markAsLocked() {
	log "${device.displayName}.markAsLocked()"
	
	sendEvent(name: "lock", value: "locked")
}


// Mark as unlocked without sending the event back to the parent app.  Called when the physical lock has unlocked, to prevent cyclical firings.
def markAsUnlocked() {
	log "${device.displayName}.markAsUnlocked()"
	
	sendEvent(name: "lock", value: "unlocked")
}




