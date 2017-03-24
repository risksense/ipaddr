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

import scala.collection.immutable.HashSet

class IpAddressTest extends UnitSpec {

  private val ip1 = IpAddress("192.168.1.2")
  private val ip2 = IpAddress("192.168.1.250")
  private val ip3 = IpAddress("192.168.1.2")
  private val ip4 = IpAddress("10.168.1.2")
  private val ip5 = IpAddress("0.0.0.0")
  private val ip6 = IpAddress("255.255.192.0")
  private val ip7 = IpAddress("0.0.255.255")
  private val ip1Add2 = IpAddress("192.168.1.4")
  private val ip2Add6 = IpAddress("192.168.2.0")
  private val ip2Sub3 = IpAddress("192.168.1.247")
  private val octet = List(0, 255)

  private val ipValidStrings = for { i <- octet; j <- octet; k <- octet; l <- octet } yield {
    List(i, j, k, l).mkString(".")
  }
  private val octet1 = List(0, 256)
  private val ipInvalidStrings = (for { i <- octet1; j <- octet1; k <- octet1; l <- octet1 } yield {
    List(i, j, k, l).mkString(".")
  }).tail

  "Creating an IpAddress" should "result in IpError if address is invalid" in {
    an[IpaddrException] should be thrownBy IpAddress("192.168.1")
    an[IpaddrException] should be thrownBy IpAddress(4294967296L)
    for { invalidIp <- ipInvalidStrings } {
      an[IpaddrException] should be thrownBy IpAddress(invalidIp)
    }
  }

  it should "create IpAddress object if address is valid" in {
    val ipFromString = IpAddress("192.168.1.0")
    val ipFromNum = IpAddress(3232235778L)
    ipFromString shouldBe a[IpAddress]
    ipFromNum shouldBe a[IpAddress]
    ipFromString.toString should be("192.168.1.0")
    for { validIp <- ipValidStrings } {
      IpAddress(validIp) shouldBe a[IpAddress]
    }
  }

  "An IpAddress object" should "perform all IP based operations correctly" in {
    ip1.numerical should be(3232235778L)
    ip1.hex should be("c0a80102")
    ip5.nonZero should be(false)
    ip2.nonZero should be(true)
    ip1.isHostmask should be(false)
    ip7.isHostmask should be(true)
    ip1.isNetmask should be(false)
    ip6.isNetmask should be(true)
    ip1.netmaskBits should be(32)
    ip6.netmaskBits should be(18)
    ip1.maxWord should be(255)
  }

  it should "perform add operation correctly" in {
    ip6 + 16000 shouldBe a[IpAddress]
    an[IpaddrException] should be thrownBy ip6 + 17000
    ip5 + 1 shouldBe a[IpAddress]
    (ip1 + 2) should be(ip1Add2)
    (ip2 + 6) should be(ip2Add6)
  }

  it should "perform subtract operation correctly" in {
    an[IpaddrException] should be thrownBy (ip5 - 1)
    (ip2 - 3) should be(ip2Sub3)
  }

  it should "perform binary shift operations correctly" in {
    val lShifted = IpAddress("42.160.4.8")
    val rShifted = IpAddress("48.42.0.64")
    (ip4 << 2) should be(lShifted)
    (ip1 >> 2) should be(rShifted)
    an[IpaddrException] should be thrownBy (ip1 << 1)
  }

  it should "perform binary operations (and, or, xor) correctly" in {
    val ip1And4 = IpAddress("0.168.1.2")
    val ip1Or4 = IpAddress("202.168.1.2")
    val ip1Xor4 = IpAddress("202.0.0.0")
    (ip1 & ip4) should be(ip1And4)
    (ip1 | ip4) should be(ip1Or4)
    (ip1 ^ ip4) should be(ip1Xor4)
  }

  it should "perform all comparison operations correctly" in {
    val s = HashSet(ip2, ip3, ip4)
    (ip1 == ip2) should be(false)
    ip1 should be(ip3)
    ip1 should not be "a"
    (ip1 >= ip2) should be(false)
    (ip1 >= ip4) should be(true)
    (ip1 > ip4) should be(true)
    (ip4 <= ip2) should be(true)
    (ip1 <= ip3) should be(true)
    (ip4 < ip2) should be(true)
    (ip1 != ip3) should be(false)
    (ip1 != ip2) should be(true)
    s.contains(ip1) should be(true)
    s.contains(ip5) should be(false)
  }

