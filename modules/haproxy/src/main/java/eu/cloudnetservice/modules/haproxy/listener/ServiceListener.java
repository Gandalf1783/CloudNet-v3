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

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.haproxy.CloudNetHAProxyModule;
import eu.cloudnetservice.modules.haproxy.ProxyInfo;
import eu.cloudnetservice.modules.haproxy.ProxyState;
import eu.cloudnetservice.node.event.service.CloudServicePreLifecycleEvent;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class ServiceListener {

  private static final Logger LOGGER = LogManager.logger(ServiceListener.class);


  @EventListener
  public void handleLifecycleChangeEvent(@NonNull CloudServicePreLifecycleEvent event) {

    if (!event.serviceInfo().configuration().groups().contains("Proxy")) {
      return;
    }

    //Start Event here
    if (event.serviceInfo().lifeCycle() == ServiceLifeCycle.PREPARED && event.targetLifecycle() == ServiceLifeCycle.RUNNING) {
      ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(event.serviceInfo().name());

      if (info == null) {
        info = new ProxyInfo();
      }

      info.state = ProxyState.UP;

      CloudNetHAProxyModule.proxyInfoHashMap.put(event.serviceInfo().name(), info);


      LOGGER.info("Proxy \"" + event.serviceInfo().name() + "\" has been registered for HAProxy and is now UP/READY.");
      event.cancelled(false);
      return;
    }

    //Stop Event here
    if (!(event.serviceInfo().lifeCycle() == ServiceLifeCycle.RUNNING
      &&
      (event.targetLifecycle() == ServiceLifeCycle.DELETED
        || event.targetLifecycle() == ServiceLifeCycle.PREPARED
        || event.targetLifecycle() == ServiceLifeCycle.STOPPED
      )
    )) {
      return;
    }

    boolean isWaitingForStop = proxiesWaitingForStop.contains(event.serviceInfo().name());
    ProxyState currentProxyState = CloudNetHAProxyModule.proxyInfoHashMap.get(event.serviceInfo().name()).state;

    if (isWaitingForStop && currentProxyState == ProxyState.DOWN) { // If the Proxy is already waiting for stop and is Down, dont cancel!
      event.cancelled(false);
      proxiesWaitingForStop.remove(event.serviceInfo().name()); // Remove it because service is no longer waiting for stop
      return;
    }

    if(event.serviceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT) == 0) {
      ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(event.serviceInfo().name());
      info.state = ProxyState.DOWN;
      event.cancelled(false);
      return;
    }

    event.cancelled(true);
    proxiesWaitingForStop.add(event.serviceInfo().name()); // Proxy is waiting for Stop (Checked for connected players = 0 for sending stop)

    LOGGER.info("HAProxy Lifecycle Change Event for Proxy "
      + event.serviceInfo().name() + " which wants to change to "
      + event.targetLifecycle().name());

    LOGGER.warning("Draining Service \"" + event.serviceInfo().name() + "\"");

    ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(event.serviceInfo().name());

    if (info == null) {
      info = new ProxyInfo();
    }

    info.state = ProxyState.DRAINING;

    CloudNetHAProxyModule.proxyInfoHashMap.put(event.serviceInfo().name(), info);

  }

  @EventListener
  public void handleProxyPlayerDisconnect(CloudServiceUpdateEvent event) {
    ServiceInfoSnapshot loginServiceInfo = event.serviceInfo();

    if (!proxiesWaitingForStop.contains(loginServiceInfo.serviceId().name())) { // Proxy does not wait for stop, return!
      LOGGER.info(event.serviceInfo().name() + " does not wait to stop!");
      return;
    }

    int players = event.serviceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT);



    if (players == 0) {
      LOGGER.info(loginServiceInfo.name() + " is drained! Setting state to down and finally stopping service");

      ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(event.serviceInfo().name());
      info.state = ProxyState.DOWN;
      CloudNetHAProxyModule.proxyInfoHashMap.put(event.serviceInfo().name(), info);

      event.serviceInfo().provider().stop();
    }
  }

}
