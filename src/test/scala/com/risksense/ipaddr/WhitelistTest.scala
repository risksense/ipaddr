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

package com.risksense.ipaddr

import com.risksense.whitelist.Whitelist

// scalastyle:off multiple.string.literals magic.number

class WhitelistTest extends UnitSpec {
  private val whitelist = IpSet(IpRange("5.5.5.5", "5.5.5.10"))

  private val validGlobList = List(
    "5.5.5.*",
    "5.5.*.*",
    "5.*.*.*",
    "*.*.*.*",
    "5.5.5.5-10",
    "5.5.5-10.*",
    "5.5-10.*.*")

  private val invalidGlobList = List(
    "5.5.*.5-10",
    "5.*.5-10.5",
    "5.5.5.6-5")

  private val octet = List(0, 255)
  private val validIPs = for { i <- octet; j <- octet; k <- octet; l <- octet } yield {
    List(i, j, k, l).mkString(".")
  }
  private val outOfRangeIPs = validIPs.filter((e) => !whitelist.contains(IpAddress(e)))

  "Valid IP/CIDR/Glob" should "be contained in whitelist (5.5.5.5-10)" in {
    Whitelist.isAllowed(IpAddress("5.5.5.5"), whitelist) should be(true)
    Whitelist.isAllowed(IpAddress("5.5.5.6"), whitelist) should be(true)
    Whitelist.isAllowed(IpAddress("5.5.5.7"), whitelist) should be(true)
    Whitelist.isAllowed(IpAddress("5.5.5.8"), whitelist) should be(true)
    Whitelist.isAllowed(IpAddress("5.5.5.9"), whitelist) should be(true)
    Whitelist.isAllowed(IpAddress("5.5.5.10"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.5"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.6"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.7"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.8"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.9"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.10"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.5/32"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.6/31"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.8/31"), whitelist) should be(true)
    Whitelist.isAllowed(IpNetwork("5.5.5.10/32"), whitelist) should be(true)
    Whitelist.isAllowed(IpRange("5.5.5.5", "5.5.5.10"), whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.5", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.6", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.7", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.8", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.9", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.10", whitelist) should be(true)
    Whitelist.isAllowed("5.5.5.5-10", whitelist) should be(true)
  }

  "Valid IP/CIDR/Glob out of range" should "not be in whitelist" in {
    for ( validNotInRangeIp <- outOfRangeIPs ) {
      Whitelist.isAllowed(IpAddress(validNotInRangeIp), whitelist) should be(false)
      Whitelist.isAllowed(IpNetwork(validNotInRangeIp), whitelist) should be(false)
    }
  }

  "Correctly constructed IP/CIDR/Glob" should "be pass validation" in {
    //valid ip
    for (validIp <- validIPs) {
      Whitelist.isValidIPSyntax(validIp) should be(true)
    }

    //valid cidr
    val cidr = List(0, 32)
    for (validCIDR <- cidr) {
      Whitelist.isValidIPSyntax("5.5.5.5/" + validCIDR.toString) should be(true)
    }

    //valid glob
    for (glob <- validGlobList) {
      Whitelist.isValidIPSyntax(glob) should be(true)
    }
  }

  "Incorrectly constructed IP/CIDR/Glob" should "not pass validation" in {
    //bad ip
    Whitelist.isValidIPSyntax("192.0.0") should be(false)
    Whitelist.isValidIPSyntax("") should be(false)

    //bad cidr
    for (validIp <- validIPs) {
      Whitelist.isValidIPSyntax(validIp + "/33") should be(false)
    }

    //bad glob
    for (glob <- invalidGlobList) {
      Whitelist.isValidIPSyntax(glob) should be(false)
    }
  }
}
