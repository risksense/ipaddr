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

import scala.math.Ordering.Implicits._ // scalastyle:ignore underscore.import

/** An IPv4 network or subnet. A combination of an IP address and a network mask.
  * Accepts CIDR and several related variants :
  *
  * (a) Standard CIDR
  * <ul><li>x.x.x.x/y -> 192.0.2.0/24 </li></ul>
  *
  * b) Hybrid CIDR format (netmask address instead of prefix), where 'y' address represent a
  * valid netmask
  * <ul><li>x.x.x.x/y.y.y.y -> 192.0.2.0/255.255.255.0 </li></ul>
  *
  * c) ACL hybrid CIDR format (hostmask address instead of prefix like Cisco's ACL bitmasks),
  * where 'y' address represent a valid netmask
  * <ul><li>x.x.x.x/y.y.y.y -> 192.0.2.0/0.0.0.255 </li></ul>
  *
  * d) Abbreviated CIDR format
  * <ul><li>x   -> 192 </li>
  * <li>x/y     -> 10/8 </li>
  * <li>x.x/y   -> 192.168/16 </li>
  * <li>x.x.x/y -> 192.168.0/24 </li></ul>
  * which are equivalent to:
  * <ul><li>x.0.0.0/y   -> 192.0.0.0/24 </li>
  * <li>x.0.0.0/y   -> 10.0.0.0/8 </li>
  * <li>x.x.0.0/y   -> 192.168.0.0/16 </li>
  * <li>x.x.x.0/y   -> 192.168.0.0/24 </li></ul>
  *
  * @constructor    creates an IpNetwork object
  * @param address  String representation of a network address
  * @param mask     Number of network bits
  * @param version  IP version of this network
  */
