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

import scala.collection.immutable.HashSet

// scalastyle:off multiple.string.literals magic.number

class IpRangeTest extends UnitSpec {

  private val addr1 = "192.168.1.200"
  private val addr2 = "192.168.1.230"
  private val range = IpRange(addr1, addr2)
  private val range2 = IpRange("192.168.1.210", "192.168.1.220")
  private val range3 = IpRange("192.168.1.100", "192.168.1.210")
  private val range4 = IpRange("192.168.1.220", "192.168.1.240")

  "Creating an IpRange" should "result in failure if addresses are invalid" in {
    // first address invalid
    an[IpaddrException] should be thrownBy IpRange("1.2.300.20", "1.2.3.2")

    // second address invalid
    an[IpaddrException] should be thrownBy IpRange("192.168.1.200", "192.168.1.256")

    // first address > second address
    an[IpaddrException] should be thrownBy IpRange("192.168.1.230", "192.168.1.229")
  }

  it should "succeed if addresses are valid" in {
    IpRange("10.2.10.12", "10.2.10.15") shouldBe a[IpRange]
    IpRange("10.2.10.230", "10.2.10.230") shouldBe a[IpRange]
  }

  "An IpRange object" should "perform all range operations" in {
    range.toString() should be(addr1 + "-" + addr2)
    range.first should be(3232235976L)
    range.last should be(3232236006L)
    range.key should be((4, 3232235976L, 3232236006L))
    range.sortKey should be((4, 3232235976L, 27))
  }

  it should "perform contains operation" in {
    // Check range edge addresses
    range.contains(addr1) should be(true)
    range.contains(addr2) should be(true)
    range.contains(range2) should be(true)
    range.contains(range3) should be(false)
    range.contains(range4) should be(false)

    val net = IpNetwork("10.4.10.100/30")
    val rightRange = "10.4.10.105"
    val r1 = IpRange("10.4.10.101", "10.4.10.102")
    val r2 = IpRange("10.4.10.99", rightRange)
    val r3 = IpRange("10.4.10.100", rightRange)
    val r4 = IpRange("10.4.10.101", rightRange)
    r1.contains(net) should be(false)
    r2.contains(net) should be(true)
    r3.contains(net) should be(true)
    r4.contains(net) should be(false)
    an[IpaddrException] should be thrownBy r4.contains("1.2.3") // address is bad
  }

  it should "perform cidrs operation" in {
    val net1 = IpNetwork("192.168.1.200/29")
    val net2 = IpNetwork("192.168.1.208/28")
    val net3 = IpNetwork("192.168.1.224/30")
    val net4 = IpNetwork("192.168.1.228/31")
    val net5 = IpNetwork("192.168.1.230/32")
    val netList = List(net1, net2, net3, net4, net5)
    range.cidrs should be(netList)
  }

  it should "check for equality" in {
    val hs = HashSet(range2, range, range3)
    range should be(IpRange(addr1, addr2))
    range == range2 should be(false)
    range.equals(range) should be(true)
    range.equals(addr1) should be(false)
    hs.contains(range) should be(true)
    hs.contains(range4) should be(false)
  }

}
