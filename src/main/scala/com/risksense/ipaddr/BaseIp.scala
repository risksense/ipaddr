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

import java.io.FileNotFoundException

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.typesafe.scalalogging.StrictLogging

/** Defines shared RegExs and methods.
  *
  * Defines regular expressions and methods that are shared among IpAddress,
  * IpNetwork and IpRange classes.
  */
object BaseIp extends StrictLogging {

  /** A helper method to convert string to integer. */
  val StringToInt: String => Int = Integer.parseInt(_: String)

  /** Check if a dot-delimited address string is valid.
    *
    * @param address  Dot-delimited address string
    * @param version  IP version number // TODO PRO-60
    * @return True if address is valid, False otherwise
    */
  def isValidAddress(address: String, version: Int): Boolean = version match {
    case Ipv4.version => Ipv4.isValidAddress(address)
    case _ => false
  }

  /** Check if a numeric address representation is valid.
    *
    * @param addressNum  A Long equivalent of an IP address
    * @param version     IP version number
    * @return An Option containing the dot-delimited string representation if address is valid,
    *         None otherwise.
    */
  def isValidAddress(addressNum: Long, version: Int): Option[String] = version match {
    case Ipv4.version => Ipv4.isValidAddress(addressNum)
    case _ => None
  }

  /** Check if mask is valid.
    *
    * @param mask     Numeric value of the mask
    * @param version  IP version number
    * @return True if the mask is valid, False otherwise
    */
  def isValidMask(mask: Int, version: Int): Boolean = version match {
    case Ipv4.version => Ipv4.isValidMask(mask)
    case _ => false
  }

  /** Create a bounded stream of IpAddresses
    *
    * Stream of IPAddress elements between an arbitrary [start, stop] IP address with intervals of
    * step between them. Stream produced is inclusive of boundary addresses.
    *
    * @param start  Starting IP address
    * @param end    Ending IP address
    * @param step   Size of step between IP addresses(defaults to 1). If you specify a negative
    *               step and if startIp > endIp then a reversed collection is generated.
    * @return A stream of IpAddress objects. An empty stream is returned if the step is 0 or if both
    *         IpAddress have different version.
    */
  def addressStream(start: IpAddress, end: IpAddress, step: Int = 1): Stream[IpAddress] = {
    if ((start.version != end.version) || (step == 0)) {
      Stream()
    } else {
      Range.Long.inclusive(start.numerical, end.numerical, step).toStream.map { ipNum =>
        IpAddress(ipNum)
      }
    }
  }

  /** Accepts a sequence of [[IpNetwork]] objects and returns a single [[IpNetwork]] object that is
    * large enough to span the lower and upper bound IP addresses with a possible overlap on either
    * end.
    *
    * @param nets A sequence of IpNetwork objects. Must contain at least two elements.
    * @return A single spanning IpNetwork object.
    */
  def spanningCidr(nets: Seq[IpNetwork]): IpNetwork = {
    val sortedIps = nets.sorted
    if (sortedIps.length < 2) { // scalastyle:ignore magic.number
      throw new IpaddrException("Input network sequence must contain at least 2 elements")
    }
    val lowestIp = sortedIps.head
    val highestIp = sortedIps.last
    if (lowestIp.ipAddr.version != highestIp.ipAddr.version) {
      throw new IpaddrException(IpaddrException.versionMismatch)
    }
    val (prefix, ipnum) = spanningCidrRecurse(highestIp.mask,
                                              highestIp.last,
                                              lowestIp.first,
                                              highestIp.ipAddr.width)
    IpNetwork(ipnum, prefix)
  }

