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

class BaseIpTest extends UnitSpec {

  private val addr1 = "192.168.1.200"
  private val addr2 = "192.168.1.230"
  private val net1 = IpNetwork(addr1)
  private val net2 = IpNetwork(addr2)
  private val net3 = IpNetwork("0.0.0.0")
  private val net4 = IpNetwork("255.255.255.255")
  private val nets = Seq("10.2.2.0/24", "10.2.1.0/24", "10.2.1.0/26", "10.2.1.0/29")

  "SpanningCidr" should "give correct result" in {
    val spanNet1 = IpNetwork("192.168.1.192/26")
    val spanNet2 = IpNetwork("0.0.0.0/0")
    BaseIp.spanningCidr(Seq(net1, net2)) should be(spanNet1)
    BaseIp.spanningCidr(Seq(net3, net4)) should be(spanNet2)
    an[IpaddrException] should be thrownBy BaseIp.spanningCidr(Seq(spanNet1))
  }

  "CidrExclude" should "work properly" in {
    val excludeNet1 = IpNetwork("192.168.1.200/32")
    val excludeNet2 = IpNetwork("192.168.1.230/32")
    val n1 = IpNetwork("10.2.1.0/24")
    val n2 = IpNetwork("10.2.2.0/24")
    val n3 = IpNetwork("10.2.2.16/32")
    BaseIp.cidrExclude(addr1, addr1) shouldBe a[Seq[_]]
    an[IpaddrException] should be thrownBy BaseIp.cidrExclude(addr1, "1.2.3.256")
    BaseIp.cidrExclude(addr1, addr2) should be(Seq(excludeNet1))
    BaseIp.cidrExclude(addr2, addr1) should be(Seq(excludeNet2))
    BaseIp.cidrExclude(n1, n2) should be(Seq(n1))
    BaseIp.cidrExclude(n2, n1) should be(Seq(n2))
    BaseIp.cidrExclude(n3, n2) should be(Nil)
    BaseIp.cidrExclude(n2, n3).toString should be("Vector(10.2.2.0/28, 10.2.2.17/32, " +
      "10.2.2.18/31, 10.2.2.20/30, 10.2.2.24/29, 10.2.2.32/27, 10.2.2.64/26, 10.2.2.128/25)")
    BaseIp.cidrExclude("0.0.0.0/0", "255.255.255.255").size should be(32)
  }

  "isValidMask" should "work properly" in {
    BaseIp.isValidMask(24, 4) should be(true)
    BaseIp.isValidMask(24, 5) should be(false)
    BaseIp.isValidMask(33, 4) should be(false)
  }

  "cidrMerge" should "be able to perform network merging" in {
    BaseIp.cidrMerge(Nil) should be(Symbol("empty"))
    BaseIp.cidrMerge(Seq(net1, net2)).size should be(2)
    val n1 = IpNetwork("10.2.3.0/16")
    val n2 = IpNetwork("10.1.2.3")
    val n3 = IpNetwork("10.1.2.3/16")
    val n4 = IpNetwork("192.168.1.8/30")
    val n5 = IpNetwork("192.168.1.12/30")
    BaseIp.cidrMerge(Seq(n1, n2)).size should be(2)
    BaseIp.cidrMerge(Seq(n2, n3)).size should be(1)
    BaseIp.cidrMerge(Seq(n1, n2, n3)).size should be(2)
    BaseIp.cidrMerge(Seq(n4, n5)).toString should be("List(192.168.1.8/29)")
    BaseIp.cidrMerge(Seq(n4, n5, n4, n4, n5)).toString should be("List(192.168.1.8/29)")
  }

  "allMatchingCidrs" should "match all networks for a valid address" in {
    val networks = nets.map(IpNetwork(_))
    BaseIp.allMatchingCidrs("10.2.1.2", nets) should be(networks.drop(1))
    BaseIp.allMatchingCidrs(addr1, nets) should be(Nil)
    BaseIp.allMatchingCidrs(addr1, Nil) should be(Nil)
  }

  it should "generate error if any input address is invalid" in {
    val invalidNets = nets :+ "10.2.1.a/24"
    val searchFor = "10.2.1.2"
    val invalids = Seq((searchFor, invalidNets), ("", nets), ("abc", nets))
    invalids.foreach { x =>
      an[IpaddrException] should be thrownBy BaseIp.allMatchingCidrs(x._1, x._2)
    }
    invalids.foreach { x =>
      an[IpaddrException] should be thrownBy BaseIp.largestMatchingCidr(x._1, x._2)
    }
    invalids.foreach { x =>
      an[IpaddrException] should be thrownBy BaseIp.smallestMatchingCidr(x._1, x._2)
    }
  }

  "largestMatchingCidrs" should "match largest network" in {
    val networks = nets.map(IpNetwork(_))
    BaseIp.largestMatchingCidr("10.2.1.5", nets).get should be(networks(1))
    BaseIp.largestMatchingCidr(addr1, nets) should be(None)
    BaseIp.largestMatchingCidr(addr1, Nil) should be(None)
  }

  "smallestMatchingCidr" should "match largest network" in {
    val networks = nets.map(IpNetwork(_))
    BaseIp.smallestMatchingCidr("10.2.1.6", nets :+ "10.3.1.0/29").get should
      be(networks(3))
    BaseIp.smallestMatchingCidr(addr1, nets) should be(None)
    BaseIp.smallestMatchingCidr(addr1, Nil) should be(None)
  }

  "arrToCidrs" should "generate IpSet" in {
    val targets = Seq("192.168.0.1", "192.168.1.1/24", "192.168.2.*", "192.168.3.1-192.168.3.3",
                      "192.168.4-5.1")
    BaseIp.arrToCidrs(targets.iterator).toString should
      be("IpSet(192.168.0.1/32, 192.168.1.0/24, 192.168.2.0/24, 192.168.3.1/32, " +
        "192.168.3.2/31, 192.168.4.1/32, 192.168.5.1/32)")
    an[IpaddrException] should be thrownBy BaseIp.arrToCidrs(Iterator("z"))
  }

  "arrsToCidrs" should "evaluate IpSet" in {
    val target = Seq("192.168.0.1")
    val targets = Seq("192.168.1.1", "192.168.1.2", "192.168.1.3")
    val exclusions = Seq("192.168.1.2", "192.168.1.3")
    val exclusions2 = Seq("192.168.1.a")
    BaseIp.arrsToCidrs(target, targets, exclusions).toString should
      be("IpSet(192.168.0.1/32, 192.168.1.1/32)")
    an[IpaddrException] should be thrownBy BaseIp.arrsToCidrs(target, targets, exclusions2)
    an[IpaddrException] should be thrownBy BaseIp.arrsToCidrs(target, exclusions2, exclusions2)
    an[IpaddrException] should be thrownBy BaseIp.arrsToCidrs(exclusions2, targets, exclusions)
  }

  "chopSet" should "chop big sets" in {
    val nets = Seq("172.24.1.0/22", "10.0.0.1/32", "192.168.1.1/30")
    val networks = nets.map(IpNetwork(_))
    val s = IpSet(networks)
    BaseIp.chopSet(s).size should be(5)
    BaseIp.chopSet(IpSet()) should be(Seq(IpSet()))
  }

}
