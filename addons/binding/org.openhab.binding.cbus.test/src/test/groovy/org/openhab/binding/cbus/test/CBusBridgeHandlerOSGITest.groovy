package org.openhab.binding.cbus.test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import org.eclipse.smarthome.config.core.Configuration
import org.eclipse.smarthome.core.thing.Bridge
import org.eclipse.smarthome.core.thing.ManagedThingProvider
import org.eclipse.smarthome.core.thing.Thing
import org.eclipse.smarthome.core.thing.ThingProvider
import org.eclipse.smarthome.core.thing.ThingTypeUID
import org.eclipse.smarthome.core.thing.ThingUID
import org.eclipse.smarthome.core.thing.binding.ThingHandler
import org.eclipse.smarthome.test.OSGiTest
import org.eclipse.smarthome.test.storage.VolatileStorageService
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.openhab.binding.cbus.CBusBindingConstants
import org.openhab.binding.cbus.handler.CBusCGateHandler
import org.openhab.binding.cbus.handler.CBusLightHandler
import org.openhab.binding.cbus.handler.CBusNetworkHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CBusBridgeHandlerOSGITest extends OSGiTest {

    Logger logger = LoggerFactory.getLogger(this.getClass())

    final ThingTypeUID BRIDGE_THING_TYPE_UID = new ThingTypeUID(CBusBindingConstants.BINDING_ID, CBusBindingConstants.BRIDGE_CGATE)

    ManagedThingProvider managedThingProvider
    VolatileStorageService volatileStorageService = new VolatileStorageService()
    Bridge cGateBridge
    Bridge networkBridge50
    Thing group43


    @Before
    public void setUp() throws Exception {
        registerService(volatileStorageService)
        managedThingProvider = getService(ThingProvider, ManagedThingProvider)
        assertThat managedThingProvider, is(notNullValue())
    }

    @Test
    void CBusBridgeHandlerRegistered() {

        CBusCGateHandler cBusCGateHandler = getService(ThingHandler, CBusCGateHandler)
        assertThat cBusCGateHandler, is(nullValue())

        Configuration configuration = new Configuration().with {
            put(CBusBindingConstants.PROPERTY_IP_ADDRESS, "127.0.0.1")
            it
        }

        ThingUID cGateUID = new ThingUID(CBusBindingConstants.BRIDGE_TYPE_CGATE, "cGateTest");

        cGateBridge = managedThingProvider.createThing(
                CBusBindingConstants.BRIDGE_TYPE_CGATE, cGateUID, null, configuration)

        assertThat cGateBridge, is(notNullValue())

        // wait for MaxCubeBridgeHandler to be registered
        waitForAssert({
            cBusCGateHandler = getService(ThingHandler, CBusCGateHandler)
            assertThat cBusCGateHandler, is(notNullValue())
        },  10000)

        CBusNetworkHandler cBusNetworkHandler = getService(ThingHandler, CBusNetworkHandler)
        assertThat cBusNetworkHandler, is(nullValue())

        Configuration configuration2 = new Configuration().with {
            put(CBusBindingConstants.PROPERTY_NETWORK_ID, "50")
            put(CBusBindingConstants.PROPERTY_PROJECT, "EVCHURCH")
            it
        }

        ThingUID networkUID = new ThingUID(CBusBindingConstants.BRIDGE_TYPE_NETWORK, "cBusNetworkTest");

        networkBridge50 = managedThingProvider.createThing(
                CBusBindingConstants.BRIDGE_TYPE_NETWORK, networkUID, cGateBridge.getUID(), configuration2)

        assertThat networkBridge50, is(notNullValue())

        // wait for MaxCubeBridgeHandler to be registered
        waitForAssert({
            cBusNetworkHandler = getService(ThingHandler, CBusNetworkHandler)
            assertThat cBusNetworkHandler, is(notNullValue())
        },  10000)

        CBusLightHandler cBusLightHandler = getService(ThingHandler, CBusLightHandler)
        assertThat cBusLightHandler, is(nullValue())

        Configuration configuration3 = new Configuration().with {
            put(CBusBindingConstants.CONFIG_GROUP_ID, "100")
            it
        }

        ThingUID groupUID = new ThingUID(CBusBindingConstants.THING_TYPE_LIGHT, "group43");

        group43 = managedThingProvider.createThing(
                CBusBindingConstants.THING_TYPE_LIGHT, groupUID, networkUID, configuration3)

        assertThat group43, is(notNullValue())

        // wait for MaxCubeBridgeHandler to be registered
        waitForAssert({
            cBusLightHandler = getService(ThingHandler, CBusLightHandler)
            assertThat cBusLightHandler, is(notNullValue())
        },  10000)

        sleep(2000)
        logger.debug("Posting update")

        sleep(60000)


        ///////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////              CLEANUP            /////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////

        managedThingProvider.remove(group43.getUID())

        // wait for MaxCubeBridgeHandler to be unregistered
        waitForAssert({
            cBusLightHandler = getService(ThingHandler, CBusLightHandler)
            assertThat cBusLightHandler, is(nullValue())
        }, 10000)

        managedThingProvider.remove(networkBridge50.getUID())

        waitForAssert({
            cBusNetworkHandler = getService(ThingHandler, CBusNetworkHandler)
            assertThat cBusNetworkHandler, is(nullValue())
        }, 10000)

        managedThingProvider.remove(cGateBridge.getUID())

        // wait for MaxCubeBridgeHandler to be unregistered
        waitForAssert({
            cBusCGateHandler = getService(ThingHandler, CBusCGateHandler)
            assertThat cBusCGateHandler, is(nullValue())
        }, 10000)

    }


    @After
    void CleanUp() {
        if (group43 != null) {
            managedThingProvider.remove(group43.getUID())
        }
        // wait for MaxCubeBridgeHandler to be unregistered
        CBusLightHandler cBusLightHandler
        waitForAssert({
            cBusLightHandler = getService(ThingHandler, CBusLightHandler)
            assertThat cBusLightHandler, is(nullValue())
        }, 10000)


        if (networkBridge50 != null) {
            managedThingProvider.remove(networkBridge50.getUID())
        }
        // wait for MaxCubeBridgeHandler to be unregistered
        CBusNetworkHandler cBusNetworkHandler
        waitForAssert({
            cBusNetworkHandler = getService(ThingHandler, CBusNetworkHandler)
            assertThat cBusNetworkHandler, is(nullValue())
        }, 10000)
        if (cGateBridge != null) {
            managedThingProvider.remove(cGateBridge.getUID())
        }
        CBusCGateHandler cBusCGateHandler
        // wait for MaxCubeBridgeHandler to be unregistered
        waitForAssert({
            cBusCGateHandler = getService(ThingHandler, CBusCGateHandler)
            assertThat cBusCGateHandler, is(nullValue())
        }, 10000)
    }

    /*
     @Test
     public void test() {
     fail("Not yet implemented");
     }
     */
}
