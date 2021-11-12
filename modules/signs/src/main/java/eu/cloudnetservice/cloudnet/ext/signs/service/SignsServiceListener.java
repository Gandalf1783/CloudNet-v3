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

package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import org.jetbrains.annotations.NotNull;

public class SignsServiceListener {

  protected final ServiceSignManagement<?> signManagement;

  public SignsServiceListener(ServiceSignManagement<?> signManagement) {
    this.signManagement = signManagement;
  }

  @EventListener
  public void handle(@NotNull CloudServiceUpdateEvent event) {
    this.signManagement.handleServiceUpdate(event.getServiceInfo());
  }

  @EventListener
  public void handle(@NotNull CloudServiceLifecycleChangeEvent event) {
    switch (event.getNewLifeCycle()) {
      case STOPPED:
      case DELETED:
        this.signManagement.handleServiceRemove(event.getServiceInfo());
        break;
      case RUNNING:
        this.signManagement.handleServiceAdd(event.getServiceInfo());
        break;
      default:
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
        break;
    }
  }
}
