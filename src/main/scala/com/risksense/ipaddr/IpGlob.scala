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

import scala.annotation.tailrec

/** Represents an IP address range using a glob-style syntax ``x.x.x-y.*``
  *
  * Individual octets can be represented using the following shortcuts :<br>
  * 1. ``*`` - the asterisk octet (represents values ``0`` through ``255``)
  * <br>
  * 2. ``x-y`` - the hyphenated octet (represents values ``x`` through ``y``)
  * <br>A few basic rules also apply :
  * <ol><li>``y`` must always be greater than ``x``, therefore :
  * <ul><li>``x`` can only be ``0`` through ``254``</li>
  * <li>``y`` can only be ``1`` through ``255``</li></ul></li>
  * <li>only one hyphenated octet per IP glob is allowed</li>
  * <li>only asterisks are permitted after a hyphenated octet</li></ol>
  * <br><br><u>Examples:</u><br>
  * +------------------+------------------------------+<br>
  * | IP glob          | Description                  |<br>
  * +==================+==============================+<br>
  * | ``192.0.2.1``    | a single address             |<br>
  * +------------------+------------------------------+<br>
  * | ``192.0.2.0-31`` | 32 addresses                 |<br>
  * +------------------+------------------------------+<br>
  * | ``192.0.2.*``    | 256 addresses                |<br>
  * +------------------+------------------------------+<br>
  * | ``192.0.2-3.*``  | 512 addresses                |<br>
  * +------------------+------------------------------+<br>
  * | ``192.0-1.*.*``  | 131,072 addresses            |<br>
  * +------------------+------------------------------+<br>
  * | ``*.*.*.*``      | the whole IPv4 address space |<br>
  * +------------------+------------------------------+<br>
  * <br><b>Note:</b> <br>
  * IP glob ranges are not directly equivalent to CIDR blocks.
  * They can represent address ranges that do not fall on strict bit mask boundaries.
  * They are suitable for use in configuration files, being more obvious and readable than their
  * CIDR counterparts, especially for admins and end users with little or no networking knowledge
  * or experience. <i>All CIDR addresses can always be represented as IP globs but the reverse
  * is not always true.</i>
  */
object IpGlob {

  private val Wildcard = "*"
  private val octet1Regex = """(25[0-4]|2[0-4][0-9]|[01]?[0-9][0-9]?)"""
  private val octet2Regex = """(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"""
  private val octetRegex = s"$octet1Regex-$octet2Regex".r

  /** Converts IpGlob to [[IpRange]]
    *
    * @param ipGlob an IP address range in glob-style format
    * @return an IpRange equivalent to the input glob, if valid.
    */
  def globToIpRange(ipGlob: String): IpRange = {
    if (validGlob(ipGlob)) {
      val globArray = splitAddress(ipGlob)
      val range = globArray.map(getBounds(_))
      val (startIp, endIp) = range.unzip
      new IpRange(IpAddress(mergeStringSeq(startIp)), IpAddress(mergeStringSeq(endIp)))
    } else {
      throw new IpaddrException(IpaddrException.invalidGlobAddress(ipGlob))
    }
  }

  /** Convert a network address to glob representation.
    *
    * @param s a network address in CIDR notation
    * @return a sequence of one or more blob strings if the input is valid, IpError otherwise
    */
  def cidrToGlob(s: String): Seq[String] = {
    val net = IpNetwork(s)
    val globs = ipRangeToGlobs(Ipv4.isValidAddress(net.first).get,
      Ipv4.isValidAddress(net.last).get)
    // There should only ever be a one to one mapping between a CIDR and an IP glob range.
    if (globs.length != 1) {
      throw new IpaddrException(IpaddrException.invalidAddress(s))
    }
    globs
  }

  /** Convert IP range to glob style addresses
    *
    * Accepts an arbitrary start and end addresses and returns its equivalent sequence of
    * glob-style addresses.
    *
    * @param start a dot-delimited address
    * @param end   a dot-delimited address
    * @return a sequence of one or more glob-style addresses if input is valid, IpError otherwise
    */
  def ipRangeToGlobs(start: String, end: String): Seq[String] = {
    ipRangeToGlobs(IpAddress(start), IpAddress(end))
  }

  /** Convert IP range to glob style addresses
    *
    * Accepts an arbitrary start and end [[IpAddress]]es and returns its equivalent containing one
    * or more glob-style addresses.
    *
    * @param start an IpAddress
    * @param end   an IpAddress
    * @return a sequence of one or more glob-style addresses if input is valid.
    */
  def ipRangeToGlobs(start: IpAddress, end: IpAddress): Seq[String] = {
    if (start.version != Ipv4.version && start.version != end.version) {
      throw new IpaddrException(IpaddrException.versionMismatch)
    }
    val glob = toGlob(start.toString, end.toString)
    if (validGlob(glob)) {
      Seq(glob)
    } else {
      // This is a workaround. It produces non-optimal but valid glob conversions.
      // Break input range into CIDRs before conversion to globs.
      val cidrs = BaseIp.ipRangeToCidrs(IpNetwork(start, end.width), IpNetwork(end, end.width))
      val res = for { c <- cidrs } yield {
        val lb = Ipv4.isValidAddress(c.first).get
        val ub = Ipv4.isValidAddress(c.last).get
        toGlob(lb, ub)
      }
      res
    }
  }

