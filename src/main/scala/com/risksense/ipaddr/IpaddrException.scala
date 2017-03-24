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

class IpaddrException(msg: String) extends Exception(msg)

object IpaddrException {
  def invalidAddress(addr: String): String = s"Cannot recognize IP address from `$addr`"
  def invalidGlobAddress(addr: String): String = s"Cannot recognize Glob-style address `$addr`"
  def invalidMask(mask: Int): String = s"Invalid network mask `$mask`"
  val invalidCidrSequence: String = "Given sequence of CIDRs is invalid."
  val versionMismatch: String = "Address version not matching"
}
