/*
 * Copyright 2019-2023 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.haproxy.listener;

import static eu.cloudnetservice.modules.haproxy.CloudNetHAProxyModule.proxiesWaitingForStop;
import static eu.cloudnetservice.modules.haproxy.CloudNetHAProxyModule.proxyInfoHashMap;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.haproxy.HAProxyState;
import eu.cloudnetservice.modules.haproxy.ProxyInfo;
import eu.cloudnetservice.node.event.service.CloudServicePreLifecycleEvent;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class ServiceListener {

  private static final Logger LOGGER = LogManager.logger(ServiceListener.class);


  /**
   * This Method processes the startup of a Proxy Service.<br>
   * It sets the ProxyState to {@link HAProxyState#UP}<br><br>
   *
   * This method does NOT cancel the event.<br>
   *
   * @param event Event to process. Must be passed by the listener.
   */
  private void processProxyStartEvent(@NonNull CloudServicePreLifecycleEvent event) {
    final String proxyName = event.serviceInfo().name();

    ProxyInfo info = proxyInfoHashMap.get(proxyName);

    if (info == null) { // If the ProxyInfoHashMap does not contain information for this proxy, create a new ProxyInfo
      info = new ProxyInfo();
      proxyInfoHashMap.put(proxyName, info); // Save it
    }

    info.state = HAProxyState.UP;


    LOGGER.info("Proxy \"" + proxyName + "\" has been registered for HAProxy and is now acceptin connections.");

    event.cancelled(false); // Make sure to not cancel the event
  }

  /**
   * This method processes the stop of a Proxy Service.<br><br>
   *
   * It checks if the Proxy is empty. If yes, the Proxy will be taken down directly (returns 1)<br>
   * Alternatively, it will check if a Proxy has been drained previously and is now stopping (returns 2)<br><br>
   *
   * @param event Event to process. Must be passed by the listener.
   * @return Returns the action that was taken as an int.<br>
   * (1) => Proxy had no online players, stopped immediately<br>
   * (2) => Proxy was drained properly and is in the ProxyState = DOWN. It will also be stopped.<br>
   * (3) => Proxy has either players online or is still in the ProxyState = UP. It won't be stopped, but drained until no
   * Players are connected. <br>
   */
  private int processProxyStopEvent(@NonNull CloudServicePreLifecycleEvent event) {
    final String proxyName = event.serviceInfo().name();

    // Is the Proxy already waiting for stop?
    boolean isWaitingForStop = proxiesWaitingForStop.contains(proxyName);
    ProxyInfo info = proxyInfoHashMap.get(proxyName);
    ServiceLifeCycle targetLifecycle = event.targetLifecycle();

    //Test if Proxy has no online Players
    if (event.serviceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT) == 0) {
      info.state = HAProxyState.DOWN; // Mark Proxy as DOWN
      event.cancelled(false); // Let CloudNet stop the proxy
      return 1;
    }

    // If the Proxy is already waiting for stop and is Down, dont cancel!
    // This is a safe state with No Connections to the Proxy and can therefore be shutdown.
    if (isWaitingForStop && info.state == HAProxyState.DOWN) {
      event.cancelled(false);
      proxiesWaitingForStop.remove(proxyName); // Remove it because service is no longer waiting for stop
      return 2;
    }

    // From here, the Proxy will not be able to stop directly. It most likely still has players on it.
    // Therefore, we set the Draining State for HAProxy to block new connections to this Proxy.

    event.cancelled(true); // Do not allow CloudNet to stop Service yet
    info.state = HAProxyState.DRAINING; // Draining via HAProxy
    proxiesWaitingForStop.add(event.serviceInfo().name()); // Proxy is waiting for Stop (Checked for connected players = 0 for sending stop)

    LOGGER.info("Proxy " + proxyName + " wants to reach state " + targetLifecycle.name() + ". Draining Service via HAProxy.");

    return 3;
  }


  /**
   * This method retrieves a {@link CloudServicePreLifecycleEvent} and checks for a proxy startup or shutdown.<br><br>
   *
   * This method will cancel an event if necessary. <br><br>
   *
   * @param event Event to process. Passed by CloudNet.
   */
  @EventListener
  public void handleLifecycleChangeEvent(@NonNull CloudServicePreLifecycleEvent event) {

    if (!event.serviceInfo().configuration().groups().contains("Proxy")) { // Check if Service is a Proxy
      return; //If is not a Proxy, return and ignore event
    }

    ServiceLifeCycle currentLifeCycle = event.serviceInfo().lifeCycle();
    ServiceLifeCycle targetLifeCycle = event.targetLifecycle();

    // The Service is stopping or Restarting if the targetLifeCycle is DELETED, PREPARED or STOPPED
    boolean isNextStateStopOrRestart = (targetLifeCycle == ServiceLifeCycle.DELETED
                                        || targetLifeCycle == ServiceLifeCycle.PREPARED
                                        || targetLifeCycle == ServiceLifeCycle.STOPPED);


    // Start Event here
    // If the current Lifecycle is PREPARED and changing to RUNNING, process the Proxy Start
    if (currentLifeCycle == ServiceLifeCycle.PREPARED && targetLifeCycle == ServiceLifeCycle.RUNNING) {
      this.processProxyStartEvent(event);
      return;
    }

    //Stop Event here
    // If the current Lifecycle is RUNNING and changing to DELETED, PREPARED or STOPPED, process the Proxy Stop
    if (currentLifeCycle == ServiceLifeCycle.RUNNING  && isNextStateStopOrRestart) {
      this.processProxyStopEvent(event);
      return;
    }
  }

  /**
   * This method handles a Player Disconnect and checks if any Proxy is waiting for a stop.<br>
   * If a proxy is currently waiting for stop and has no more players, it will stop the Proxy.<br>
   *
   * @param event Event to process. Passed by CloudNet.
   */
  @EventListener
  public void handleProxyPlayerDisconnect(CloudServiceUpdateEvent event) {
    final String proxyName = event.serviceInfo().name();
    boolean isWaitingForStop = proxiesWaitingForStop.contains(proxyName);
    
    if (!isWaitingForStop) { // Proxy does not wait for stop, ignore the event.
      return;
    }

    ProxyInfo info = proxyInfoHashMap.get(event.serviceInfo().name());


    // Get player count
    int players = event.serviceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT);


    // No more players online, stop the Service
    if (players == 0) {
      LOGGER.info("Proxy " + proxyName + " has been drained and is now stopping.");

      info.state = HAProxyState.DOWN;

      // Let CloudNet stop the Service. This will trigger the CloudServicePreLifecycleEvent.
      event.serviceInfo().provider().stop();
    }
  }

}
