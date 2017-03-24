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

class IpSetTest extends UnitSpec {

  private val n1 = IpNetwork("192.168.1.2")
  private val n2 = IpNetwork("192.168.1.3/24")
  private val n3 = IpNetwork("10.1.2.3")
  private val n4 = IpNetwork("10.2.3.0/16")
  private val n5 = IpNetwork("192.168.1.23/24")
  private val r1 = IpRange("10.0.0.0", "10.0.0.255")
  private val r2 = IpRange("10.0.0.128", "10.10.10.10")
  private val setN1N2 = IpSet(Seq(n1, n2))
  private val setN1 = IpSet(n1)
  private val setN5 = IpSet(n5)
  private val setMix = IpSet(Seq(n3, n1, n4))

  private val n6 = IpNetwork("12.0.2.0")
  private val n7 = IpNetwork("12.0.2.1")
  private val n8 = IpNetwork("12.0.2.3")
  private val n9 = IpNetwork("12.0.2.3/31")
  private val n10 = IpNetwork("12.0.2.0/24")
  private val n11 = IpNetwork("12.0.3.0/24")
  private val n12 = IpNetwork("12.0.4.0/24")
  private val setN6 = IpSet(n6)
  private val setN7 = IpSet(n7)
  private val setN8 = IpSet(n8)
  private val setN9 = IpSet(n9)
  private val setN10 = IpSet(n10)
  private val setN11 = IpSet(n11)
  private val setN12 = IpSet(n12)
  private val setN6N7 = "IpSet(12.0.2.0/31)"

  private val n13 = IpNetwork("10.2.2.0")
  private val n14 = IpNetwork("10.2.2.2")
  private val n15 = IpNetwork("10.2.2.4")
  private val setN13N14 = IpSet(Seq(n13, n14))
  private val setN14N15 = IpSet(Seq(n14, n15))

  "Creating IpSet" should "succeed if input is valid" in {
    val setRangeStr = "IpSet(192.168.1.2/31, 192.168.1.4/30, 192.168.1.8/30, 192.168.1.12/32)"
    val setNetworkStr = "IpSet(192.168.1.0/24)"
    val setNetwork2Str = "IpSet(192.168.1.2/32)"
    val setSetStr = setRangeStr
    val setMixStr = "IpSet(10.1.2.3/32, 10.2.3.0/16, 192.168.1.2/32)"
    val range = IpRange("192.168.1.2", "192.168.1.12")
    val setRange = IpSet(range)
    val setSet = IpSet(setRange)
    setRange.toString should be(setRangeStr)
    setN1N2.toString should be(setNetworkStr)
    setN1.toString should be(setNetwork2Str)
    setSet.toString should be(setSetStr)
    setMix.toString should be(setMixStr)
  }

  "IpSet" should "check for equality" in {
    val emptySet = IpSet()
    setMix.empty should be(emptySet)
    setN1N2 should be(setN5)
  }

  it should "perform union" in {
    (setN6 | setN7).toString should be(setN6N7)
    (setN6 | setN7 | setN8).toString should be("IpSet(12.0.2.0/31, 12.0.2.3/32)")
    (setN6 | setN7 | setN9).toString should be("IpSet(12.0.2.0/30)")
    (setN10 | setN11 | setN12).toString should be("IpSet(12.0.2.0/23, 12.0.4.0/24)")
    (setN13N14 | setN14N15).toString should be("IpSet(10.2.2.0/32, 10.2.2.2/32, 10.2.2.4/32)")
    (setN14N15 | setN13N14) should be(setN13N14 | setN14N15)
  }

  it should "perform intersection" in {
    (setN13N14 & setN14N15).toString should be("IpSet(10.2.2.2/32)")
    (setN14N15 & setN13N14) should be(setN13N14 & setN14N15)
    (setN10 & setN9) should be(setN9)
  }

  it should "convert IpSet to IpRange" in {
    val n1 = IpNetwork("10.0.0.0/25")
    val n2 = IpNetwork("10.0.0.128/25")
    val n3 = IpNetwork("10.0.1.2/32")
    val ipSet = IpSet(Seq(n1, n2)) // this set is contiguous
    val ipSet2 = IpSet(Seq(n1, n2, n3)) // this is non-contiguous set
    val ipRange = IpRange("10.0.0.0", "10.0.0.255")
    ipSet.ipRange shouldBe a[IpRange]
    ipSet.ipRange should be(ipRange)

    // non-contiguous set cannot be converted
    an[IpaddrException] should be thrownBy ipSet2.ipRange

    // empty set cannot be converted
    an[IpaddrException] should be thrownBy IpSet().ipRange
  }

