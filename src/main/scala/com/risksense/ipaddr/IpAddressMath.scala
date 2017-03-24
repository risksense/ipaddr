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

// scalastyle:off method.name

/** Defines several mathematical operations on an IpAddress object. These include
  * +, -, <<, >>, &amp;, or, xor.
  */
trait IpAddressMath {

  this: IpAddressFormat =>

  /** Add to IP address.
    *
    * Increases the numerical value of this IPAddress by 'n' and returns a new IpAddress object.
    * An instance of IpError is returned if the result exceeds maximum IP address value or is less
    * than zero.
    *
    * @param n size of IP address increment.
    * @return new IpAddress object with incremented address.
    */
  def +(n: Int): IpAddress = IpAddress(this.numerical + n)

  /** Subtract from IP address
    *
    * Decreases the numerical value of this IPAddress by n and returns a new IpAddress object.
    * An IndexError is raised if result exceeds maximum IP address value or is less than zero.
    *
    * @param n size of IP address decrement.
    * @return new IpAddress object with decremented address.
    */
  def -(n: Int): IpAddress = IpAddress(this.numerical - n)

  /** Right shifts numerical value of this IP address and returns a new IpAddress object.
    *
    * @param numbits Size of bitwise shift.
    * @return an IPAddress object based on this one with its integer value right shifted by numbits.
    */
  def >>(numbits: Int): IpAddress = IpAddress(this.numerical >> numbits)

  /** Left shifts numerical value of this IP address and returns a new IpAddress object.
    *
    * @param numbits Size of bitwise shift.
    * @return an IPAddress object based on this one with its integer value left shifted by numbits.
    */
  def <<(numbits: Int): IpAddress = IpAddress(this.numerical << numbits)

  /** Bitwise AND operation between two IpAddress objects.
    *
    * @param ipAddress An IpAddress object.
    * @return bitwise exclusive AND (x&amp;y) between the numerical value of this IP address and 'i'
    */
  def &(ipAddress: IpAddress): IpAddress = IpAddress(this.numerical & ipAddress.numerical)

  /** Bitwise OR operation between two IpAddress objects.
    *
    * @param ipAddress An IpAddress object.
    * @return bitwise OR (x|y) between the numerical value of this IP address and 'ipAddress'.
    */
  def |(ipAddress: IpAddress): IpAddress = IpAddress(this.numerical | ipAddress.numerical)

  /** Bitwise XOR operation between two IpAddress objects.
    *
    * @param ipAddress An IPAddress object.
    * @return bitwise ExclusiveOR between the numerical value of this IP address and 'ipAddress'.
    * */
  def ^(ipAddress: IpAddress): IpAddress = IpAddress(this.numerical ^ ipAddress.numerical)

}
