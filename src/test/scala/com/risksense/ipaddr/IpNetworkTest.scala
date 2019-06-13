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

// scalastyle:off multiple.string.literals magic.number

class IpNetworkTest extends UnitSpec {

  private val net1 = IpNetwork("192.168.1.2", 24)
  private val net2 = IpNetwork("192.168.1.2/255.255.255.0")
  private val net3 = IpNetwork("10.2.10.230/255.255.255.0")
  private val net4 = IpNetwork(3232235778L, 32)
  private val net5 = IpNetwork(net1.toString)
  private val net6 = IpNetwork("0.0.0.0/0")

  private val netAddr = "192.168.1.0"
  private val netAddr2 = "192.168.1.255"
  private val netAddr3 = "192.168.2.0"
  private val netAddr4 = "192.168.0.255"
  private val range = IpRange(netAddr, netAddr2)
  private val range2 = IpRange(netAddr, netAddr3)
  private val range3 = IpRange(netAddr4, netAddr2)

  "Creating an IpNetwork " should "result in IpaddrException if address is invalid" in {
    an[IpaddrException] should be thrownBy IpNetwork("192.168.256/24")
    an[IpaddrException] should be thrownBy IpNetwork("192.168.256/256.255.255.0")
  }

  it should "create IpNetwork object if address is valid" in {
    IpNetwork("10.2.1.0/24") shouldBe a[IpNetwork]
    IpNetwork("10.2.1.0/255.255.255.0") shouldBe a[IpNetwork]
  }

  "Network" should "perform all network based operations correctly" in {
    val net1Broadcast = IpAddress("192.168.1.255")
    val net1Hostmask = IpAddress("0.0.0.255")
    val net1Ip = IpAddress("192.168.1.2")
    val net1IpAddr = IpAddress("192.168.1.0")
    val net1Netmask = IpAddress("255.255.255.0")
    net1.key should be((4, 3232235776L, 3232236031L))
    net1.sortKey should be((4, 3232235776L, 23, 2))
    net1.broadcast should be(net1Broadcast)
    net1.cidr should be(net2)
    net1.hostmask should be(net1Hostmask)
    net1.ip should be(net1Ip)
    net1.netmask should be(net1Netmask)
    net1.size should be(256)
    net1.first should be(3232235776L)
    net1.last should be(3232236031L)
    net1.network should be(net1IpAddr)
    net6.size should be(4294967296L)
  }

  it should "perform supernet operation" in {
    net1.supernet(22).size should be(2)
    net1.supernet(33) should be(Nil)
  }

  it should "perform subnet operation" in {
    net1.subnet(26).size should be(4) // scalastyle:ignore
    net1.subnet(26).size should be(4)
    net1.subnet(33) should be(Nil)

    val largeAmountOfSubnets = net6.subnet(17, 100000)

    largeAmountOfSubnets.size should be (100000)
    largeAmountOfSubnets.forall(s => s.mask == 17) should be(true)

  }

  it should "perform next operation" in {
    val net1Next = IpNetwork("192.168.2.0/24")
    net1.next() == net1Next should be(true)
    val maxNet = IpNetwork("255.255.255.255/30")
    an[IpaddrException] should be thrownBy maxNet.next()
  }

  it should "perform previous operation" in {
    val net1Previous = IpNetwork("192.168.0.0/24")
    net1.previous() == net1Previous should be(true)
    val minNet = IpNetwork("0.0.0.0/24")
    an[IpaddrException] should be thrownBy minNet.previous()
  }

  it should "perform all comparison operations" in {
    (net1 == net2) should be(true)
    (net1 == net3) should be(false)
    (net1 == "1.2.3.4") should be(false)
  }

  it should "perform allHosts operation" in {
    IpNetwork("192.168.1.1/30").allHosts.force should be(
      Seq(IpAddress("192.168.1.0"), IpAddress("192.168.1.1"),
          IpAddress("192.168.1.2"), IpAddress("192.168.1.3"))
    )
  }

  "Network object" should "not contain bad input" in {
    an[IpaddrException] should be thrownBy net1.contains("abc")
    an[IpaddrException] should be thrownBy net1.contains("2.0.0.0.")
  }

  it should "contain IpRange" in {
    net1.contains(range) should be(true)
    net1.contains(range2) should be(false)
    net1.contains(range3) should be(false)
  }

  it should "contain Network" in {
    net1.contains(net4) should be(true)
    net1.contains(net5) should be(true)
    net4.contains(net1) should be(false)
  }

  it should "contain IpAddress" in {
    net1.contains(netAddr) should be(true)
    net1.contains(netAddr2) should be(true)
    net1.contains(netAddr3) should be(false)
    net1.contains(netAddr4) should be(false)
  }

}
