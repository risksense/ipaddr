/**
  * Copyright 2017 RiskSense, Inc.
  * This file is part of ipaddr library.
  *
  * Ipaddr is free software licensed under the Apache License, Version 2.0 (the "License"); you
  * may not use this file except in compliance with the License. You may obtain a copy of the
  * License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the
  * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  * express or implied. See the License for the specific language governing permissions and
  * limitations under the License.
  */

package com.risksense.whitelist

import com.risksense.ipaddr.IpAddress
import com.risksense.ipaddr.IpaddrException
import com.risksense.ipaddr.IpGlob
import com.risksense.ipaddr.IpNetwork
import com.risksense.ipaddr.IpRange
import com.risksense.ipaddr.IpSet

/**
  * Utility for checking whitelist inclusion as well as IP/CIDR string
  * representation validity.
  */
object Whitelist {
  def isAllowed (ipInfo: IpAddress, whitelist: IpSet): Boolean =
    whitelist.contains(ipInfo)

  def isAllowed (ipInfo: IpNetwork, whitelist: IpSet): Boolean =
    whitelist.contains(ipInfo)

  def isAllowed (ipInfo: IpRange, whitelist: IpSet): Boolean = {
    ipInfo.cidrs.foldLeft(true) {
      (res, net) => {
        res && whitelist.contains(net)
      }
    }
  }

  def isAllowed (ipGlob: String, whitelist: IpSet): Boolean = {
    ipGlob match {
      case _ if (ipGlob.isEmpty) => false
      case _ if (!IpGlob.validGlob(ipGlob)) => false
      case _ => isAllowed(IpGlob.globToIpRange(ipGlob), whitelist)
    }
  }

  def isValidIPSyntax(ipDescriptor: String): Boolean = {
    ipDescriptor match {
      case _ if (ipDescriptor.isEmpty) => false
      case _ if (ipDescriptor.contains("-") || ipDescriptor.contains("*")) =>
        IpGlob.validGlob(ipDescriptor)
      case _ if (!ipDescriptor.contains('/')) => {
        try {
          IpAddress(ipDescriptor)
          true
        }
        catch {
          case e: IpaddrException => false
        }
      }
      case _ if (ipDescriptor.contains('/')) => {
        try {
          IpNetwork(ipDescriptor)
          true
        }
        catch {
          case e: IpaddrException => false
        }
      }
      case _ => false
    }
  }
}

abstract class Whitelist {
  /**
    * Check given IpAddress for inclusion in given IpSet (whitelist)
    */
  def isAllowed(ipInfo: IpAddress, whitelist: IpSet): Boolean

  /**
    * Check given IpNetwork for inclusion in given IpSet (whitelist)
    */
  def isAllowed(ipInfo: IpNetwork, whitelist: IpSet): Boolean

  /**
    * Check given IpRange for inclusion in given IpSet (whitelist)
    * Note: true only if every IP in range is within whitelist.
    */
  def isAllowed(ipInfo: IpRange, whitelist: IpSet): Boolean

  /**
    * Check given IpGlob string rep. for inclusion in given IpSet (whitelist)
    */
  def isAllowed(ipInfo: String, whitelist: IpSet): Boolean

  /**
    * Check the validity of a given string in relation to valid IP/CIDR/Glob.
    */
  def isValidIPSyntax(ipDescriptor: String): Boolean
}
