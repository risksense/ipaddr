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

// scalastyle:off multiple.string.literals

/** Unit tests for IpGlob */
class IpGlobTest extends UnitSpec {


  private val validGlobs = IndexedSeq("1.2.3.4", "255.255.255.0-31", "255.255.255.*",
    "255.255.*.*", "255.2-3.*.*", "1-200.*.*.*", "*.*.*.*")
  private val invalidGlobs = IndexedSeq("*.255.255.255", "255.*.255.255", "255.2-1.*.*",
    "2-1.255.255.255", "192.168.1", "192.168.1.a")

  "IpGlob" should "validate octets" in {
    validGlobs.foreach {
      IpGlob.validGlob(_) should be(true)
    }
    invalidGlobs.foreach {
      IpGlob.validGlob(_) should be(false)
    }
  }

  it should "convert valid globs to IpAddress tuple" in {
    val globToIpStr = validGlobs.map(IpGlob.globToIpTuple(_))
    globToIpStr(0) should
      be((IpAddress(validGlobs(0)),
        IpAddress(validGlobs(0))))
    globToIpStr(1) should
      be((IpAddress("255.255.255.0"),
        IpAddress("255.255.255.31")))
    IpGlob.globToIpTuple("192.168.0.12-14") should
      be((IpAddress("192.168.0.12"),
        IpAddress("192.168.0.14")))
    IpGlob.globToIpTuple("*.*.*.*") should
      be((IpAddress("0.0.0.0"),
        IpAddress("255.255.255.255")))
  }

  it should "not convert invalid globs to IpAddress tuple" in {
    invalidGlobs.foreach {
      an[IpaddrException] should be thrownBy IpGlob.globToIpTuple(_)
    }
  }

  it should "convert valid globs to IpRange" in {
    val r1 = IpRange("192.168.0.12", "192.168.0.14")
    val r2 = IpRange("192.0.0.0", "192.1.255.255")
    IpGlob.globToIpRange("192.168.0.12-14") should be(r1)
    IpGlob.globToIpRange("192.0-1.*.*") should be(r2)
  }

  it should "not convert invalid globs to IpRange" in {
    invalidGlobs.foreach {
      an[IpaddrException] should be thrownBy IpGlob.globToIpRange(_)
    }
  }

  it should "generate globs from valid address bounds" in {
    val n1 = IpAddress("0.0.0.0")
    val n2 = IpAddress("1.1.1.1")
    IpGlob.ipRangeToGlobs(n1, n2) should
      be(Seq("0.*.*.*", "1.0.*.*", "1.1.0.*", "1.1.1.0-1"))
    IpGlob.ipRangeToGlobs("10.3.10.0", "10.2.10.0") should
      be(Seq("10.3.10-11.*", "10.3.12-15.*", "10.3.16-31.*", "10.3.32-63.*",
        "10.3.64-127.*"))
  }

  it should "not generate globs from invalid address bounds" in {
    an[IpaddrException] should be thrownBy IpGlob.ipRangeToGlobs("1.2.3.4", "1.256.1.2")
  }

  it should "translate CIDR address to globs" in {
    IpGlob.cidrToGlob("10.2.1.2/31") should be(Seq("10.2.1.2-3"))
    IpGlob.cidrToGlob("10.2.1.2/16") should be(Seq("10.2.*.*"))
    an[IpaddrException] should be thrownBy IpGlob.cidrToGlob("10.1.2.a")
    an[IpaddrException] should be thrownBy IpGlob.cidrToGlob("10.1.2.256")
  }

  it should "translate glob to Networks" in {
    val n1 = IpNetwork("10.2.1.2/31")
    val n2 = IpNetwork("10.2.1.2/16")
    IpGlob.globToCidrs("10.2.1.2-3") should be(Seq(n1))
    IpGlob.globToCidrs("10.2.*.*") should be(Seq(n2))
    an[IpaddrException] should be thrownBy IpGlob.globToCidrs("10.2-3.0.*")
  }
}
