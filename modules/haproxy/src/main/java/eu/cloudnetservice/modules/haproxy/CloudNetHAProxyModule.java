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

package eu.cloudnetservice.modules.haproxy;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.haproxy.listener.ServiceListener;
import eu.cloudnetservice.modules.haproxy.socket.HAProxySocketController;
import eu.cloudnetservice.node.command.CommandProvider;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;

@Singleton
public class CloudNetHAProxyModule extends DriverModule {
  private static final Logger LOGGER = LogManager.logger(CloudNetHAProxyModule.class);

  private final Runnable socketControllerRunnable = new HAProxySocketController();
  private final Thread socketControllerThread = new Thread(this.socketControllerRunnable);

  public static HashMap<String, ProxyInfo> proxyInfoHashMap = new HashMap<>();
  public static Set<String> proxiesWaitingForStop = new HashSet<>(); // List that allows no duplicates

  /**
   * Finishes the startup by registering the ServiceListener
   *
   * @param eventManager EventManager where events will be registered
   */
  @ModuleTask
  public void registerListeners(@NonNull EventManager eventManager) {
    eventManager.registerListener(ServiceListener.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void startSocketServer(@NonNull CommandProvider commandProvider) {
    // register the bridge command
    this.socketControllerThread.start();
  }


  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void stopThread() {
    this.socketControllerThread.interrupt();
  }

}
