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


preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
		section(getFormat("title", "Reliable Lock Instance")) {
			input (
	            name:				"wrappedLock",
	            type:				"capability.lock",
	            title:				"Wrapped Lock",
	            description:		"Select the lock to WRAP IN RELIABILITY.",
	            multiple:			false,
	            required:			true
            )
			input (
                name:				"refreshTime",
	            type:				"number",
	            title:				"After sending commands to lock, delay this many seconds and then refresh the lock",
	            defaultValue:		6,
	            required:			true
            )
			input (
            	name:				"autoRefreshOption",
	            type:				"enum",
	            title:				"Auto refresh every X minutes?",
	            options:			["Never", "1", "5", "10", "30" ],
	            defaultValue:		"30",
	            required:			true
            )
			input (
                type:               "bool",
                name:               "retryLockCommands",
                title:              "Retry lock/unlock commands if the lock doesn't respond the first time?",
                required:           true,
                defaultValue:       false
            )
		}
        section() {
            input (
				type:               "bool",
				name:               "enableDebugLogging",
				title:              "Enable Debug Logging?",
				required:           true,
				defaultValue:       true
			)
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
		log.info "Deleting child device: ${it.displayName}"
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
	
	if (autoRefreshOption == "30") {
		runEvery30Minutes(refreshWrappedLock)
	}
	else if (autoRefreshOption == "10") {
		runEvery10Minutes(refreshWrappedLock)
	}
	else if (autoRefreshOption == "5") {
		runEvery5Minutes(refreshWrappedLock)
	}
	else if (autoRefreshOption == "1") {
		runEvery1Minute(refreshWrappedLock)
	}
	else {
		unschedule(refreshWrappedLock)	
	}
}


def lockWrappedLock() {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")
	
	log "${reliableLock.displayName}:locking detected"
	log "${wrappedLock.displayName}:locking"
	wrappedLock.lock()
    
    state.desiredLockState = "locked"
    state.retryCount = 0
	
	runIn(refreshTime, refreshWrappedLock)
}


def unlockWrappedLock() {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")
	
	log "${reliableLock.displayName}:unlocking detected"
	log "${wrappedLock.displayName}:unlocking"
	wrappedLock.unlock()
    
    state.desiredLockState = "unlocked"
    state.retryCount = 0
	
	runIn(refreshTime, refreshWrappedLock)
}


def refreshWrappedLock() {
	log "${wrappedLock.displayName}:refreshing"
	wrappedLock.refresh()
    
    if (retryLockCommands) {
        runIn(5, retryIfCommandNotFollowed)
    }
}


def retryIfCommandNotFollowed() {
    log "${wrappedLock.displayName}:retryIfCommandNotFollowed"
    
    // Check if the command had been followed.
    def commandWasFollowed = wrappedLock.currentValue("lock") == state.desiredLockState
    
    if (!commandWasFollowed) {
        log "Command was not followed. RetryCount is ${state.retryCount}."
        
        // Check if we have exceeded 2 retries.
        if (state.retryCount < 3) {
            // If we still need to retry, fire off lockWrappedLock or unlockWrappedLock again.
            state.retryCount = state.retryCount + 1
            if (state.desiredLockState == "locked") {
                log "${wrappedLock.displayName}:locking"
	            wrappedLock.lock()
            }
            else {
                log "${wrappedLock.displayName}:unlocking"
	            wrappedLock.unlock()
            }
            runIn(refreshTime, refreshWrappedLock)
        }
    }
}


def wrappedLockHandler(evt) {
	def reliableLock = getChildDevice("Reliable-${wrappedLock.displayName}")

	if (wrappedLock.currentValue("lock") == "locked") {
		log "${wrappedLock.displayName}:locked detected"
		log "${reliableLock.displayName}:setting locked"
		reliableLock.markAsLocked()
        state.desiredLockState = "locked"
	}
	else {
		log "${wrappedLock.displayName}:unlocked detected"
		log "${reliableLock.displayName}:setting unlocked"
		reliableLock.markAsUnlocked()
        state.desiredLockState = "unlocked"
	}
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}