  it should "add a disjoint Network" in {
    val emptySet = IpSet()
    val net = emptySet + n6
    net should be(setN6)
    net should not be(setN7)
    (setN8 + n6).toString should be("IpSet(12.0.2.0/32, 12.0.2.3/32)")
  }

  it should "add an overlapping Network" in {
    (setN7 + n6).toString should be(setN6N7)
    (setN6 + n7).toString should be(setN6N7)
  }

  it should "add a matching Network" in {
    (setN6 + n6) should be(setN6)
  }

  it should "subtract a disjoint Network" in {
    (setN8 - n1) should be(setN8)
  }

  it should "subtract an overlapping Network" in {
    (setN10 - n9).toString should be("IpSet(12.0.2.0/31, 12.0.2.4/30, 12.0.2.8/29, 12.0.2.16/28, " +
                                 "12.0.2.32/27, 12.0.2.64/26, 12.0.2.128/25)")
  }

  it should "subtract a matching Network" in {
    (setN1 - n1) should be(IpSet())
  }

  it should "add an IpAddress" in {
    val emptySet = IpSet()
    val add1 = emptySet + IpAddress("1.1.1.1")
    add1.toString should be("IpSet(1.1.1.1/32)")
  }

  it should "subtract an IpAddress" in {
    val sub1 = setN1 - IpAddress(n1.ipAddr.toString)
    val sub2 = setN1N2 - IpAddress(n1.ipAddr.toString)
    sub1 should be(sub1.empty)
    sub2.toString should be("IpSet(192.168.1.0/31, 192.168.1.3/32, 192.168.1.4/30, " +
                            "192.168.1.8/29, 192.168.1.16/28, 192.168.1.32/27, 192.168.1.64/26, " +
                            "192.168.1.128/25)")
  }

  it should "add a disjoint IpRange" in {
    val emptySet = IpSet()
    val add1 = emptySet + r1
    add1.toString should be("IpSet(10.0.0.0/24)")
  }

  it should "subtract an IpRange" in {
    val s1 = IpSet(r1)
    val s2 = s1 - r2
    s1.toString should be("IpSet(10.0.0.0/24)")
    s2.toString should be("IpSet(10.0.0.0/25)")
  }

  it should "support symmetric difference" in {
    setN9.symmetricDiff(setN10).toString should be("IpSet(12.0.2.0/31, 12.0.2.4/30, 12.0.2.8/29, " +
      "12.0.2.16/28, 12.0.2.32/27, 12.0.2.64/26, 12.0.2.128/25)")
    (setN9 ^ setN11).toString should be("IpSet(12.0.2.3/31, 12.0.3.0/24)")
  }

  it should "compare with another IpSet" in {
    (setN6 < setN7) should be(false)
    (setN6 < setN6) should be(false)
    (setN6 <= setN6) should be(true)
    (setN7 < setN6) should be(false)
    (setN10 < (setN10 + n11)) should be(true)
    ((setN10 + n11) > setN10) should be(true)
    (setN8 > setN7) should be(false)
    (setN8 > setN8) should be(false)
    (setN8 >= setN8) should be(true)
    setN6.subsetOf(setN6) should be(true)
    setN6.subsetOf(setN9) should be(false)
    setN6.subsetOf(setN10) should be(true)
    setN10.subsetOf(setN6) should be(false)
    setN10.supersetOf(setN6) should be(true)
    setN9.subsetOf(setN10) should be(true)
    setN9 should be(setN9)
    setN9 should not be(n9)
  }

  it should "check disjoint" in {
    setN6.isDisjoint(setN7) should be(true)
    setN7.isDisjoint(setN6) should be(true)
    setN6.isDisjoint(setN8) should be(true)
    setN7.isDisjoint(setN9) should be(true)
    setN6.isDisjoint(setN10) should be(false)
    setN8.isDisjoint(setN10) should be(false)
  }

  it should "merge sets" in {
    val n1 = IpNetwork("10.2.2.8/30")
    val n2 = IpNetwork("10.2.2.12/30")
    val n3 = IpNetwork("10.2.2.8/29")
    val set1 = IpSet(n1)
    val set2 = IpSet(n2)
    val set3 = IpSet(n3)
    (set1 | set2) should be(set3)
  }

  it should "check continuity" in {
    setN1N2.isContiguous should be(true)
    setMix.isContiguous should be(false)
  }

  it should "iterate over all elements" in {
    setN1N2.networkSeq.toString should be("Vector(192.168.1.0/24)")
  }

  it should "calculate volume" in {
    setN1.volume should be(1)
    setN1N2.volume should be(256)
  }

  it should "contain IpAddress" in {
    val ip1 = IpAddress(n1.ipAddr.toString)
    val ip2 = IpAddress(n2.ipAddr.toString)
    setMix.contains(ip1) should be(true)
    setN1.contains(ip2) should be(false)
  }

}
