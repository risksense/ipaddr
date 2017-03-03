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

import scala.util.hashing.MurmurHash3

/** IpAddress trait provides access to all the API functions by extending IpAddressMath
  * and IpAddressFormat traits.
  */
trait IpAddress // scalastyle:ignore equals.hash.code
  extends Ordered[IpAddress]
    with IpAddressMath
    with IpAddressFormat {

  /** Hashcode override.
    *
    * HashCode of this IpAddress is calculated over all octets by using Austin Appleby's MurmurHash3
    * algorithm. This algorithm is used because numerical value of an IP address is longer than
    * signed Int max value. Therefore, traditional hashcode calculation will not work.
    */
  override lazy val hashCode: Int = MurmurHash3.orderedHash(words)

  /** Checks if numerical value of this IP address is zero.
    *
    * @return True if the numerical value of this IP address is not zero, False otherwise.
    */
  lazy val nonZero: Boolean = numerical != 0

  /** Returns a tuple that uniquely identifies this IP address.
    *
    * @return Tuple(IPnumerical, IPversion)
    */
  lazy val key: (Long, Int) = (numerical, version)

  def compare(that: IpAddress): Int = {
    // Compares the key value from two IpAddress objects.
    if (this.equals(that)) {
      0
    } else if (this.key._1 < that.key._1) {
      -1
    } else {
      1
    }
  }

  /** Compares numerical values of two IpAddress objects.
    *
    * @param other IpAddress to compare
    * @return True if these IpAddress objects are equal, False otherwise.
    */
  override def equals(other: Any): Boolean = other match {
    case that: IpAddress => that.canEqual(this) && (this.key == that.key)
    case _ => false
  }

  /** Checks if a given object is an instance of IpAddress.
    *
    * @param other an Object
    * @return True if both objects are instances of IpAddress class, False otherwise
    */
  def canEqual(other: Any): Boolean = other.isInstanceOf[IpAddress]

  /** Checks if this IP address falls under LinkLocal network.
    *
    * @return True if this IP address is link-local address, False otherwise.
    *         Reference: RFCs 3927 and 4291.
    */
  def isLinkLocal: Boolean

  /** Checks if this IP address falls under a loopback network.
    *
    * @return True if this IP address is a loopback address, False otherwise.
    */
  def isLoopback: Boolean

  /** Checks if this IP address falls under a unicast network.
    *
    * @return True if this IP address is a unicast address, False otherwise.
    */
  def isUnicast: Boolean

  /** Checks if this IP address falls under a multicast network.
    *
    * @return True if this IP address is mulitcast address, False otherwise.
    */
  def isMulticast: Boolean

  /** Checks if this IP address falls under a private network.
    *
    * @return True if this IP address is private address, False otherwise.
    */
  def isPrivate: Boolean

  /** Checks if this IP address falls under a reserved network.
    *
    * @return True if this IP address is reserved address, False otherwise.
    */
  def isReserved: Boolean

  /** Apply a network mask to this address and return the result as a new IpAddress object.
    *
    * @param mask Numerical equivalent of netmask
    * @return IP address after netmask application
    */
  def applyMask(mask: Int): IpAddress = {
    if (Ipv4.isValidMask(mask)) {
      val addressWithMask = binary.substring(0, mask)
                                  .padTo(width, Zero.head)
                                  .mkString
      val numericalAddress = binaryToNum(addressWithMask)
      IpAddress(numericalAddress)
    } else {
      throw new IpaddrException(IpaddrException.invalidMask(mask))
    }
  }

}

/** Methods to create an IpAddress object. */
object IpAddress {

  /** Creates an IpAddress form a dot-delimited string notation.
    *
    * @param address  Dot-delimited IP address
    * @return IpAddress object if the input is valid
    * @throws IpaddrException if the input address is invalid
    */
  @throws(classOf[IpaddrException])
  def apply(address: String): IpAddress = {
    if (BaseIp.isValidAddress(address, Ipv4.version)) {
      new Ipv4(address)
    } else {
      throw new IpaddrException(IpaddrException.invalidAddress(address))
    }
  }

  /** Creates an IpAddress from a numerical value.
    *
    * @param addressNum  A Long equivalent of an IP address
    * @return an IpAddress object if the input is valid
    * @throws IpaddrException if the input address is invalid
    */
  @throws(classOf[IpaddrException])
  def apply(addressNum: Long): IpAddress = {
    val addr = BaseIp.isValidAddress(addressNum, Ipv4.version)
    if (addr.isEmpty) {
      throw new IpaddrException(IpaddrException.invalidAddress(addressNum.toString))
    }
    new Ipv4(addr.get)
  }

  /** Creates an IpAddress from another IpAddress object.
    *
    * @param address an IpAddress object
    * @return an IpAddress object
    */
  private[ipaddr] def apply(address: IpAddress): IpAddress = new Ipv4(address.toString)

}