  /** Creates a bounded sequence of IpNetworks.
    *
    * Accepts an arbitrary start and end IpNetwork and returns a sequence of [[IpNetwork]] objects
    * that fit exactly between the boundaries of the two with no overlap.
    *
    * @param startNetwork  Lower bound IpNetwork
    * @param endNetwork    Upper bound IpNetwork
    * @return A sequence of IpNetwork objects
    */
  def boundedNetworkSeq(startNetwork: IpNetwork, endNetwork: IpNetwork): Seq[IpNetwork] = {
    val cidrSpan = spanningCidr(Seq(startNetwork, endNetwork))
    val width = startNetwork.ipAddr.width
    lazy val exclude0 = IpNetwork(startNetwork.first - 1, width)
    lazy val newNetSeq = cidrPartition(cidrSpan, exclude0)._3
    lazy val exclude1 = IpNetwork(endNetwork.last + 1, width)
    val partitionProc = (newCidrSpan: IpNetwork) => cidrPartition(newCidrSpan, exclude1)._1
    if (cidrSpan.first < startNetwork.first) {
      /* `newNetSeq` stores IpNetwork objects. The last object in this Seq might overlap with the
       * `end` range. Therefore, first read all the elements of newNetSeq into cidrList2 and then
       * check if the `last` element overlaps. If it does, then discard it, otherwise merge with
       * cidrList2.
       */
      val cidrList2 = newNetSeq.init
      val newCidrSpan = newNetSeq.last
      if (newCidrSpan.last > endNetwork.last) {
        cidrList2 ++ partitionProc(newCidrSpan)
      } else {
        cidrList2 :+ newCidrSpan
      }
    } else if (cidrSpan.last > endNetwork.last) {
      partitionProc(cidrSpan)
    } else {
      Seq(cidrSpan)
    }
  }

  /** Removes a [[IpNetwork]] from the target Network.
    *
    * @param target   Network address in CIDR string formart that will be divided up
    * @param exclude  A network address in CIDR string that will be moved from the target network
    * @return Sequence of IpNetwork objects remaining after exclusion.
    */
  def cidrExclude(target: String, exclude: String): Seq[IpNetwork] = {
    val targetNetwork = IpNetwork(target)
    val excludeNetwork = IpNetwork(exclude)
    val (before, _, after) = cidrPartition(targetNetwork, excludeNetwork)
    before ++ after
  }

  /** Removes a [[IpNetwork]] from the target Network.
    *
    * @param target   An IpNetwork to be divided up.
    * @param exclude  An IpNetwork to be removed from the target.
    * @return Sequence of IpNetwork objects remaining after exclusion.
    */
  def cidrExclude(target: IpNetwork, exclude: IpNetwork): Seq[IpNetwork] = {
    val (before, _, after) = cidrPartition(target, exclude)
    before ++ after
  }

  /** Address partitioner.
    *
    * Partitions a target [[IpNetwork]] object on an exclude Network.
    *
    * @param target   An IpNetwork that needs to be divided up
    * @param exclude  An IpNetwork to partition on
    * @return A 3-tuple of sorted IpNetwork objects before the target, at the partition point, and
    *         after the target. Adding these three sequences returns the equivalent of the original
    *         network address.
    */
  def cidrPartition(
      target: IpNetwork,
      exclude: IpNetwork): (IndexedSeq[IpNetwork], IndexedSeq[IpNetwork], IndexedSeq[IpNetwork]) = {

    @tailrec // Tail recursive method that partitions a network.
    def cidrPartitionRecurse(
        lower: Long,
        upper: Long,
        prefix: Int,
        left: IndexedSeq[IpNetwork],
        right: IndexedSeq[IpNetwork]): (IndexedSeq[IpNetwork], IndexedSeq[IpNetwork]) = {
      if (exclude.mask < prefix) {
        (left, right)
      } else {
        val (matched, newLeft, newRight) = if (exclude.first >= upper) {
                                             val newNetwork = IpNetwork(lower, prefix)
                                             (upper, left :+ newNetwork, right)
                                           } else {
                                             val newNetwork = IpNetwork(upper, prefix)
                                             (lower, left, right :+ newNetwork)
                                           }
        if ((prefix + 1) <= target.ipAddr.width) {
          val newLower = matched
          val newUpper = matched + scala.math.pow(2, target.ipAddr.width - (prefix + 1)).toLong
          cidrPartitionRecurse(newLower, newUpper, prefix + 1, newLeft, newRight)
        } else {
          (newLeft, newRight)
        }
      }
    }

    if (exclude.last < target.first) {
      // e.g. exclude([3-7], [1-2]) = (Nil, Nil, [3-7])
      (IndexedSeq(), IndexedSeq(), IndexedSeq(target.cidr))
    } else if (target.last < exclude.first) {
      // e.g. exclude([1-2], [3-7]) = ([1-2], Nil, Nil)
      (IndexedSeq(target.cidr), IndexedSeq(), IndexedSeq())
    } else if (target.mask >= exclude.mask) {
      // e.g. exclude([3-5], [1-7]) = (Nil, [3-5], Nil)
      (IndexedSeq(), IndexedSeq(target), IndexedSeq())
    } else {
      // `target` and `exclude` overlap. Do partition.
      val newPrefix = target.mask + 1
      val iLower = target.first
      val iUpper = target.first +
        scala.math.pow(2, target.ipAddr.width - newPrefix).toLong
      val (left, right) =
        cidrPartitionRecurse(iLower, iUpper, newPrefix, IndexedSeq(), IndexedSeq())
      (left, IndexedSeq(exclude), right.reverse)
    }
  }

