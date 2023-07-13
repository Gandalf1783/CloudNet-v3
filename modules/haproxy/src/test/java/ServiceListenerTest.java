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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.haproxy.listener.ServiceListener;
import eu.cloudnetservice.node.event.service.CloudServicePreLifecycleEvent;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ServiceListenerTest {


  private CloudServicePreLifecycleEvent mockEvent;
  private ServiceInfoSnapshot mockSnapshot;
  private ServiceConfiguration mockServiceConfig;
  private ServiceListener serviceListener;

  @BeforeEach
  public void setup() {
    this.mockEvent = mock(CloudServicePreLifecycleEvent.class);
    this.mockSnapshot = mock(ServiceInfoSnapshot.class);
    this.mockServiceConfig = mock(ServiceConfiguration.class);

    this.serviceListener = new ServiceListener();
  }

  @Test
  void lifecycleChangeEvent_TestForCorrectGroups_EarlyReturn() {

    Set<String> groups = new HashSet<>();
    groups.add("Global");

    when(this.mockEvent.serviceInfo()).thenReturn(this.mockSnapshot);
    when(this.mockSnapshot.configuration()).thenReturn(this.mockServiceConfig);
    when(this.mockServiceConfig.groups()).thenReturn(groups);

    this.serviceListener.handleLifecycleChangeEvent(this.mockEvent);

    verify(this.mockEvent, times(1)).serviceInfo();
  }

  @Test
  void lifecycleChangeEvent_TestForCorrectGroups_NoReturn() {
    Set<String> groups = new HashSet<>();
    groups.add("Global");
    groups.add("Proxy");

    when(this.mockEvent.serviceInfo()).thenReturn(this.mockSnapshot);
    when(this.mockSnapshot.configuration()).thenReturn(this.mockServiceConfig);
    when(this.mockServiceConfig.groups()).thenReturn(groups);

    this.serviceListener.handleLifecycleChangeEvent(this.mockEvent);

    verify(this.mockEvent, atLeastOnce()).serviceInfo();
  }

}
