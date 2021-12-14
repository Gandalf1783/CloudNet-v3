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

package eu.cloudnetservice.cloudnet.ext.report.config;

import com.google.common.base.Verify;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record ReportConfiguration(
  boolean saveRecords,
  boolean saveOnCrashOnly,
  @NotNull Path recordDestination,
  long serviceLifetime,
  @NotNull DateFormat dateFormat,
  @NotNull List<PasteService> pasteServers
) {

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ReportConfiguration configuration) {
    return builder()
      .saveRecords(configuration.saveRecords())
      .saveOnCrashOnly(configuration.saveOnCrashOnly())
      .recordDestination(configuration.recordDestination())
      .dateFormat(configuration.dateFormat())
      .pasteServers(configuration.pasteServers());
  }

  public static final class Builder {

    private boolean saveRecords = true;
    private boolean saveOnCrashOnly = true;
    private Path recordDestination = Path.of("records");
    private long serviceLifetime = 5000L;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private List<PasteService> pasteServers = List.of(new PasteService("default", "https://just-paste.it"));

    public @NotNull Builder saveRecords(boolean saveRecords) {
      this.saveRecords = saveRecords;
      return this;
    }

    public @NotNull Builder saveOnCrashOnly(boolean saveOnCrashOnly) {
      this.saveOnCrashOnly = saveOnCrashOnly;
      return this;
    }

    public @NotNull Builder recordDestination(@NotNull Path recordDestination) {
      this.recordDestination = recordDestination;
      return this;
    }

    public @NotNull Builder serviceLifetime(long serviceLifetime) {
      this.serviceLifetime = serviceLifetime;
      return this;
    }

    public @NotNull Builder dateFormat(@NotNull DateFormat dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public @NotNull Builder pasteServers(@NotNull List<PasteService> pasteServers) {
      this.pasteServers = List.copyOf(pasteServers);
      return this;
    }

    public @NotNull ReportConfiguration build() {
      Verify.verifyNotNull(this.recordDestination, "No recordDestination provided");
      Verify.verifyNotNull(this.dateFormat, "No dateFormat provided");
      Verify.verifyNotNull(this.pasteServers, "No pasteServers provided");

      return new ReportConfiguration(
        this.saveRecords,
        this.saveOnCrashOnly,
        this.recordDestination,
        this.serviceLifetime,
        this.dateFormat,
        this.pasteServers);
    }
  }
}
