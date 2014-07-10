package com.kixeye.chassis.support.util;

/*
 * #%L
 * Chassis Support
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.netflix.config.ConfigurationManager;

/**
 * Random networking utils.
 * 
 * @author ebahtijaragic
 */
public final class NetworkingUtils {

	private NetworkingUtils() {}
	
	/***
     * From Stack Overflow (http://stackoverflow.com/a/14364233).
     *
     * @return one of the computer's IP addresses
     */
    public static String getIpAddress() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip;
    }

    /***
     * Return Amazon instance id if available, otherwise IP address with
     * periods replaced with underscores to be Graphite friendly.
     *
     * @return application identifier
     */
    public static String getApplicationIdentifier() {
        // Bootstrap sets "aws.instance.id" if available
        String id = ConfigurationManager.getConfigInstance().getString("aws.instance.id",null);

        // Fallback to the IP address
        if (id == null) {
            id = getIpAddress();
        }
        return id.replace('.', '_');
    }
}
