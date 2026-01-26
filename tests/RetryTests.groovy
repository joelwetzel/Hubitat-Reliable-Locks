package joelwetzel.reliable_locks.tests

import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Tests for retry logic in ReliableLocksInstance.groovy
*/
class RetryTests extends IntegrationAppSpecification {
    def lockFixture = LockFixtureFactory.create('testLock')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "ReliableLocksInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [wrappedLock: lockFixture, refreshTime: 3, autoRefreshOption: "none", retryLockCommands: true, notificationDevice: null, enableDebugLogging: true])
    }

    void "retryIfCommandNotFollowed() does not retry when command was followed"() {
        given:
        appScript.installed()
        appScript.initialize()
        appState.desiredLockState = "locked"
        appState.retryCount = 0
        lockFixture.currentValue('lock') >> 'locked'

        when:
        appScript.retryIfCommandNotFollowed()

        then:
        // Should not call lock again since command was followed
        0 * lockFixture.lock()
        appState.retryCount == 0
    }

    void "retryIfCommandNotFollowed() retries when command was not followed"() {
        given:
        appScript.installed()
        appScript.initialize()
        appState.desiredLockState = "locked"
        appState.retryCount = 0
        lockFixture.currentValue('lock') >> 'unlocked' // Lock did not respond

        when:
        appScript.retryIfCommandNotFollowed()

        then:
        // Should retry the lock command
        1 * lockFixture.lock()
        appState.retryCount == 1
    }

    void "retryIfCommandNotFollowed() stops retrying after 2 retries"() {
        given:
        appScript.installed()
        appScript.initialize()
        appState.desiredLockState = "locked"
        appState.retryCount = 2 // Already retried twice
        lockFixture.currentValue('lock') >> 'unlocked' // Still not responding

        when:
        appScript.retryIfCommandNotFollowed()

        then:
        // Should not retry again
        0 * lockFixture.lock()
        appState.retryCount == 2
    }

    void "retryIfCommandNotFollowed() retries unlock when unlock command was not followed"() {
        given:
        appScript.installed()
        appScript.initialize()
        appState.desiredLockState = "unlocked"
        appState.retryCount = 0
        lockFixture.currentValue('lock') >> 'locked' // Lock did not unlock

        when:
        appScript.retryIfCommandNotFollowed()

        then:
        // Should retry the unlock command
        1 * lockFixture.unlock()
        appState.retryCount == 1
    }
}