class IpNetwork private[ipaddr](
    address: String,
    val mask: Int,
    val version: Int)
  extends Ordered[IpNetwork] {

  /** The IP address of this IpNetwork object.
    *
    * This may or may not be the same as the network IP address which varies according to the
    * value of the CIDR subnet prefix.
    */
  val ip: IpAddress = IpAddress(address)

  // scalastyle:ignore equals.hash.code
  private val maskedAddr = this.ip.applyMask(this.mask)

  /** Number of IP addresses in this Network. */
  lazy val size: Long = last - first + 1

  /** The network address of this IpNetwork as an IpAddress object. */
  lazy val network: IpAddress = ipAddr

  /** HashCode of this IpNetwork is calculated over all octets.
    *
    * Please refer to hashCode in [[IpAddress]] for details.
    */
  override def hashCode: Int = ipAddr.hashCode

  /** True address for this IpNetwork object which omits any host bits to the right of the CIDR
    * subnet prefix.
    */
  val ipAddr: IpAddress = this.maskedAddr

  /** Number of host bits in this network address */
  val hostmaskNum: Long = (1L << (this.ipAddr.width - this.mask)) - 1

  /** The host mask of this IpNetwork object */
  val hostmask: IpAddress = IpAddress(this.hostmaskNum)

  /** Numerical value of the network address */
  val netmaskNum: Long = this.ip.maxNumerical ^ this.hostmaskNum

  /** The subnet mask of this IpNetwork as an IpAddress object.
    * @example If netmask is 24, then IpAddress(255.255.255.0)
    */
  val netmask: IpAddress = IpAddress(this.netmaskNum)

  /** The broadcast address of this IpNetwork object. */
  val broadcast: IpAddress = IpAddress(this.ip.numerical | this.hostmaskNum)

  /** The numerical value of first IP address found within this IpNetwork object. */
  val first: Long = this.ip.numerical & (this.ip.maxNumerical ^ this.hostmaskNum)

  /** The numerical value of last IP address found within this IpNetwork object. */
  val last: Long = this.ip.numerical | this.hostmaskNum

  /** A key tuple used to uniquely identify this Network.
    *
    * (numerical IP address, numerical first host, numerical last host)
    */
  val key: (Int, Long, Long) = (this.version, this.first, this.last)

  /** A tuple used to sort networks.
    *
    * A tuple (IP version, numerical first host, net bits, host bits) that is used to perform
    * sorting.
    *
    * @return A key tuple used to compare and sort this network.
    */
  lazy val sortKey: (Int, Long, Int, Long) = {
    val netSizeBits = this.mask - 1
    val hostBits = this.ip.numerical - this.first
    (this.version, this.first, netSizeBits, hostBits)
  }

  /** All hosts in this Network. Includes network address and broadcast address as well. */
  lazy val allHosts: Stream[IpAddress] = {
    if (this.version == 4) {
      BaseIp.addressStream(IpAddress(this.first), IpAddress(this.last))
    } else {
      Stream()
    }
  }

  /** The true CIDR address for this IpNetwork object which omits any host bits to the right of the
    * CIDR subnet prefix.
    */
  def cidr: IpNetwork = IpNetwork(this.ipAddr, this.mask)

  /** Succeeding Network.
    *
    * @param step The number of IP subnets between this IpNetwork object and the expected subnet.
    *             Defaults to 1.
    * @return The adjacent subnet succeeding this IpNetwork object.
    * @throws IpaddrException if the address cannot be calculated.
    */
  def next(step: Int = 1): IpNetwork = {
    val newValue = this.ipAddr.numerical + (this.size * step)
    if (newValue + (this.size - 1) > this.ipAddr.maxNumerical || (newValue < 0)) {
      throw new IpaddrException(IpaddrException.invalidAddress(newValue.toString))
    } else {
      IpNetwork(newValue, this.mask)
    }
  }

  /** Preceding network
    *
    * @param step The number of IP subnets between this IpNetwork object and the expected subnet.
    *             Defaults to 1.
    * @return The adjacent subnet preceding this IpNetwork object.
    * @throws IpaddrException if the address cannot be calculated.
    */
  def previous(step: Int = 1): IpNetwork = {
    val newValue = this.ipAddr.numerical - (this.size * step)
    if ((newValue + (this.size - 1)) > this.ipAddr.maxNumerical || (newValue < 0)) {
      throw new IpaddrException(IpaddrException.invalidAddress(newValue.toString))
    } else {
      IpNetwork(newValue, this.mask)
    }
  }

  /** Find subnet.
    *
    * Divide this Network's subnet into smaller subnets based on a specified CIDR prefix.
    *
    * @param prefix  A prefix value indicating size of subnets to be returned.
    * @param count   Number of consecutive networks to be returned. (Optional)
    * @return a sequence of IpNetwork objects
    */
  def subnet(prefix: Int, count: Int = 0): Seq[IpNetwork] = {
    if (prefix < 0 || prefix > this.ipAddr.width || prefix < this.mask) {
      Nil
    } else {
      val maxSubnets = scala.math.pow(2, prefix - this.mask).toInt
      val countNew = count match {
        case 0 => maxSubnets
        case _ => count
      }
      if (countNew < 0 || countNew > maxSubnets) {
        Nil
      } else {
        val temp = IpNetwork(this.first, prefix)
        for { i <- 0 until countNew } yield {
          val addressNum = temp.ip.numerical + (temp.size * i)
          IpNetwork(addressNum, prefix)
        }
      }
    }
  }

  /** Find supernet.
    *
    * Returns a sequence of supernets for this [[IpNetwork]] object between the size of the current
    * prefix and (if specified) an endpoint prefix.
    *
    * @param prefix A prefix value for the maximum supernet (optional)
    *               Default: 0 - returns all possible supernets.
    * @return a sequence of IpNetwork objects.
    */
  def supernet(prefix: Int = 0): Seq[IpNetwork] = {
    if (prefix < 0 || prefix > this.ipAddr.width) {
      Nil
    } else {
      for { p <- prefix until this.mask } yield IpNetwork(this.ipAddr, p)
    }
  }

  /** String representation of this network object.
    *
    * @return a CIDR format string with address and mask.
    */
  override def toString: String = ip.toString + IpNetwork.fwdSlashStr + mask

  def compare(that: IpNetwork): Int = {
    // Compares the key objects from two IpAddress objects.
    if (this.equals(that)) {
      0
    } else if (this.sortKey < that.sortKey) {
      -1
    } else {
      1
    }
  }

  // $COVERAGE-OFF$

  /** Override `equals`.
    *
    * Checks if two IpNetwork objects are equal. Networks are equal if they have same network
    * address and same mask.
    *
    * @param other IpNetwork object to compare with
    * @return True if IpNetwork objects are equal, False otherwise
    */
  override def equals(other: Any): Boolean = other match {
    case that: IpNetwork => that.canEquals(this) && (this.key == that.key)
    case _ => false
  }

  // $COVERAGE-ON$

  /** Checks if that is same instance of this IpNetwork object.
    *
    * @param other an Object
    * @return True if both objects are instances of IpNetwork class, False otherwise.
    */
  def canEquals(other: Any): Boolean = other.isInstanceOf[IpNetwork]

  /** Checks if the given [[IpRange]] belongs to this [[IpNetwork]] object.
    *
    * @param range an IpRange object
    * @return True if input IpRange belongs to this Network, False otherwise.
    */
  def contains(range: IpRange): Boolean = {
    if (this.version != range.version) {
      false
    } else {
      val shiftWidth = this.ipAddr.width - this.mask
      val thisNet = this.ip.numerical >> shiftWidth // flush the host bits
      (this.ipAddr.numerical <= range.start.numerical) &&
        (((thisNet + 1) << shiftWidth) > range.end.numerical)
    }
  }

  /** Checks if the given IpNetwork belongs to this [[IpNetwork]] object.
    *
    * @param net a IpNetwork object
    * @return True if input IpNetwork belongs to this Network, False otherwise.
    */
  def contains(net: IpNetwork): Boolean = {
    if (this.version != net.version) {
      false
    } else {
      val shiftWidth = this.ipAddr.width - this.mask
      val thisNet = this.ip.numerical >> shiftWidth // flush the host bits
      val thatNet = net.ip.numerical >> shiftWidth
      (thatNet == thisNet) && (net.mask >= this.mask)
    }
  }

  /** Checks if the given IP address belongs to this [[IpNetwork]] object.
    *
    * @param addr an IP address specified as dot-delimited string
    * @return True if input address belongs to this Network, False otherwise.
    */
  def contains(addr: String): Boolean = contains(IpAddress(addr))

  /** Checks if the given [[IpAddress]] belongs to this [[IpNetwork]] object.
    *
    * @param ipAddress an IpAddress object
    * @return True if input IpAddress belongs to this Network, False otherwise.
    */
  def contains(ipAddress: IpAddress): Boolean = {
    if (this.version != ipAddress.version) {
      false
    } else {
      val shiftWidth = this.ipAddr.width - this.mask
      val thisNet = this.ip.numerical >> shiftWidth // flush the host bits
      val thatNet = ipAddress.numerical >> shiftWidth
      thatNet == thisNet // compare network bits only
    }
  }
}

