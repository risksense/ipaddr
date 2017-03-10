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

import com.typesafe.scalalogging.StrictLogging

/** An arbitrary IPv4 address range.
  *
  * Formed from a lower and upper bound IP address. The upper bound IP cannot be numerically
  * smaller than the lower bound and the IP version of both must match.
  *
  * @constructor
  * @param start An IPv4 address that forms the lower boundary of this IP range.
  * @param end   An IPv4 address that forms the upper boundary of this IP range.
  */
class IpRange ( // scalastyle:ignore equals.hash.code
    val start: IpAddress,
    val end: IpAddress) extends StrictLogging {

  if (start.version != end.version) {
    logger.error(s"Error creating IpRange with lowerBound: $start, upperBound: $end'")
    throw new IpaddrException(IpaddrException.versionMismatch)
  } else if (start.numerical > end.numerical) {
    throw new IpaddrException(s"Invalid IpRange parameters. The lowerBound value $start cannot " +
      s"be greater than upperBound $end")
  }

  /** HashCode of this [[IpRange]] instance calculated over hashCodes of all [[IpNetwork]] objects
    * in this IpRange.
    */
  override val hashCode: Int = MurmurHash3.orderedHash(cidrs.map(_.hashCode))

  /** The long equivalent value of first IP address in this `IpRange` object. */
  val first: Long = this.start.numerical

  /** The integer equivalent value of last IP address in this `IpRange` object. */
  val last: Long = this.end.numerical

  /** The total number of IP addresses in this range. */
  val size: Long = this.last - this.first + 1

  /** IP version number of this IpRange object. */
  val version: Int = start.version

  /** A key tuple used to uniquely identify this `IPRange`. */
  val key: (Int, Long, Long) = (this.version, this.first, this.last)

  /** A key tuple used to compare and sort this `IPRange` correctly. */
  val sortKey: (Int, Long, Int) = (this.version, this.first, this.start.width - numBits(this.size))

  /** String representation of this IpRange object.
    *
    * @return IP range in common representational format
    * @example 192.168.1.2-192.168.1.23
    */
  override def toString: String = this.start.toString + "-" + this.end.toString

  /** Checks if the input [[IpRange]] belongs to this IpRange object.
    *
    * @param that an IpRange
    * @return True if input object belongs to this IpRange, False otherwise.
    */
  def contains(that: IpRange): Boolean = (this.version == that.version) &&
                                         (this.first <= that.first) &&
                                         (this.last >= that.last)

  /** Checks if a given dot-delimited address belongs to this IpRange object.
    *
    * @param that a dot-delimited IP address
    * @return True if input address belongs to this IpRange, False otherwise.
    */
  def contains(that: String): Boolean = contains(IpAddress(that))

  /** Checks if the input [[IpAddress]] belongs to this IpRange object.
    *
    * @param that an IpAddress
    * @return True if input object belongs to this IpRange, False otherwise.
    */
  def contains(that: IpAddress): Boolean = (this.version == that.version) &&
                                           (this.first <= that.numerical) &&
                                           (this.last >= that.numerical)

  /** Checks if a given [[IpNetwork]] belongs to this IpRange object.
    *
    * @param that an IpNetwork object
    * @return True if input network belongs to this IpRange, False otherwise.
    */
  def contains(that: IpNetwork): Boolean = {
    val shiftWidth = that.ipAddr.width - that.mask
    val thatStart = that.ipAddr.numerical
    // Start of the next network after other
    val nextStart = thatStart + (1 << shiftWidth)
    this.first <= thatStart && this.last > nextStart
  }

  /** Returns a sequence of [[IpNetwork]] objects found within the lower and upper bound addresses
    * of this IpRange.
    *
    * @return Sequence of IpNetwork objects
    */
  def cidrs: Seq[IpNetwork] = {
    val firstNetwork = IpNetwork(this.start, this.start.netmaskBits)
    val lastNetwork = IpNetwork(this.end, this.end.netmaskBits)
    BaseIp.boundedNetworkSeq(firstNetwork, lastNetwork)
  }

  /** Override `equals`.
    *
    * Compares this IpRange with another IpRange for equality.
    *
    * @param other an IpRange object to compare with
    * @return True if ranges are equal, False otherwise
    */
  override def equals(other: Any): Boolean = other match {
    case that: IpRange =>
      that.canEquals(this) && (this.start == that.start) && (this.end == that.end)
    case _ => false
  }

  /** Checks if `that` is same instance of `this` IpRange.
    *
    * @param other an Object
    * @return True if both objects are instances of IpRange class, False otherwise.
    */
  def canEquals(other: Any): Boolean = other.isInstanceOf[IpRange]

  /** Minimum number of bits required to represent this value.
    *
    * @param num a Long value
    * @return number of bits required to represent `num`
    */
  private def numBits(num: Long): Int = num.toBinaryString.length

}

/** Companion object for IpRange class */
object IpRange {

  /** Creates an IpRange object from two IP addresses in string notation.
    *
    * The`start` value cannot be higher than the `end` value.
    *
    * @param start Lower bound IP address in string format
    * @param end Upper bound IP address in string format
    * @return IpRange if input addresses are valid
    */
  @throws(classOf[IpaddrException])
  def apply(start: String, end: String): IpRange = new IpRange(IpAddress(start), IpAddress(end))

}