  it should "perform conversion operations correctly" in {
    ip1.binaryToNum("11000000101010000000000100000001") should be(3232235777L)
    ip1.binaryToNum("110000001010100000000001000000") should be(0)
    ip1.binaryToNum("1a000000101010000000000100000001") should be(0)
  }

  "Ipv4" should "expand partial addresses" in {
    Ipv4.expandPartialAddress("12") should be(Some("12.0.0.0"))
    Ipv4.expandPartialAddress("12.13") should be(Some("12.13.0.0"))
    Ipv4.expandPartialAddress("12.13.14") should be(Some("12.13.14.0"))
    Ipv4.expandPartialAddress("12.13.14.15") should be(Some("12.13.14.15"))
    Ipv4.expandPartialAddress("12.13.14.15.") should be(None)
    Ipv4.expandPartialAddress("12.13.14.256") should be(None)
  }

  it should "identify Loopback addresses" in {
    val loopBack1 = IpAddress("127.0.0.1")
    val loopBack2 = IpAddress("127.255.255.254")
    val loopBack3 = IpAddress("128.0.0.0")
    val loopBack4 = IpAddress("126.255.255.255")
    loopBack1.isLoopback should be(true)
    loopBack2.isLoopback should be(true)
    loopBack3.isLoopback should be(false)
    loopBack4.isLoopback should be(false)
  }

  it should "identify Private addresses" in {
    val private1 = IpAddress("172.16.0.1")
    val private2 = IpAddress("172.31.255.255")
    val private3 = IpAddress("171.16.0.0")
    val private4 = IpAddress("172.32.0.0")

    val private5 = IpAddress("10.0.0.1")
    val private6 = IpAddress("10.255.255.255")
    val private7 = IpAddress("11.0.0.0")

    val private8 = IpAddress("192.168.0.0")
    val private9 = IpAddress("192.168.255.255")
    val private10 = IpAddress("192.169.0.0")
    val private11 = IpAddress("192.167.255.255")

    private1.isPrivate should be(true)
    private2.isPrivate should be(true)
    private3.isPrivate should be(false)
    private4.isPrivate should be(false)
    private5.isPrivate should be(true)
    private6.isPrivate should be(true)
    private7.isPrivate should be(false)
    private8.isPrivate should be(true)
    private9.isPrivate should be(true)
    private10.isPrivate should be(false)
    private11.isPrivate should be(false)
  }

  it should "identify Reserved addresses" in {
    val reserved1 = IpAddress("192.0.0.0")
    val reserved2 = IpAddress("192.0.0.255")
    val reserved3 = IpAddress("192.0.1.0")
    val reserved4 = IpAddress("191.0.0.0")

    val reserved5 = IpAddress("240.0.0.0")
    val reserved6 = IpAddress("255.255.255.255")
    val reserved7 = IpAddress("238.255.255.255")

    reserved1.isReserved should be(true)
    reserved2.isReserved should be(true)
    reserved3.isReserved should be(false)
    reserved4.isReserved should be(false)
    reserved5.isReserved should be(true)
    reserved6.isReserved should be(true)
    reserved7.isReserved should be(false)
  }

  it should "identify Multicast addresses" in {
    val multicast1 = IpAddress("239.192.0.1")
    val multicast2 = IpAddress("224.0.0.0")
    val multicast3 = IpAddress("239.255.255.255")
    val multicast4 = IpAddress("240.0.0.0")
    val unicast1 = IpAddress("192.0.2.1")

    multicast1.isMulticast should be(true)
    multicast2.isMulticast should be(true)
    multicast3.isMulticast should be(true)
    multicast4.isMulticast should be(false)
    unicast1.isMulticast should be(false)
  }

  it should "identify Unicast addresses" in {
    val multicast1 = IpAddress("239.192.0.1")
    val unicast1 = IpAddress("192.0.2.1")
    unicast1.isUnicast should be(true)
    multicast1.isUnicast should be(false)
  }

  it should "identify Linklocal addresses" in {
    val linklocal1 = IpAddress("169.254.0.0")
    val linklocal2 = IpAddress("169.254.0.1")
    val linklocal3 = IpAddress("169.254.255.255")
    val linklocal4 = IpAddress("169.255.0.1")
    linklocal1.isLinkLocal should be(true)
    linklocal2.isLinkLocal should be(true)
    linklocal3.isLinkLocal should be(true)
    linklocal4.isLinkLocal should be(false)
  }

}