  /** Accepts a sequence of [[IpNetwork]] objects, merging them into the smallest possible list of
    * CIDRs. It merges adjacent subnets where possible, those contained within others and also
    * removes any duplicates.
    *
    * @param nets Sequence of IpNetwork objects.
    * @return A summarized sequence of IpNetwork objects.
    */
  def cidrMerge(nets: Seq[IpNetwork]): Seq[IpNetwork] = {

    /* Recursively merge networks in a sequence.
     * @param r1 a sequence of network data
     * @param r2 a sequence of network data
     * @param result sequence of merged networks
     * @return a sequence of (version, first addr, last addr, Some[Network]).
     *         The last tuple is `None` if the network gets merged.
     */
    @tailrec
    def mergeRecurse(
        r1: Seq[(Int, Long, Long, Option[IpNetwork])],
        r2: Seq[(Int, Long, Long, Option[IpNetwork])],
        result: Seq[(Int, Long, Long, Option[IpNetwork])]):
      Seq[(Int, Long, Long, Option[IpNetwork])] = {
      if (r2.isEmpty) {
        result ++ r1
      } else {
        val (currentVer, currentFirst, currentLast, _) = r2.head
        val (previousVer, previousFirst, previousLast, _) = r1.head
        if (currentVer == previousVer && currentFirst - 1 <= previousLast) {
          // Networks can be merged
          // Merged networks will change last tuple value to `None`
          val newNet = (currentVer, previousFirst, previousLast.max(currentLast), None)
          mergeRecurse(r1.updated(1, newNet).drop(1), r2.drop(1), result)
        } else {
          mergeRecurse(r1.drop(1), r2.drop(1), result :+ r1.head)
        }
      }
    }

    if (nets.isEmpty) {
      Nil
    } else {
      val ranges = for { net <- nets.sorted } yield (net.version, net.first, net.last, Some(net))
      val netWidth = nets.head.ip.width // network bits
      val mergedRanges = mergeRecurse(ranges, ranges.drop(1), Nil)
      val res: Seq[Seq[IpNetwork]] = for { rangeTuple <- mergedRanges } yield {
        rangeTuple._4 match {
          case Some(x) => Seq(x)
          case None =>
            val startAddr = IpNetwork(rangeTuple._2, netWidth)
            val endAddr = IpNetwork(rangeTuple._3, netWidth)
            boundedNetworkSeq(startAddr, endAddr)
        }
      }
      res.flatten
    }
  }

  /** Matches an IP address against a given sequence of network addresses.
    *
    * @param address  A dot-delimited IP address
    * @param cidrs    A sequence of network addresses in CIDR notation
    * @return Sequence of matching Networks, an empty sequence if there was no match.
    */
  def allMatchingCidrs(address: String, cidrs: Seq[String]): IndexedSeq[IpNetwork] = {
    val addr = IpAddress(address)
    val nets = netsRecurse(cidrs, Some(Nil))
    if (nets.isEmpty) {
      throw new IpaddrException(IpaddrException.invalidCidrSequence)
    }
    allMatchingCidrsRecurse(addr, nets.get.sorted, Nil.toIndexedSeq)
  }

  /** Returns largest matching network for a given IP address.
    *
    * @param ip     A dot-delimited IP address
    * @param cidrs  A sequence of network addresses in CIDR notation
    * @return The largest (least specific) matching IpNetwork from the provided sequence,
    *         None if there was no match.
    */
  def largestMatchingCidr(ip: String, cidrs: Seq[String]): Option[IpNetwork] = {
    val addr = IpAddress(ip)
    val nets = netsRecurse(cidrs, Some(Nil))
    if (nets.isEmpty) {
      throw new IpaddrException(IpaddrException.invalidCidrSequence)
    }
    largestMatchingCidrRecurse(addr, nets.get.sorted, None)
  }

