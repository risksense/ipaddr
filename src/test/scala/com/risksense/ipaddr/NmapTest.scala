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

// scalastyle:off underscore.import import.grouping

/** Unit tests for Nmap */
class NmapTest extends UnitSpec {

  private val validNmaps = IndexedSeq("192.168.1.2", "192.168.3-5,7.1", "10.2-3.4.5-8",
    "172.163.-.12", "12-.13.14.15")
  private val invalidNmaps = IndexedSeq("192.168.1-4", "10.1.2,1-3", "10.1.2,0,1", "1.2.256.2",
    "a.2.3.0", "17.12.12-a.3")

  "Nmap" should "validate nmap style addresses" in {
    validNmaps.foreach(Nmap.validNmapRange(_) should be(true))
    invalidNmaps.foreach(Nmap.validNmapRange(_) should be(false))
  }

  it should "generate IpAddresses from nmap address" in {
    val next = Nmap.iterNmapRange(validNmaps(0)).next()
    next.toString should be(validNmaps(0))

    Nmap.iterNmapRange(validNmaps(1)).toSeq.map(_.toString) should
      be(Seq("192.168.3.1", "192.168.4.1", "192.168.5.1", "192.168.7.1"))
    an[IpaddrException] should be thrownBy Nmap.iterNmapRange(invalidNmaps(0))
  }

}
