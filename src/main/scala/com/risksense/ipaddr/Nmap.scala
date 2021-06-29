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
import scala.collection.SortedSet

/** Contains methods for translating and operating on Nmap addresses. */
object Nmap extends {

  /** Checks if nmap address is valid.
    *
    * @param s an nmap-style address
    * @return True if input nmap address is valid, False otherwise.
    */
  def validNmapRange(s: String): Boolean = generateNmapOctetRanges(s).nonEmpty

  /** Expands nmap addresses to IpAddress.
    *
    * The nmap security tool supports a custom type of IPv4 range using multiple hyphenated octets.
    * This method generates a non-strict sequence of [[IpAddress]]es resulting from expansion of the
    * input nmap address.
    *
    * @param s      An nmap-style target specification
    * @param limit  Should the addresses be limited to a single class A
    * @return A non-strict sequence of IPAddress objects.
    */
  def iterNmapRange(s: String, limit: Boolean = true): Iterator[IpAddress] = {
    val addresses = generateNmapOctetRanges(s)
    if (addresses.isEmpty) {
      throw new IpaddrException("Cannot iterate over empty address sequence.")
    } else if (limit && addresses.head.length > 1) {
      throw new IpaddrException("Prohibitive number of addresses generated form the input.")
    } else {
      val iters = addresses.map(_.to(LazyList))
      val (src1, src2, src3, src4) = (iters.head, iters(1), iters(2), iters(3))
      src1.flatMap { w =>
        src2.flatMap { x =>
          src3.flatMap { y => src4.map(z => IpAddress(s"$w.$x.$y.$z")) }
        }
      }.iterator
    }
  }

  /** Generates sequence of values for an individual octet.
    *
    * @param s a string consisting of nmap style addresses separated by `,`
    * @return a sequence of integer words
    */
  private def nmapOctetTargetValues(s: String): Seq[Int] = {
    translateRecurse(s.split(',').toSeq, Some(SortedSet[Int]())) match {
      case Some(x) => x.toSeq
      case _ => Nil
    }
  }

  /** Convert nmap word into a tuple with lower and upper bounds
    *
    * @param n an nmap style word
    * @return a tuple(lower bound, upper bound)
    */
  private def nmapWordTranslate(n: String): (Int, Int) = {

    /* Convert input strings to tuple(Int, Int)
     *
     * @param s1 a String representation of a word
     * @param s2 a String representation of a word
     * @return a 2 tuple translation of the input to Integer values.
     *         Returns (-1, -1) if at least one input is invalid.
     */
    def toNum(s1: String, s2: String): (Int, Int) = {
      if (isValidWord(s1) && isValidWord(s2)) {
        (Integer.parseInt(s1), Integer.parseInt(s2))
      } else {
        (-1, -1)
      }
    }

    if (n.contains('-')) {
      val arr = n.split('-')
      if (arr.length == 0) {
        (0, 255)
      } else if (arr.length == 1) {
        toNum(arr(0), "255")
      } else {
        toNum(arr(0), arr(1))
      }
    } else if (isValidWord(n)) {
      val num = Integer.parseInt(n)
      (num, num)
    } else {
      (-1, -1)
    }
  }

  /** Check if the input string represents a valid word.
    *
    * Word length should not be more than 3 digits.
    *
    * @param s the input string to validate
    * @return True if the input string represents a word, False otherwise.
    */
  private def isValidWord(s: String): Boolean = {
    if (s.length == 0 || s.length > 3) {
      false
    } else {
      s.forall(_.isDigit)
    }
  }

  /** Recursively translate nmap address to Integer words.
    *
    * @param addrSeq A sequence of nmap style addresses
    * @param result  A set of nmap address translations
    * @return A set of integer words or None if the translation fails
    */
  @tailrec
  private def translateRecurse(
      addrSeq: Seq[String],
      result: Option[SortedSet[Int]]): Option[SortedSet[Int]] = {
    if (addrSeq.nonEmpty) {
      val addr = addrSeq.head
      val (low, high) = nmapWordTranslate(addr)
      if (low < 0 || low > 255 || high < 0 || high > 255 || low > high) {
        // invalid input
        translateRecurse(Nil, None)
      } else {
        translateRecurse(addrSeq.drop(1),
          Some(result.get ++ Range.inclusive(low, high)))
      }
    } else {
      result
    }
  }

  /** Generates sequences of octet values containing all octets defined by the given nmap address.
    *
    * @param s an nmap-style address
    * @return sequences of expanded octets from input address
    */
  private def generateNmapOctetRanges(s: String): Seq[Seq[Int]] = {
    val tokens = s.split('.')
    if (tokens.length != 4) {
      Nil
    } else {
      val res = tokens.map(nmapOctetTargetValues(_)).toSeq
      if (res.contains(Nil)) {
        Nil
      } else {
        res
      }
    }
  }

}
