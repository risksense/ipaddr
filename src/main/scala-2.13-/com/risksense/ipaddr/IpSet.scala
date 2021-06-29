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
import scala.util.hashing.MurmurHash3

/** Represents an unordered collection (set) of IpNetwork elements.
  *
  * @constructor creates a new IpSet.
  * @param networkSeq a sorted sequence of [[IpNetwork]] objects.
  */
case class IpSet private[ipaddr] (networkSeq: IndexedSeq[IpNetwork]) // scalastyle:ignore
  extends SortedSet[IpNetwork] {

  /** Size of this IpSet.
    *
    * The cardinality of this IpSet (based on the number of individual IP addresses including those
    * implicitly defined in networks).
    */
  lazy val volume: Long = networkSeq.map(_.size).sum[Long]

  /** True if the members of the set form a contiguous IP address range (with no gaps),
    * False otherwise.
    */
  lazy val isContiguous: Boolean = {
    if (networkSeq.size > 1) {
      val networkPairs = networkSeq.sliding(2)
      networkPairs.forall { netSeq => (netSeq(0).last + 1).equals(netSeq(1).first) }
    } else {
      true
    }
  }

  /** [[IpRange]] representation of this IpSet.
    *
    * IpRange equivalent to this IpSet only if all its members form a single contiguous sequence,
    * otherwise generates a IpaddrException.
    *
    * @note This can be resource intensive if the address space is huge.
    */
  @throws(classOf[IpaddrException])
  lazy val ipRange: IpRange = {
    if (isContiguous) {
      if (networkSeq.isEmpty) {
        throw new IpaddrException("Cannot create IpRange from an empty IpSet.")
      } else {
        val ip1 = IpAddress(networkSeq.head.allHosts.head)
        val ip2 = IpAddress(networkSeq.last.allHosts.last)
        new IpRange(ip1, ip2)
      }
    } else {
      throw new IpaddrException(
        "The input IpRange does not represent a single contiguous sequence of addresses")
    }
  }

  /** HashCode of this IpSet calculated over hashCodes of all [[IpNetwork]] objects in this IpSet.
    */
  override val hashCode: Int = MurmurHash3.orderedHash(networkSeq.map(_.hashCode))

  final implicit def ordering: Ordering[IpNetwork] = Ordering[IpNetwork]

  /** String representation of this IpSet.
    *
    * @return a String consisting of addresses in this IpSet separated by `, `
    */
  override def toString(): String = s"IpSet(${networkSeq.mkString(", ")})"

  /** Returns an empty IpSet.
    *
    * @return a new IpSet object with empty sequence `cidrs`.
    */
  override def empty: IpSet = IpSet()

  /** Override `equals`.
    *
    * Compares this IpSet with another IpSet for equality.
    *
    * @param other an IpSet object to compare with
    * @return True if IpSet objects are equal, False otherwise
    */
  override def equals(other: Any): Boolean = other match {
    case that: IpSet => that.canEquals(this) && networkMatch(that)
    case _ => false
  }

  /** Checks if that is same instance of this IpSet.
    *
    * @param other an Object
    * @return True if both objects are instances of IpSet class, False otherwise.
    */
  def canEquals(other: Any): Boolean = other.isInstanceOf[IpSet]

  /** Checks if every IpNetwork in this [[IpSet]] is present in that IpSet and vice-versa.
    *
    * @param that an IpSet to match this IpSet with
    * @return True if all networks match, False otherwise.
    */
  private def networkMatch(that: IpSet): Boolean = {
    val thisHashSeq = for { n <- this.networkSeq } yield n.hashCode
    val thatHashSeq = for { n <- that.networkSeq } yield n.hashCode
    val thisHashSet = thisHashSeq.toSet
    val thatHashSet = thatHashSeq.toSet
    thisHashSet.equals(thatHashSet)
  }

  /** Add an IpAddress to this IpSet.
    *
    * Adds an [[IpAddress]] to this IpSet and returns a new IpSet. Where possible the input
    * IpAddress is merged with other Networks of this set to form more concise blocks.
    *
    * @param that an IpAddress object.
    * @return an IpSet resulting from addition of `that` IpAddress to `this`
    */
  def +(that: IpAddress): IpSet = { // scalastyle:ignore method.name
    val newNetwork = IpNetwork(that, that.width)
    this + newNetwork
  }

  /** Add a IpNetwork to this IpSet
    *
    * Adds a [[IpNetwork]] to this IpSet and returns a new IpSet. Where possible the input Network
    * is merged with other Networks of `this` set to form more concise blocks.
    *
    * @param that a IpNetwork object.
    * @return an IpSet resulting from addition of `that` IpNetwork to `this`
    */
  def +(that: IpNetwork): IpSet = { // scalastyle:ignore method.name
    if (this.contains(that)) {
      this
    } else {
      val newNetworkSeq: Seq[IpNetwork] = this.networkSeq :+ that
      IpSet(newNetworkSeq)
    }
  }

  /** Add an IpRange to this IpSet
    *
    * Adds an [[IpRange]] to this IpSet and returns a new IpSet. Where possible the addresses from
    * input IpRange are merged with other Networks of this set to form more concise blocks.
    *
    * @param that an IpRange object.
    * @return an IpSet resulting from addition of `that` IpRange to `this`
    */
  def +(that: IpRange): IpSet = { // scalastyle:ignore method.name
    // Create a new IpSet from input IpRange
    val newSet = IpSet(that)
    // Perform union with this IpSet
    val unionResult = this | newSet
    // Return the result of union as a new IpSet
    IpSet(unionResult)
  }

  /** Remove IpAddress
    *
    * Removes an [[IpAddress]] from this IpSet and returns a new IpSet.
    *
    * @param that an IpAddress to remove from this IpSet.
    * @return a new IpSet
    */
  def -(that: IpAddress): IpSet = { // scalastyle:ignore method.name
    val net = IpNetwork(that, that.width)
    this - net
  }

  /** Remove Network
    *
    * Removes a [[IpNetwork]] from this IpSet and returns a new IpSet.
    *
    * @param that the IpNetwork object to remove from this IpSet.
    * @return a new IpSet
    */
  def -(that: IpNetwork): IpSet = { // scalastyle:ignore method.name
    val (matched, unmatched) = networkSeq.partition(_.contains(that))
    if (matched.isEmpty) {
      this
    } else {
      val newNetworks = BaseIp.cidrExclude(matched.head, that)
      IpSet(newNetworks ++ unmatched)
    }
  }

  /** Removes an [[IpRange]] from this IpSet and returns a new IpSet.
    *
    * @param that an IpRange to remove from this IpSet.
    * @return a new IpSet
    */
  def -(that: IpRange): IpSet = { // scalastyle:ignore method.name
    // Create a new IpSet from input IpRange
    val thatSet = IpSet(that)
    val diffResult = this.diff(thatSet)
    IpSet(diffResult.toSeq)
  }

  def iterator: Iterator[IpNetwork] = {
    this.networkSeq.iterator
  }

  def keysIteratorFrom(start: IpNetwork): Iterator[IpNetwork] = {
    val matchFoundAt = this.networkSeq.indexWhere(_ >= start)
    if (matchFoundAt < 0) {
      Nil.iterator
    } else {
      this.networkSeq.drop(matchFoundAt).iterator
    }
  }

  def rangeImpl(from: Option[IpNetwork], until: Option[IpNetwork]): IpSet = {
    val beginIndex = from match {
      case Some(x) => this.networkSeq.indexWhere(_ >= x)
      case _ => 0
    }
    if (beginIndex < 0) {
      IpSet(Nil)
    } else {
      val endIndex = until match {
        case Some(x) => this.networkSeq.indexWhere(_ >= x, beginIndex)
        case _ => this.networkSeq.length
      }
      if (endIndex < 0) {
        val newSeq = this.networkSeq.slice(beginIndex, this.networkSeq.length)
        IpSet(newSeq)
      } else {
        val newSeq = this.networkSeq.slice(beginIndex, endIndex)
        IpSet(newSeq)
      }
    }
  }

  /** Checks if this IpSet is less than another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is less than `that`, False otherwise.
    */
  def <(that: IpSet): Boolean = { // scalastyle:ignore method.name
    (this.volume < that.volume) && this.subsetOf(that)
  }

  /** Checks if this IpSet is less than or equal to another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is less than or equal to `that`, False otherwise.
    */
  def <=(that: IpSet): Boolean = this.subsetOf(that) // scalastyle:ignore method.name

  /** Checks if this IpSet is greater than another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is greater than `that`, False otherwise.
    */
  def >(that: IpSet): Boolean = { // scalastyle:ignore method.name
    (this.volume > that.volume) && this.supersetOf(that)
  }

  /** Checks if this IpSet is greater than or equal to another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is greater than or equal to `that`, False otherwise.
    */
  def >=(that: IpSet): Boolean = this.supersetOf(that) // scalastyle:ignore method.name

  /** Checks if this IpSet is superset of another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is superset of `that`, False otherwise.
    */
  def supersetOf(that: IpSet): Boolean = that.subsetOf(this)

  /** Checks if this IpSet is subset of another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` is subset of `that`, False otherwise.
    */
  def subsetOf(that: IpSet): Boolean = this.networkSeq.forall(that.contains(_))

  /** Test if a given [[IpAddress]] is present in this IpSet.
    *
    * @param ipAddress an IpAddress object
    * @return True if the input IpAddress is present in this IpSet, False otherwise.
    */
  def contains(ipAddress: IpAddress): Boolean = contains(IpNetwork(ipAddress, ipAddress.width))

  /** Test if a given [[IpNetwork]] is present in this IpSet.
    *
    * @param network a IpNetwork object
    * @return True if input IpNetwork is present in this IpSet, False otherwise.
    */
  def contains(network: IpNetwork): Boolean = this.exists(_.contains(network))

  /** Checks if this IpSet has nothing in common with another IpSet.
    *
    * @param that an IpSet
    * @return True if `this` and `that` have no common elements, False otherwise.
    */
  def isDisjoint(that: IpSet): Boolean = this.intersect(that).isEmpty

  /** Symmetric difference
    *
    * Returns an IpSet containing networks that appear in either `this` or `that` but not in both.
    *
    * @param that an IpSet
    * @return an IpSet resulting from symmetric difference
    */
  def ^(that: IpSet): IpSet = this.symmetricDiff(that) // scalastyle:ignore method.name

  /** Symmetric difference
    *
    * Returns an IpSet containing networks that appear in either `this` or `that` but not in both.
    *
    * @param that an IpSet
    * @return an IpSet resulting from symmetric difference
    */
  def symmetricDiff(that: IpSet): IpSet = {
    val common = this.intersect(that)
    val all = this | that
    val res = all.diff(common)
    IpSet(res.toSeq)
  }

  /** Intersection of `this` IpSet and `that`
    *
    * @param that an IpSet to perform intersection with
    * @return an IpSet containing all networks that are common between `this` IpSet and `that`.
    */
  def intersect(that: IpSet): IpSet = {
    val thisNets = this.networkSeq
    val thatNets = that.networkSeq

    /* Recursively find common networks
     *
     * @param s1 `this` IpSets network sequence
     * @param s2 `that` IpSets network sequence
     * @param result a sequence of IpNetwork objects common in `this` and `that`
     */
    @tailrec
    def intersectRecurse(
        s1: Seq[IpNetwork],
        s2: Seq[IpNetwork],
        result: Seq[IpNetwork]): Seq[IpNetwork] = {
      if (s1.isEmpty || s2.isEmpty) {
        result
      } else {
        if (s1.head == s2.head) {
          intersectRecurse(s1.drop(1), s2.drop(1), result :+ s1.head)
        } else if (s1.head.contains(s2.head)) {
          intersectRecurse(s1, s2.drop(1), result :+ s2.head)
        } else if (s2.head.contains(s1.head)) {
          intersectRecurse(s1.drop(1), s2, result :+ s1.head)
        } else {
          if (s1.head < s2.head) {
            intersectRecurse(s1.drop(1), s2, result)
          } else {
            intersectRecurse(s1, s2.drop(1), result)
          }
        }
      }
    }

    val commonNets = intersectRecurse(thisNets, thatNets, Nil)
    IpSet(commonNets)
  }

  /** Intersection of `this` IpSet and `that`
    *
    * @param that an IpSet to perform intersection with
    * @return an IpSet containing common networks between `this` and `that`.
    */
  def &(that: IpSet): IpSet = this.intersect(that) // scalastyle:ignore method.name

  /** Union of `this` IpSet with `that`
    *
    * @param that an IpSet to perform union with
    * @return an IpSet containing all elements from `this` and `that` and no duplicates.
    */
  def |(that: IpSet): IpSet = this.union(that) // scalastyle:ignore method.name

  /** Union of `this` IpSet with `that`
    *
    * @param that an IpSet to perform union with
    * @return an IpSet containing all elements from `this` and `that` and no duplicates.
    */
  def union(that: IpSet): IpSet = {
    val res = super.union(that)
    IpSet(res.toSeq)
  }

}