  /** Returns smallest matching network for given IP address.
    *
    * @param ip     A dot-delimited IP address
    * @param cidrs  A sequence of network addresses in CIDR notation
    * @return The smallest (most specific) matching IpNetwork from the provided sequence,
    *         None if there was no match.
    */
  def smallestMatchingCidr(ip: String, cidrs: Seq[String]): Option[IpNetwork] = {
    val addr = IpAddress(ip)
    val nets = netsRecurse(cidrs, Some(Nil))
    if (nets.isEmpty) {
      throw new IpaddrException(IpaddrException.invalidCidrSequence)
    }
    smallestMatchingCidrRecurse(addr, nets.get.sorted, None)
  }

  /** Converts input data of CSV addresses into [[IpSet]].
    *
    * @param target1     A sequence of dot-delimited IP addresses
    * @param target2     A sequence of dot-delimited IP addresses
    * @param excludeSeq  A sequence of dot-delimited IP addresses
    * @return An IpSet resulting from equation (target1+target2)-excludeSeq
    * @throws IpaddrException if any translation from address to IpSet fails
    */
  def arrsToCidrs(target1: Seq[String], target2: Seq[String], excludeSeq: Seq[String]): IpSet = {
    arrsToCidrs(target1.iterator, target2.iterator, excludeSeq.iterator)
  }

  /** Converts input data of CSV addresses into [[IpSet]].
    *
    * @param target1    an iterator over dot-delimited IP addresses
    * @param target2    an iterator over dot-delimited IP addresses
    * @param excludeSeq an iterator over dot-delimited IP addresses
    * @return an IpSet resulting from equation (target1+target2)-excludeSeq
    * @throws IpaddrException if any translation from address to IpSet fails
    */
  @throws(classOf[IpaddrException])
  def arrsToCidrs(
      target1: Iterator[String],
      target2: Iterator[String],
      excludeSeq: Iterator[String]): IpSet = {
    val set1 = arrToCidrs(target1)
    val set2 = arrToCidrs(target2)
    // Add the two target arrays of cidrs
    val targetSet = set1 | set2
    val excludeSet = arrToCidrs(excludeSeq)
    // Remove the exclude array of cidrs
    IpSet(targetSet.diff(excludeSet).toSeq)
  }

  /** Converts a sequence of strings into an IpSet.
    *
    * @param it An iterator over addresses in various formats (CIDR, glob, range)
    * @return An IpSet constructed from the input sequence
    * @throws IpaddrException if error happens during conversion
    */
  @throws(classOf[IpaddrException])
  def arrToCidrs( // scalastyle:ignore cyclomatic.complexity method.length
      it: Iterator[String]): IpSet = {
    it.foldLeft (IpSet()) { (acc, item) =>
      val data = item.trim
      logger.debug("Attempting network parsing of input")
      Try(IpNetwork(data)) match {
        case Success(n) => acc | IpSet(n)
        case Failure(_) =>
          logger.debug("Attempting range parsing of input")
          Try(getRange(data)) match {
            case Success(range) => acc | IpSet(range)
            case Failure(_) =>
              logger.debug("Attempting glob parsing of input")
              Try(IpGlob.globToCidrs(data)) match {
                case Success(network) => acc | IpSet(network)
                case Failure(_) =>
                  logger.debug("Attempting nmap parsing of input")
                  Try(Nmap.iterNmapRange(data)) match {
                    case Success(iter) =>
                      if (iter.isEmpty) {
                        // no results were found in the nmap iterator
                        throw new IpaddrException(IpaddrException.invalidCidrSequence)
                      } else {
                        acc | iter.foldLeft (IpSet()) { (innerAcc, item) =>
                          innerAcc | IpSet(IpNetwork(item, item.width))
                        }
                      }
                    case Failure(err) =>
                      // The value did not parse as any known format
                      throw err
                  }
              }
          }
      }
    }
  }

  /** Accepts a valid IP range notation and converts it into IpRange.
    *
    * @param data String representation of IP range e.g "10.2.1.0-10.2.1.4"
    * @return An IpRange if conversion was successful.
    * @throws IpaddrException if an error occurs.
    */
  private def getRange(data: String): IpRange = {
    val rangeRegex = ("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\s*-""" +
                      """\s*(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""").r
    data match {
      case rangeRegex(start, end) => IpRange(start, end)
      case _ => throw new IpaddrException(s"Cannot parse IpRange from '$data'")
    }
  }

