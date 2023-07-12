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

package eu.cloudnetservice.modules.haproxy.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.modules.haproxy.CloudNetHAProxyModule;
import eu.cloudnetservice.modules.haproxy.ProxyInfo;
import eu.cloudnetservice.modules.haproxy.ProxyState;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import lombok.NonNull;


@Singleton
@CommandPermission("cloudnet.command.haproxy")
@Description("module-docker-command-description")
public class HAProxyCommand {

  private static final Logger LOGGER = LogManager.logger(HAProxyCommand.class);

  @CommandMethod("haproxy <proxy> drain")
  public void drain(@NonNull CommandSource source, @Argument("proxy") String proxy) {
    LOGGER.info("Setze auf drain!");

    ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(proxy);

    if (info == null) {
      info = new ProxyInfo();
    }

    info.state = ProxyState.DRAINING;

    CloudNetHAProxyModule.proxyInfoHashMap.put(proxy, info);

  }


  @CommandMethod("haproxy <proxy> up")
  public void up(@NonNull CommandSource source, @Argument("proxy") String proxy) {
    LOGGER.info("Setze auf up & ready!");

    ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(proxy);

    if (info == null) {
      info = new ProxyInfo();
    }

    info.state = ProxyState.UP;

    CloudNetHAProxyModule.proxyInfoHashMap.put(proxy, info);

  }


  @CommandMethod("haproxy <proxy> down")
  public void down(@NonNull CommandSource source, @Argument("proxy") String proxy) {
    LOGGER.info("Setze auf down!");

    ProxyInfo info = CloudNetHAProxyModule.proxyInfoHashMap.get(proxy);

    if (info == null) {
      info = new ProxyInfo();
    }

    info.state = ProxyState.DOWN;

    CloudNetHAProxyModule.proxyInfoHashMap.put(proxy, info);

  }



}
