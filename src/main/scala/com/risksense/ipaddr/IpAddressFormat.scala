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

/** Components required for formatting an IpAddress object such as words, width, address as string,
  * numerical address, etc.
  * Hierarchy: IpAddress < IpAddressMath < IpAddressFormat
  */
trait IpAddressFormat {

  /** Long value equivalent of this IP address. */
  lazy val numerical: Long = wordsToNum(this.words)

  /** The maximum numerical value (Long) that can be represented by this address. */
  lazy val maxNumerical: Long = (scala.math.pow(2, this.width) - 1).toLong

  /** The number of words in this address. */
  lazy val wordCount: Int = this.width / this.wordSize

  /** The maximum numerical value (Int) for an individual word in this address. */
  lazy val maxWord: Int = scala.math.pow(2, this.wordSize).toInt - 1

  /** IP address representation as a binary string. */
  val binary: String

  /** IP address representation as a sequence of Ints.
    *
    * @example 192.168.1.2 = Seq[Int](192, 168, 1, 2).
    */
  val words: Seq[Int]

  /** The width (in bits) of this address. */
  val width: Int

  /** The individual word size (in bits) of this address. */
  val wordSize: Int

  /** The separator string used between each word. */
  val delimiter: String

  /** A user-friendly string name for this address type. */
  val familyName: String

  /** The IP version of this address. */
  val version: Int

  /** Holds a 0 string */
  val Zero: String = "0"

  /** IP address in delimited binary string format.
    *
    * @param d Delimiter string (default = "")
    * @return delimited binary string representation of this IP address
    */
  def bits(d: String = ""): String

  /** Converts this IP address into a hexadecimal string.
    *
    * @return A hexadecimal string representation of this IP address.
    */
  def hex: String = {
    val res = this.words.map { w: Int =>
      val hex = Integer.toHexString(w)
      if (hex.length < 2) {
        this.Zero + hex
      } else {
        hex
      }
    }
    res.mkString
  }

  // $COVERAGE-OFF$

  /** Converts a sequence of Int words to an equivalent long value.
    *
    * @param words A sequence of integer values representing an IP address.
    * @return a Long value equivalent of the input word sequence.
    */
  def wordsToNum(words: Seq[Int]): Long = {
    val res = for { i <- 0 until this.wordCount } yield {
      words(i).toLong << (this.wordSize * (this.wordCount - i - 1))
    }
    res.reduceLeft(_ | _)
  }

  // $COVERAGE-ON$

  /** Convert a delimited binary string representation of an IP address to a numerical equivalent.
    *
    * @param binary     Delimited binary string representation of IP address.
    * @param delimiter  String used to delimit word groups (default is "", no separator).
    * @return An Long value that is equivalent to the input IP address. If the input binary string
    *         is not a valid representation of address, 0 is returned.
    */
  def binaryToNum(binary: String, delimiter: String = ""): Long = {
    if (!isValidBinary(binary, delimiter)) {
      0
    } else {
      val binaryWithoutDelimiter = binary.replaceAll(delimiter, "")
      BigInt(binaryWithoutDelimiter, 2).toLong
    }
  }

  /** Verifies a given binary string against a desired length.
    *
    * @param binary     A network address in a delimited binary string format.
    * @param delimiter  String used to delimit word groups.
    * @return True if network address is valid, False otherwise.
    */
  private def isValidBinary(binary: String, delimiter: String = ""): Boolean = {
    val binaryWithoutDelimiter = binary.replaceAll(delimiter, "")
    if (binaryWithoutDelimiter.length != this.width) {
      false
    } else {
      try {
        val res = BigInt(binaryWithoutDelimiter, 2).toLong
        res >= 0 && res <= this.maxNumerical
      } catch {
        case _: NumberFormatException => false
      }
    }
  }

  /** Converts numerical value of an IP address into binary string notation with custom delimiters.
    *
    * @param ipNum      A Long value representing numerical value of IP address.
    * @param delimiter  String used to delimit word groups. (default is "")
    * @return a network address in a delimited binary string format that is equivalent to ipNum.
    */
  def numToBinary(ipNum: Long, delimiter: String = ""): String = {
    val words = ipNumericalToWords(ipNum)
    (for { w <- words } yield toBinary(w, this.wordSize)).mkString(delimiter)
  }

  /** Converts an IP address integer equivalent to a binary string representation of a specified
    * length.
    *
    * @param ipNum   Integer value representation of an address
    * @param length  Desired length of the resulting binary string
    * @return Binary string of specified length representing value 'ipNum'
    */
  private def toBinary(ipNum: Int, length: Int): String = {
    val binaryString = ipNum.toBinaryString
    if (length <= binaryString.length) {
      binaryString
    } else {
      // Pad zeroes to binaryString to reach the desired length
      binaryString.reverse.padTo(length, this.Zero.head).reverse.mkString
    }
  }

  /** Converts a long equivalent of an IP address to a sequence of Integers.
    *
    * @param ipNum A Long equivalent of an IP address.
    * @return A sequence of integer values that represent octets for this IP.
    */
  def ipNumericalToWords(ipNum: Long): Seq[Int] = for { i <- 1 to this.wordCount } yield {
    ((ipNum >> (this.wordSize * (this.wordCount - i))) & 0xFF).toInt
  }

  /**
    * @return String representation of this IP address.
    */
  def toString: String

  /** Check if this IP address is a hostmask.
    *
    * @return True if this IP address is a hostmask, False otherwise.
    */
  def isHostmask: Boolean = {
    val res = this.numerical + 1
    (res & (res - 1)) == 0
  }

  /** Get the number of network bits.
    *
    * @return The number of network bits. If this IP address is a valid netmask, the number of
    *         non-zero bits are returned, otherwise it returns the width in bits for this IP address
    *         version.
    */
  def netmaskBits: Int = {
    if (!isNetmask) {
      this.width
    } else {
      this.binary.count(_ == '1')
    }
  }

  /** Checks if this IP address is a netmask.
    *
    * @return True if this IP address is a network mask, False otherwise.
    */
  def isNetmask: Boolean = {
    val res = (this.numerical ^ this.maxNumerical) + 1
    (res & (res - 1)) == 0
  }

}
