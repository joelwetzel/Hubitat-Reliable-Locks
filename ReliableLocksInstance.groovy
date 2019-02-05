/**
 *  Reliable Locks Instance
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

import groovy.json.*
	
definition(
	parent: "joelwetzel:Reliable Locks",
    name: "Reliable Locks Instance",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "Child app that is instantiated by the Reliable Locks app.  It creates the binding between the physical lock and the virtual reliable lock.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


def wrappedLock = [
	name:				"wrappedLock",
	type:				"capability.lock",
	title:				"Wrapped Lock",
	description:		"Select the lock to WRAP IN RELIABILITY.",
	multiple:			false,
	required:			true
]


def refreshTime = [
	name:				"refreshTime",
	type:				"number",
	title:				"Delay before Refresh",
	defaultValue:		6,
	required:			true
]


def autoRefresh = [
	name:				"autoRefresh",
	type:				"bool",
	title:				"Auto refresh every 10 minutes?",
	defaultValue:		true,
	required:			true
]


preferences {
	page(name: "mainPage", title: "<b>Wrapped Lock:</b>", install: true, uninstall: true) {
		section("") {
			input wrappedLock
			input refreshTime
			input autoRefresh
		}
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	addChildDevice("joelwetzel", "Reliable Lock Virtual Device", "Reliable-${wrappedLock.displayName}", null, [name: "Reliable-${wrappedLock.displayName}", label: "Reliable ${wrappedLock.displayName}", completedSetup: true, isComponent: true])
	
	initialize()
}


def uninstalled() {
    childDevices.each {
		log.debug "Deleting child device: ${it.displayName}"
		deleteChildDevice(it.deviceNetworkId)
	}
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")
	
	subscribe(wrappedLock, "lock", wrappedLockHandler)

	// Generate a label for this child app
	app.updateLabel("Reliable ${wrappedLock.displayName}")
	
	// Make sure the ReliableLock state matches the WrappedLock upon initialization.
	wrappedLockHandler(null)
	
	if (autoRefresh == true) {
		runEvery10Minutes(refreshWrappedLock)
	}
	else {
		unschedule(refreshWrappedLock)	
	}
}


def lockWrappedLock() {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")
	
	log.debug "${reliableLock.displayName}:locking detected"
	log.debug "${wrappedLock.displayName}:locking"
	wrappedLock.lock()
	
	runIn(refreshTime, refreshWrappedLock)
}


def unlockWrappedLock() {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")
	
	log.debug "${reliableLock.displayName}:unlocking detected"
	log.debug "${wrappedLock.displayName}:unlocking"
	wrappedLock.unlock()
	
	runIn(refreshTime, refreshWrappedLock)
}


def refreshWrappedLock() {
	log.debug "${wrappedLock.displayName}:refreshing"
	wrappedLock.refresh()
}


def wrappedLockHandler(evt) {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")

	if (wrappedLock.currentValue("lock") == "locked") {
		log.debug "${wrappedLock.displayName}:locked detected"
		log.debug "${reliableLock.displayName}:setting locked"
		reliableLock.markAsLocked()
	}
	else {
		log.debug "${wrappedLock.displayName}:unlocked detected"
		log.debug "${reliableLock.displayName}:setting unlocked"
		reliableLock.markAsUnlocked()
	}
}



