  /** Converts input data of CSV addresses into [[IpSet]].
    *
    * @param target          A CSV-separated list of targets
    * @param targetFile      A filename containing IP representations
    * @param exclusionsFile  A filename containing IP representations
    * @return an IpSet resulting from equation (target1+target2)-excludeSeq.
    */
  @throws(classOf[IpaddrException])
  def inputToCidrs(target: String, targetFile: String, exclusionsFile: String): IpSet = {
    try {
      val targetFileLines = Source.fromFile(targetFile).getLines()
      val excludeFileLines = Source.fromFile(exclusionsFile).getLines()
      linesToCidrs(target, targetFileLines, excludeFileLines)
    } catch {
      case fnf: FileNotFoundException => throw fnf
    }
  }

  /** CSV, lines, and exclude lines to IPSet.
    *
    * @param csvLine      Comma separated list of IP representations
    * @param lines        An iterator over IP representation lines
    * @param excludeLines An iterator over IP representation lines
    * @return An IpSet resulting from equation (target1+target2)-excludeSeq.
    */
  def linesToCidrs(
      csvLine: String,
      lines: Iterator[String],
      excludeLines: Iterator[String]): IpSet = {
    arrsToCidrs(csvLine.split(',').iterator, lines, excludeLines)
  }

  /** Chops an IpSet into smaller pieces.
    *
    * Generates multiple IpSets of size not exceeding 256 hosts.
    *
    * @param ipSet An IpSet to chop
    * @return A sequence of small IpSets
    */
  def chopSet(ipSet: IpSet): Seq[IpSet] = {

    /* Recursively chop networks to size no more than 256 hosts.
     *
     * @param in input sequence of Networks
     * @param out the resulting sequence of chopped Networks
     * @return a sequence of chopped [[Network]] objects
     */
    @tailrec
    def chopRecurse(in: Seq[IpNetwork], out: Seq[IpNetwork]): Seq[IpNetwork] = {
      if (in.nonEmpty) {
        val currentNet = in.head
        if (currentNet.size > 256) {
          // Chop this network down to 256 hosts i.e. 24 network bits
          val chopSize = 24
          chopRecurse(in.drop(1), out ++ currentNet.subnet(chopSize))
        } else {
          chopRecurse(in.drop(1), currentNet +: out)
        }
      } else {
        out
      }
    }

    if (ipSet.isEmpty) {
      Seq(ipSet)
    } else {
      val choppedNetworks = chopRecurse(ipSet.networkSeq, Nil)
      mergeSets(choppedNetworks.map(IpSet(_)))
    }
  }

  /** Merge IpSets containing fewer than 256 hosts.
    *
    * @param ipSetSeq A sequence of IpSets to merge
    * @return A sequence of merged IpSets
    */
  private def mergeSets(ipSetSeq: Seq[IpSet]): Seq[IpSet] = {

    /* Recursively merge IpSets containing fewer than 256 hosts.
     *
     * @param in input sequence of [[IpSet]]
     * @param out the resulting sequence of merged IpSets
     * @return a sequence of merged IpSets
     */
    @tailrec
    def mergeRecurse(in: Seq[IpSet], out: Seq[IpSet]): Seq[IpSet] = {
      if (in.nonEmpty) {
        val currentSet = in.head
        if (out.nonEmpty) {
          if (out.head.volume + currentSet.volume > 256) {
            val newOut = currentSet +: out
            mergeRecurse(in.drop(1), newOut.sortBy(_.volume))
          } else {
            val newOut = out.updated(0, out.head | currentSet)
            mergeRecurse(in.drop(1), newOut.sortBy(_.volume))
          }
        } else {
          mergeRecurse(in.drop(1), Seq(currentSet))
        }
      } else {
        out
      }
    }

    if (ipSetSeq.isEmpty) {
      ipSetSeq
    } else {
      val (smallSets, bigSets) = ipSetSeq.partition {
        _.volume != 256
      }
      mergeRecurse(smallSets, Nil) ++ bigSets
    }
  }