/** Implements an IP Network.
  *
  * For handling single IP addresses, see Ipv4 class.
  */
object IpNetwork {

  private val fwdSlashStr = "/"

  /** Creates an [[IpNetwork]] from an IpAddress and prefix.
    *
    * @param address  An IpAddress object
    * @param mask     Subnet prefix number
    * @return IpNetwork if the input is valid.
    * @throws IpaddrException if the input address is invalid
    */
  def apply(address: IpAddress, mask: Int): IpNetwork = apply(address.toString, mask)

  /** Creates an [[IpNetwork]] object from a numeric network address and prefix.
    *
    * @param address  Long equivalent of a network address
    * @param mask     Subnet prefix number
    * @return IpNetwork if the input is valid.
    * @throws IpaddrException if the input address is invalid
    */
  def apply(address: Long, mask: Int): IpNetwork = apply(IpAddress(address), mask)

  /** Creates an IpNetwork object from a network address and a network prefix
    *
    * @param address  Dot-delimited string containing network address
    * @param mask     Subnet prefix number
    * @return IpNetwork if input is valid, otherwise an IpaddrException.
    * @throws IpaddrException if the input address is invalid
    */
  def apply(address: String, mask: Int): IpNetwork = new IpNetwork(address, mask, Ipv4.version)

  /** Creates an [[IpNetwork]] object from CIDR address like `10.2.1.0/24`
    *
    * @param address  Dot-delimited CIDR string representation of a network address
    * @return IpNetwork if input is valid
    * @throws IpaddrException if the input address is invalid
    */
  @throws(classOf[IpaddrException])
  def apply(address: String): IpNetwork = {
    val res = parseIpNetwork(address)
    if (res.isEmpty) {
      throw new IpaddrException(IpaddrException.invalidAddress(address))
    }
    new IpNetwork(res.get._1, res.get._2, Ipv4.version)
  }

  /** Converts a dot-delimited string to meaningful network address.
    *
    * @param address Dot-delimited string representation of a network address
    * @return An Option containing 2-Tuple(Network address CIDR string, Integer Mask).
    *         None is returned if the input cannot be properly parsed.
    */
  private def parseIpNetwork(address: String): Option[(String, Int)] = {
    Ipv4.expandPartialAddress(address.split('/')(0)) match {
      case Some(addrCidr) =>
        val maskArray = address.split('/')
        val mask = if (maskArray.length == 2) {
                     Ipv4.isValidMask(maskArray(1))
                   } else {
                     Some(Ipv4.width) // Assume default mask with all network bits set
                   }
        mask match {
          case Some(m) => Some((addrCidr, m))
          case _ => None
        }
      case _ => None
    }
  }

}
