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

package eu.cloudnetservice.modules.haproxy.socket;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.modules.haproxy.CloudNetHAProxyModule;
import eu.cloudnetservice.modules.haproxy.ProxyInfo;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * This class reqplies to HAProxy Agent-Check Requests to control the Proxy's mode in HAProxy, e.g. DRAIN, DOWN, UP etc.
 */
@Singleton
public class HAProxySocketController implements Runnable {

  private static final Logger LOGGER = LogManager.logger(HAProxySocketController.class);
  private static boolean shouldStop = false;

  private ServerSocket serverSocket;

  private int tcpPort = 7331;

  public void openSocket() {
    try {
      this.serverSocket = new ServerSocket(this.tcpPort);
      LOGGER.info("Running HAProxy Agent-Check on TCP " + this.tcpPort);
    } catch (IOException e) {
      LOGGER.severe("HAProxy: Could not open ServerSocket at " + this.tcpPort, e);
    }
  }



  @Override
  public void run() {
    this.openSocket(); // Open the Server Socket

    if (this.serverSocket.isClosed()) {
      LOGGER.info("Cannot serve. Exiting Thread.");
      return;
    }

    while (!shouldStop) {
      try {
        Socket socket = this.serverSocket.accept();

        byte[] buffer = new byte[1024]; // Buffer to read the "Agent-Send" Option
        int read;
        String proxyName = "";

        ProxyInfo info;

        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        PrintWriter writer = new PrintWriter(output, true); // Auto-Flush the writer


        //Read "Agent-Send" String from HAProxy
        while ((read = input.read(buffer)) != -1) {
          proxyName = new String(buffer, 0, read);
          break;
        }

        LOGGER.fine("Searching for \"" + proxyName + "'s\" status");

        info = CloudNetHAProxyModule.proxyInfoHashMap.get(proxyName); // Retrieve ProxyInfo by Name


        // If Proxy is not known, assume it does not acceppt any connections
        if (info == null) {
          LOGGER.warning("HAProxy Agent-Send is not known (" + proxyName + ")!");
          writer.write("down\r\n");
          writer.flush();
          writer.close();
          continue;
        }

        LOGGER.fine(proxyName + " state: " + info.maxPlayers + " connections / " + info.state.toString());

        //Respond with current proxy state
        writer.print("maxconn:" + info.maxPlayers + " " + info.state.toString() + "\r\n");
        writer.flush();
        socket.close();

      } catch (IOException e) {
        LOGGER.warning("HAProxy: Could not accept server socket", e);
      }
    }
  }

}
