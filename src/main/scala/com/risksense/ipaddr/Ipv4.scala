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

/** Handles individual IPv4 addresses.
  *
  * Implements and overrides some methods from IpAddress trait.
  *
  * @constructor creates Ipv4 object
  * @param addr Dot-delimited version 4 IP address
  */
case class Ipv4 private[ipaddr](addr: String) extends IpAddress {

  lazy val binary: String = bits()
  val width: Int = Ipv4.width
  val wordSize: Int = Ipv4.wordSize
  val delimiter: String = Ipv4.wordSep
  val familyName = "IPv4"
  val version: Int = Ipv4.version
  val words: Seq[Int] = addr.split('.').map(BaseIp.StringToInt).toSeq

  def bits(d: String = ""): String = numToBinary(numerical, d)

  def isLinkLocal: Boolean = Ipv4.Ipv4LinkLocal.contains(this)

  def isLoopback: Boolean = Ipv4.Ipv4Loopback.contains(this)

  def isUnicast: Boolean = !isMulticast

  def isMulticast: Boolean = Ipv4.Ipv4Multicast.contains(this)

  def isPrivate: Boolean = Ipv4.Ipv4Private.exists(_.contains(this))

  def isReserved: Boolean = Ipv4.Ipv4Reserved.exists(_.contains(this))

  override def toString: String = addr
}

/** Defines regular expressions and methods that are used for validation. */
object Ipv4 {

  private val Octet = """(\d{1,3})"""
  /** Matches a netmask octet <u>excluding</u> the value 0. */
  private val MaskOctetMatch = "(254|252|248|240|224|192|128)"
  /** Matches a netmask octet <u>including</u> the value 0. */
  private val MaskOctet2Match = "(254|252|248|240|224|192|128|0)"
  private val IP4Address = """(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})"""

  /** Loopback network address */
  lazy val Ipv4Loopback = IpNetwork("127.0.0.0/8")
  /** Sequence of private Ipv4 networks */
  lazy val Ipv4Private = Seq(
    IpNetwork("10.0.0.0/8"),
    IpNetwork("172.16.0.0/12"),
    IpNetwork("192.168.0.0/16"),
    IpNetwork("192.0.2.0/24")) // Test-Net
  /** Ipv4 link local address */
  lazy val Ipv4LinkLocal = IpNetwork("169.254.0.0/16")
  /** Multicast Ipv4 network */
  lazy val Ipv4Multicast = IpNetwork("224.0.0.0/4")
  /** Sequence of reserved network addresses */
  lazy val Ipv4Reserved = Seq(
    // Reserved but subject to allocation
    IpNetwork("192.0.0.0/24"),
    // Reserved for Future Use
    IpNetwork("240.0.0.0/4"))
  /** IP version */
  val version = 4
  /** Bit length for this IP */
  val width = 32
  /** Separator for Ipv4 addresses */
  val wordSep = "."
  /** Bit length of a word */
  val wordSize = 8

  // Network("239.0.0.0/239.255.255.255")
  /** Number of words */
  private val wordCount = width / wordSize
  /** Maximum numerical value for this IP */
  private val maxNumerical = (scala.math.pow(2, this.width) - 1).toLong
  /** Maximum word value for this IP */
  private val maxWord = 255

  /** Regex to match a mask as a numerical value, e.g. 24 */
  private val Mask1Regex = """(\d|1\d|2\d|3[0-2])""".r
  /** Regex to match a dot-delimited mask, e.g. 255.255.0.0 */
  private val Mask2Regex = ("((" + MaskOctetMatch + "\\.0\\.0\\.0)" +
    "|(255\\." + MaskOctet2Match + "\\.0\\.0)" +
    "|(255\\.255\\." + MaskOctet2Match + "\\.0)" +
    "|(255\\.255\\.255\\." + MaskOctet2Match + "))").r
  private val Net4Regex = IP4Address.r

  private val Net1Regex = Octet.r

  private val Net2Regex = (Octet + """\.""" + Octet).r // scalastyle:ignore multiple.string.literals
  private val Net3Regex = (Octet + """\.""" + Octet + """\.""" + Octet).r

  /** Checks if a Long value represents a valid Ipv4 address.
    *
    * @param numerical A Long value
    * @return An option containing the dot-delimited string representing the Ipv4 address equivalent
    *         of the input long value if the address is valid, None otherwise.
    */
  def isValidAddress(numerical: Long): Option[String] = {
    if (numerical >= 0 && numerical <= maxNumerical) {
      val res: Seq[Int] = for { i <- 1 to wordCount } yield {
        ((numerical >> (wordSize * (wordCount - i))) & 0xFF).toInt
      }
      Some(res.mkString(this.wordSep))
    } else {
      None
    }
  }

  /** Checks if a dot-delimited string represents a valid Ipv4 address.
    *
    * @param address A dot-delimited string representing an Ipv4 address
    * @return True if address is valid, False otherwise
    * @example isValidAddress("10.0.1.2") => true <br/>
    * isValidAddress("192.168.0") => false
    */
  def isValidAddress(address: String): Boolean = address match {
    case Net4Regex(o1, o2, o3, o4) =>
      val res = List(o1, o2, o3, o4)
                  .map(BaseIp.StringToInt)
                  .count { n => n < 0 || n > maxWord }
      res == 0
    case _ => false
  }

  /** Checks if the numerical equivalent of a netmask is valid.
    *
    * @param maskNum Numerical representation of a netmask
    * @return True if mask is valid, False otherwise
    */
  def isValidMask(maskNum: Int): Boolean = maskNum >= 0 && maskNum <= this.width

  /** Checks if the mask is valid and counts the number of network bits.
    *
    * @param mask A dot-delimited string representing a network mask
    * @return An option containing the number of network bits if the mask is valid, None otherwise
    */
  def isValidMask(mask: String): Option[Int] = mask match {
    case Mask1Regex(_*) => Some(Integer.parseInt(mask))
    case Mask2Regex(_*) =>
      val maskBinArray = mask.split('.')
                             .map(Integer.parseInt(_).toBinaryString)
      Some(maskBinArray.mkString.count(_ == '1'))
    case _ => None
  }

  /** Complete partial IP address.
    *
    * Expands a partial IPv4 address into a full 4-octet version and also checks the validity of
    * the expanded address.
    *
    * @param partialAddress a partial or abbreviated IPv4 address
    * @return an expanded IP address in presentation format (x.x.x.x)
    *
    * @example 192 -> returns Some("192.0.0.0") because the result is valid address <br/>
    * 256 -> returns None because address `256.0.0.0` is invalid
    */
  def expandPartialAddress(partialAddress: String): Option[String] = {
    val completeAddress = partialAddress match {
      case Net1Regex(_*) => Some(partialAddress + ".0.0.0")
      case Net2Regex(_*) => Some(partialAddress + ".0.0")
      case Net3Regex(_*) => Some(partialAddress + ".0")
      case Net4Regex(_*) => Some(partialAddress)
      case _ => None
    }
    completeAddress match {
      case Some(addr) if isValidAddress(addr) => Some(addr)
      case _ => None
    }
  }

}