  /** A recursive function to evaluate spanning addresses.
    *
    * Recursively compute IP addresses until prefix is valid and new IP address is higher than
    * `lowIp` address. With each recursion, new value of IP address is calculated by decrementing
    * the old value by the number of hosts that can fit inside the `newPrefix`. Recursion ends when
    * the prefix <= 0 or new IP address becomes lower than `lowIp`.
    *
    * @param prefix Numerical prefix
    * @param ip     Long equivalent of the upper IP address
    * @param lowIp  Long equivalent of the lower IP address
    * @param width  Number of bits in this IP address
    * @return tuple(prefix, ip)
    */

  @tailrec
  private def spanningCidrRecurse(
      prefix: Int,
      ip: Long,
      lowIp: Long,
      width: Int): (Int, Long) = {
    if (prefix > 0 && ip > lowIp) {
      val newPrefix = prefix - 1
      val ip2 = -(1L << (width - newPrefix))
      spanningCidrRecurse(newPrefix, ip & ip2, lowIp, width)
    } else {
      (prefix, ip)
    }
  }

  /** A recursive function for `allMatchingCidrs`.
    *
    * @param ip    An [[IpAddress]] object
    * @param cidrs A sequence of [[IpNetwork]] objects
    * @param res   A sequence of matching IpNetwork objects
    * @return A sequence of matching Networks, an empty sequence if there was no match.
    */
  @tailrec
  private def allMatchingCidrsRecurse(
      ip: IpAddress,
      cidrs: Seq[IpNetwork],
      res: IndexedSeq[IpNetwork]): IndexedSeq[IpNetwork] = {
    if (cidrs.nonEmpty) {
      if (cidrs.head.contains(ip)) {
        allMatchingCidrsRecurse(ip, cidrs.drop(1), res :+ cidrs.head)
      } else if (res.nonEmpty && !res.last.contains(cidrs.head.network)) {
        allMatchingCidrsRecurse(ip, Nil, res)
      } else {
        allMatchingCidrsRecurse(ip, cidrs.drop(1), res)
      }
    } else {
      res
    }
  }

  /** Recursively convert sequence of addresses to [[IpNetwork]] objects.
    *
    * @param s   A sequence of network addresses in CIDR notation
    * @param res A sequence of Networks
    * @return A sequence of IpNetwork objects, None if there was an error during conversion
    */
  @tailrec
  private def netsRecurse(s: Seq[String], res: Option[Seq[IpNetwork]]): Option[Seq[IpNetwork]] = {
    if (s.nonEmpty) {
      Try(IpNetwork(s.head)) match {
        case Success(x) => netsRecurse(s.drop(1), Some(res.get :+ x))
        case Failure(_) => netsRecurse(Nil, None)
      }
    } else {
      res
    }
  }

  /** Recursive function for [[largestMatchingCidr]]
    *
    * @param ip     An [[IpAddress]]
    * @param cidrs  A sequence of [[IpNetwork]] objects.
    * @param res    A matching IpNetwork object
    * @return The largest (least specific) matching IpNetwork from the provided sequence,
    *         None if there was no match.
    */
  @tailrec
  private def largestMatchingCidrRecurse(
      ip: IpAddress,
      cidrs: Seq[IpNetwork],
      res: Option[IpNetwork]): Option[IpNetwork] = {
    if (cidrs.nonEmpty) {
      if (cidrs.head.contains(ip)) {
        largestMatchingCidrRecurse(ip, Nil, Some(cidrs.head))
      } else {
        largestMatchingCidrRecurse(ip, cidrs.drop(1), res)
      }
    } else {
      res
    }
  }

  /** Recursive function for [[smallestMatchingCidr]]
    *
    * @param ip     An [[IpAddress]]
    * @param cidrs  A sequence of [[IpNetwork]] objects
    * @param res    A matching IpNetwork object
    * @return The smallest (most specific) matching IpNetwork from the provided sequence,
    *         None if there was no match.
    */
  @tailrec
  private def smallestMatchingCidrRecurse(
      ip: IpAddress,
      cidrs: Seq[IpNetwork],
      res: Option[IpNetwork]): Option[IpNetwork] = {
    if (cidrs.nonEmpty) {
      if (cidrs.head.contains(ip)) {
        smallestMatchingCidrRecurse(ip, cidrs.drop(1), Some(cidrs.head))
      } else if (res.isDefined && !res.get.contains(cidrs.head.network)) {
        smallestMatchingCidrRecurse(ip, Nil, res)
      } else {
        smallestMatchingCidrRecurse(ip, cidrs.drop(1), res)
      }
    } else {
      res
    }
  }

}