  /** Construct globs from lower and upper bound addresses.
    *
    * @param lowerAddress  Lower-bound dot-delimited address
    * @param upperAddress  Upper-bound dot-delimited address
    * @return a glob-style address containing `lowerAddress` and `upperAddress`
    */
  private def toGlob(lowerAddress: String, upperAddress: String): String = {
    val addr1 = splitAddress(lowerAddress).map(BaseIp.StringToInt)
    val addr2 = splitAddress(upperAddress).map(BaseIp.StringToInt)
    val words = addr1.zip(addr2).map { x =>
      if (x._1 == x._2) {
        // a normal octet
        x._1.toString
      } else if (x._1 == 0 && x._2 == 255) {
        // an asterisk octet
        this.Wildcard
      } else {
        // create a hyphenated octet
        raw"${x._1}-${x._2}"
      }
    }
    mergeStringSeq(words)
  }

  /** Convert glob to Network.
    *
    * Accepts a glob-style address and returns a sequence of [[IpNetwork]] objects that exactly
    * match it.
    *
    * @param ipGlob a glob-style address
    * @return a sequence of Network objects that exactly match the input.
    * @throws IpaddrException if an error occurs.
    */
  def globToCidrs(ipGlob: String): Seq[IpNetwork] = {
    val glob = globToIpTuple(ipGlob)
    BaseIp.ipRangeToCidrs(IpNetwork(glob._1, glob._1.width), IpNetwork(glob._2, glob._2.width))
  }

  /** Convert IpGlob to a pair of [[IpAddress]] objects.
    *
    * Accepts a glob-style IP range and returns the component lower and upper bound IpAddress.
    *
    * @param ipGlob an IP address range in glob-style format string
    * @return a tuple containing lower and upper bound IpAddress
    */
  def globToIpTuple(ipGlob: String): (IpAddress, IpAddress) = {
    if (validGlob(ipGlob)) {
      val globArray = splitAddress(ipGlob)
      val range = globArray.map(getBounds (_))
      val (startIp, endIp) = range.unzip
      (IpAddress(mergeStringSeq(startIp)), IpAddress(mergeStringSeq(endIp)))
    } else {
      throw new IpaddrException(IpaddrException.invalidGlobAddress(ipGlob))
    }
  }

  /** Check if glob is valid.
    *
    * @param ipGlob an IP address range in glob-style string format
    * @return True if input glob is valid, False otherwise
    */
  def validGlob(ipGlob: String): Boolean = {
    val globSeq = ipGlob.split('.').toSeq

    @tailrec
    def validGlobRecurse(octetSeq: Seq[String], check: (Boolean, Boolean, Boolean)): Boolean = {
      if (octetSeq.isEmpty || !check._1) {
        check._1
      } else {
        val newCheck = isValidOctet(octetSeq.head, check._2, check._3)
        validGlobRecurse(octetSeq.drop(1), newCheck)
      }
    }

    if (globSeq.length != 4) {
      false
    } else {
      validGlobRecurse(globSeq, (true, false, false))
    }
  }

  /** Check if an octet or octet range is valid.
    *
    * @param octet         a string representing an octet or octet range e.g. 255, 1-12
    * @param seenHyphen    denotes if the input octet contains a `-`
    * @param seenAsterisk  denotes if the input octet contains a `*`
    * @return A 3-tuple. The first value denotes if input was valid, second value denotes if the
    *         input contained a `-`, and third boolean is True if this octet is a wildcard `*`.
    */
  private def isValidOctet(
      octet: String,
      seenHyphen: Boolean,
      seenAsterisk: Boolean): (Boolean, Boolean, Boolean) = octet match {
    case octetRegex(o1, o2) =>
      val octet1 = Integer.parseInt(o1)
      val octet2 = Integer.parseInt(o2)
      if (octet1 <= octet2 && octet2 >= 1 && !seenHyphen && !seenAsterisk) {
        (true, true, false)
      } else {
        (false, true, false)
      }
    case octet2Regex.r(_*) => (!seenHyphen && !seenAsterisk, false, false)
    case Wildcard => (true, false, true)
    case _ => (false, false, false)
  }

  /** Extract bounds from a glob-style octet representation.
    *
    * @example 2-8 = (2, 8) <br/>
    * * = (0, 255) <br/>
    * 4 = (4, 4)
    *
    * @param s an octet
    * @return a tuple(lower, upper) value of input octet
    */
  private def getBounds(s: String): (String, String) = {
    if (s.contains('-')) {
      val bounds = s.split('-')
      (bounds(0), bounds(1))
    } else if (s == Wildcard) {
      ("0", "255")
    } else {
      (s, s)
    }
  }

  /** Combine a sequence of string elements with period.
    *
    * @param s a sequence of strings
    * @return A single string element generated by combining input sequence elements with a '.'
    * @example Seq(10, 2, 0, 12) = "10.2.0.12"
    */
  private def mergeStringSeq(s: Seq[String]): String = s.mkString(".")

  /** Converts an IP address string to a sequence of words.
    *
    * @param s a dot-delimited IP address
    * @return a sequence of words
    * @example "10.2.0.12" = Seq(10, 2, 0, 12)
    */
  private def splitAddress(s: String): Seq[String] = s.split('.').toSeq

}