/** Contains various methods to facilitate creation of IpSet */
object IpSet {

  /** Creates an empty IpSet */
  def apply(): IpSet = new IpSet(Nil.toIndexedSeq)

  /** Creates a new IpSet from the given [[IpNetwork]]
    *
    * @param network a IpNetwork object
    * @return an IpSet
    */
  def apply(network: IpNetwork): IpSet = IpSet(Seq(network))

  /** Creates a new IpSet from the given [[IpRange]]
    *
    * @param ipRange an IpRange object
    * @return an IpSet
    */
  def apply(ipRange: IpRange): IpSet = IpSet(ipRange.cidrs)

  /** Creates a new IpSet from another IpSet.
    *
    * @param ipSet an IpSet object
    * @return an IpSet
    */
  def apply(ipSet: IpSet): IpSet = IpSet(ipSet.networkSeq.asInstanceOf[Seq[IpNetwork]])

  /** Creates a new IpSet from a sequence of [[IpNetwork]] objects.
    *
    * @param netSeq a sequence of IpNetwork objects
    * @return an IpSet
    */
  def apply(netSeq: Seq[IpNetwork]): IpSet = {
    if (netSeq.isEmpty) {
      apply()
    } else {
      val mergedNetworks = BaseIp.cidrMerge(netSeq).toIndexedSeq
      new IpSet(mergedNetworks.sorted)
    }
  }

}
