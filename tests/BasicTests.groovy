package joelwetzel.reliable_locks.tests

import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Basic tests for ReliableLocksInstance.groovy
*/
class BasicTests extends IntegrationAppSpecification {
    def lockFixture = LockFixtureFactory.create('testLock')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "ReliableLocksInstance.groovy",
                                    validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
                                    userSettingValues: [wrappedLock: lockFixture, refreshTime: 3, autoRefreshOption: "none", retryLockCommands: false, notificationDevice: null, enableDebugLogging: true])
    }

    void "installed() logs the settings"() {
        when:
        appScript.installed()

        then:
        1 * log.info({ it.contains('Installed with settings:') })
    }

    void "initialize() subscribes to lock and battery events"() {
        when:
        appScript.installed()
        appScript.initialize()

        then:
        1 * appExecutor.subscribe(lockFixture, 'lock', 'wrappedLockHandler')
        1 * appExecutor.subscribe(lockFixture, 'battery', 'batteryHandler')
    }

    void "lockWrappedLock() sets desiredLockState and calls lock"() {
        given:
        appScript.installed()
        appScript.initialize()

        when:
        appScript.lockWrappedLock()

        then:
        1 * lockFixture.lock()
        appState.desiredLockState == "locked"
        appState.retryCount == 0
    }

    void "unlockWrappedLock() sets desiredLockState and calls unlock"() {
        given:
        appScript.installed()
        appScript.initialize()

        when:
        appScript.unlockWrappedLock()

        then:
        1 * lockFixture.unlock()
        appState.desiredLockState == "unlocked"
        appState.retryCount == 0
    }

    void "wrappedLockHandler() updates reliableLock state when wrapped lock is locked"() {
        given:
        appScript.installed()
        appScript.initialize()
        lockFixture.currentValue('lock') >> 'locked'

        when:
        appScript.wrappedLockHandler(null)

        then:
        appState.desiredLockState == "locked"
    }

    void "wrappedLockHandler() updates reliableLock state when wrapped lock is unlocked"() {
        given:
        appScript.installed()
        appScript.initialize()
        lockFixture.currentValue('lock') >> 'unlocked'

        when:
        appScript.wrappedLockHandler(null)

        then:
        appState.desiredLockState == "unlocked"
    }
}
