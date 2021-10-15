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

package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostInstallDependencyEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostLoadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostReloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostStartEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostStopEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePostUnloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreInstallDependencyEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreLoadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreReloadEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreStartEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreStopEvent;
import de.dytanic.cloudnet.driver.event.events.module.ModulePreUnloadEvent;
import org.jetbrains.annotations.NotNull;

public class DefaultModuleProviderHandler implements IModuleProviderHandler {

  private static final Logger LOGGER = LogManager.getLogger(DefaultModuleProviderHandler.class);

  @Override
  public boolean handlePreModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
    boolean cancelled = this.callEvent(new ModulePreLoadEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        LanguageManager.getMessage("cloudnet-pre-load-module"),
        this.getModuleProvider(),
        moduleWrapper.getModuleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleLoad(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostLoadEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      LanguageManager.getMessage("cloudnet-post-load-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }

  @Override
  public boolean handlePreModuleStart(@NotNull IModuleWrapper moduleWrapper) {
    boolean cancelled = this.callEvent(new ModulePreStartEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        LanguageManager.getMessage("cloudnet-pre-start-module"),
        this.getModuleProvider(),
        moduleWrapper.getModuleConfiguration()));
      CloudNetDriver.getInstance().getEventManager().registerListener(moduleWrapper.getModule());
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleStart(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostStartEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      LanguageManager.getMessage("cloudnet-post-start-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }

  @Override
  public boolean handlePreModuleReload(@NotNull IModuleWrapper moduleWrapper) {
    boolean cancelled = this.callEvent(new ModulePreReloadEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        LanguageManager.getMessage("cloudnet-pre-reload-module"),
        this.getModuleProvider(),
        moduleWrapper.getModuleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleReload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostReloadEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      LanguageManager.getMessage("cloudnet-post-reload-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }


  @Override
  public boolean handlePreModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    boolean cancelled = this.callEvent(new ModulePreStopEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
    if (!cancelled) {
      LOGGER.info(this.replaceAll(
        LanguageManager.getMessage("cloudnet-pre-stop-module"),
        this.getModuleProvider(),
        moduleWrapper.getModuleConfiguration()));
    }

    return !cancelled;
  }

  @Override
  public void handlePostModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    CloudNetDriver.getInstance().getServicesRegistry().unregisterAll(moduleWrapper.getClassLoader());
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(moduleWrapper.getClassLoader());

    this.callEvent(new ModulePostStopEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      LanguageManager.getMessage("cloudnet-post-stop-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }

  @Override
  public void handlePreModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePreUnloadEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.info(this.replaceAll(
      LanguageManager.getMessage("cloudnet-pre-unload-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }

  @Override
  public void handlePostModuleUnload(@NotNull IModuleWrapper moduleWrapper) {
    this.callEvent(new ModulePostUnloadEvent(this.getModuleProvider(), moduleWrapper));
    LOGGER.fine(this.replaceAll(
      LanguageManager.getMessage("cloudnet-post-unload-module"),
      this.getModuleProvider(),
      moduleWrapper.getModuleConfiguration()));
  }

  @Override
  public void handlePreInstallDependency(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePreInstallDependencyEvent(this.getModuleProvider(), configuration, dependency));
    LOGGER.fine(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-install-dependency-module")
        .replace("%group%", dependency.getGroup())
        .replace("%name%", dependency.getName())
        .replace("%version%", dependency.getVersion()),
      this.getModuleProvider(), configuration));
  }

  @Override
  public void handlePostInstallDependency(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) {
    this.callEvent(new ModulePostInstallDependencyEvent(this.getModuleProvider(), configuration, dependency));
    LOGGER.fine(this.replaceAll(LanguageManager.getMessage("cloudnet-post-install-dependency-module")
        .replace("%group%", dependency.getGroup())
        .replace("%name%", dependency.getName())
        .replace("%version%", dependency.getVersion()),
      this.getModuleProvider(), configuration));
  }

  protected IModuleProvider getModuleProvider() {
    return CloudNetDriver.getInstance().getModuleProvider();
  }

  protected @NotNull <T extends Event> T callEvent(@NotNull T event) {
    return CloudNetDriver.getInstance().getEventManager().callEvent(event);
  }

  protected String replaceAll(String text, IModuleProvider moduleProvider, ModuleConfiguration configuration) {
    Preconditions.checkNotNull(text);
    Preconditions.checkNotNull(moduleProvider);
    Preconditions.checkNotNull(configuration);

    return text.replace("%module_group%", configuration.getGroup())
      .replace("%module_name%", configuration.getName())
      .replace("%module_version%", configuration.getVersion())
      .replace("%module_author%", configuration.getAuthor() == null ? "" : configuration.getAuthor());
  }
}