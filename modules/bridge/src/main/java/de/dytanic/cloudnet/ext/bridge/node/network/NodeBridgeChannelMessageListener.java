/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.bridge.node.network;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.node.NodeBridgeManagement;
import org.jetbrains.annotations.NotNull;

public final class NodeBridgeChannelMessageListener {

  private final IEventManager eventManager;
  private final NodeBridgeManagement management;

  public NodeBridgeChannelMessageListener(
    @NotNull NodeBridgeManagement management,
    @NotNull IEventManager eventManager
  ) {
    this.management = management;
    this.eventManager = eventManager;
  }

  @EventListener
  public void handle(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(BridgeManagement.BRIDGE_CHANNEL_NAME)
      && event.getMessage() != null
      && event.getMessage().equals("update_bridge_configuration")) {
      // read the config
      BridgeConfiguration configuration = event.getContent().readObject(BridgeConfiguration.class);
      // set the configuration
      this.management.setConfigurationSilently(configuration);
      // call the update event
      this.eventManager.callEvent(new BridgeConfigurationUpdateEvent(configuration));
    }
  }
}