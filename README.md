[![license](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](https://github.com/risksense/ipaddr/blob/master/LICENSE)

# Ipaddr
Network address manipulation library for Scala. Inspired by [netaddr](https://github.com/drkjam/netaddr)
library for Python. Examples in this readme are taken from [here](https://netaddr.readthedocs.io/en/latest/).

## Usage
Add the following to your build.sbt:

`libraryDependencies += "com.risksense" % "ipaddr_2.12" % "1.0.3"`

## Tutorial
  * [IpAddress](#ipaddress)
  * [IpNetwork](#ipnetwork)
  * [IpRange](#iprange)
  * [IpSet](#ipset)
  * [Nmap style address](#nmap)

## Contributing
Before making any contributions, please check the issues section to make sure your concern is not
duplicate. If your issue is not already addressed, please create one. Describe the problem/bug/feature
that you would like to be solved. If you are ready to contribute, read on...

1. Fork this repo!
2. Create a new branch with a name that hints about what is added/fixed in the branch.
3. Squash your commits. We like having single commit pull requests :)
4. Open a pull request and make sure your commit message references the issue number.

## License
This project is licensed under the Apache License. Please see [LICENSE](LICENSE) file for more details.

## Authors
  * [Pankaj Khatkar](https://github.com/khatkar) - initial work

### <a name="ipaddress"></a>IpAddress
Create `IpAddress` from a string notation or a Long value
```scala
val ip1: IpAddress = IpAddress("192.168.0.1")
val ip2: IpAddress = IpAddress(3232235521L) // also represents 192.168.0.1
val ip3: IpAddress = IpAddress("192.168.0.2")
```

**Check the equality**
```scala
ip1.equals(ip2) // true
ip1.equals(ip3) // false
```

**More information about an IpAddress**
```scala
ip3.width // 32
ip3.wordSize // 8
ip3.wordCount  // 4
ip3.maxWord  // 255
```

**Numerical representation**
```scala
val ip: IpAddress = IpAddress(169738753L) // 10.30.2.1
ip.hex // 0a1e0201
ip.words // WrappedArray(10, 30, 2, 1)
ip.bits("-") // 00001010-00011110-00000010-00000001
ip.version // 4
```

**Sorting**
```scala
val unsortedIpSeq = Seq(IpAddress("192.0.2.130"), IpAddress("10.0.0.1"), IpAddress("192.0.2.128"),
                        IpAddress("192.0.3.0"), IpAddress("192.0.2.0"))
unsortedIpSeq.sorted // List(10.0.0.1, 192.0.2.0, 192.0.2.128, 192.0.2.130, 192.0.3.0)
```

**Categorization**
  * Private
```scala
IpAddress("192.168.0.1").isPrivate // true
IpAddress("8.8.8.8").isPrivate // false
```
  * LinkLocal
```scala
IpAddress("169.254.0.0").isLinkLocal // true
IpAddress("192.168.0.1").isLinkLocal  // false
```
  * Loopback
```scala
IpAddress("127.0.0.1").isLoopback // true
IpAddress("192.168.0.1").isLoopback  // false
```
  * Multicast
```scala
IpAddress("239.192.0.1").isMulticast // true
IpAddress("192.168.0.1").isMulticast  // false
```
  * Unicast
```scala
IpAddress("239.192.0.1").isUnicast // false
IpAddress("192.168.0.1").isUnicast  // true
```
  * Reserved
```scala
IpAddress("192.0.0.1").isReserved // true
IpAddress("10.1.2.0").isReserved  // false
```
  * Is Netmask
```scala
IpAddress("255.255.254.0").isNetmask // true
IpAddress("239.192.0.1").isNetmask  // false
```
  * Is Hostmast
```scala
IpAddress("0.0.1.255").isHostmask // true
IpAddress("10.1.2.0").isHostmask  // false
```

**Comparing IpAddress**
```scala
IpAddress("192.0.2.1") == IpAddress("192.0.2.1") // true
IpAddress("192.0.2.1") < IpAddress("192.0.2.2") // true
IpAddress("192.0.2.2") > IpAddress("192.0.2.1") // true
IpAddress("192.0.2.1") != IpAddress("192.0.2.1") // false
IpAddress("192.0.2.1") >= IpAddress("192.0.2.1") // true
IpAddress("192.0.2.2") >= IpAddress("192.0.2.1") // true
IpAddress("192.0.2.1") <= IpAddress("192.0.2.1") // true
IpAddress("192.0.2.1") <= IpAddress("192.0.2.2") // true
```

### <a name="ipnetwork"></a>IpNetwork
**Create an IpNetwork from a CIDR string notation. All of the following calls represent the same network:**
```scala
val n1 = IpNetwork("192.0.3.112/22")
val n2 = IpNetwork("192.0.3.112", 22)
val n3 = IpNetwork(IpAddress("192.0.3.112"), 22)
val n4 = IpNetwork(3221226352L, 22)
```

**Useful details about a network:**
```scala
n1.ip // 192.0.3.112
n1.broadcast // 192.0.3.255
n1.netmask // 255.255.252.0
n1.hostmask // 0.0.3.255
n1.size // 1024
n1.first // 3221225472
n1.last // 3221226495
n1.cidr // 192.0.0.0/22, true CIDR address which omits all host bits

val n5 = IpNetwork("192.0.2.1/32") // this creates a network with single host
val n6 = IpNetwork("192.0.2.1") // same as n5
n5.ip // 192.0.2.1
n5.broadcast // 192.0.2.1
n5.netmask // 255.255.255.255
n5.hostmask // 0.0.0.0
n5.size // 1
n5.first // 3221225985
n5.last // 3221225985
```

**Checking if an IpAddress/IpNetwork/IpRange belongs to another IpNetwork:**
```scala
n1.contains("192.0.3.136") // true
n1.contains(IpNetwork("192.0.3.112/24")) // true
n1.contains(IpRange("192.0.3.112", "192.0.3.115")) // true
```

**Get a list of IpAddresses belonging to an IpNetwork:**
```scala
IpNetwork("192.0.3.112/30").iter // Vector(192.0.3.112, 192.0.3.113, 192.0.3.114, 192.0.3.115)
```

**Sorting**
```scala
val unsortedNetworks = Seq(IpNetwork("192.0.2.128/28"), IpNetwork("192.0.3.0/24"),
                           IpNetwork("192.0.2.0/24"), IpNetwork("172.24/12"))
unsortedNetworks.sorted // List(172.24.0.0/12, 192.0.2.0/24, 192.0.2.128/28, 192.0.3.0/24)
```

**Comparing IpNetworks**

*IpNetwork compares the subnets (or lower and upper boundaries) rather than their individual IP
address values. That's why following example returns true.*
```scala
IpNetwork("192.0.2.0/24") == IpNetwork("192.0.2.112/24") // true
```
You can exactly specify which portion of each IpNetwork you’d like to compare.
```scala
IpNetwork("192.0.2.0/24").ip == IpNetwork("192.0.2.112/24").ip // false
IpNetwork("192.0.2.0/24").ip < IpNetwork("192.0.2.112/24").ip // true
IpNetwork("192.0.2.0/24").cidr == IpNetwork("192.0.2.112/24").cidr // true
IpNetwork("192.0.2.0/24") == IpNetwork("192.0.3.0/24") // false
IpNetwork("192.0.2.0/24") != IpNetwork("192.0.3.0/24") // true
IpNetwork("192.0.2.0/24") < IpNetwork("192.0.3.0/24") // true
```

### <a name="iprange"></a>IpRange
**Create an IpRange from dot-delimited IP addresses**
```scala
val r1 = IpRange("10.1.2.3", "10.1.2.9")
r1.toString // 10.1.2.3-10.1.2.9
```

**More information about an IpRange**
```scala
r1.first // 167838211
r1.last // 167838217
r1.size // 7
r1.cidrs  // Vector(10.1.2.3/32, 10.1.2.4/30, 10.1.2.8/31)
```

**Check if an IpAddress/IpNetwork/IpRange belongs to another IpRange**
```scala
r1.contains(IpAddress("10.1.2.4")) // true
r1.contains("10.1.2.4") // true
r1.contains("10.1.2.12") // false
r1.contains(IpNetwork("10.1.2.0/31")) // false
```

### <a name="ipset"></a>IpSet
**Create an IpSet form IpNetwork, IpRange or a sequence of IpNetwork elements**
```scala
val s1 = IpSet(IpRange("10.1.2.0", "10.1.2.8"))
val s2 = IpSet(IpNetwork("10.1.2.0/28"))
val s3 = IpSet(Seq(IpNetwork("10.1.2.0"), IpNetwork("10.1.2.8")))
val emptySet = IpSet()
s1.head // 10.1.2.0/29
s1.ipRange // 10.1.2.0-10.1.2.8
s2.ipRange // 10.1.2.0-10.1.2.15
s1.isContiguous // true
s1.volume // 9
```

**Adding and removing elements from IpSet**

Add/remove an IpAddress, IpNetwork and IpRange from an IpSet.
```scala
s1 + IpAddress("10.1.2.10") // IpSet(10.1.2.0/29, 10.1.2.8/32, 10.1.2.10/32)
s1 + IpAddress("10.1.2.0") // IpSet(10.1.2.0/29, 10.1.2.8/32)
s1 - IpAddress("10.1.2.0") // IpSet(10.1.2.1/32, 10.1.2.2/31, 10.1.2.4/30, 10.1.2.8/32)
s1 - IpNetwork("10.1.2.2/31") // IpSet(10.1.2.0/31, 10.1.2.4/30, 10.1.2.8/32)
s1 - IpRange("10.1.2.0", "10.1.2.8") // IpSet()
```
**Comparing IpSets**
```scala
val largeSet = IpSet(IpRange("10.1.2.0", "10.1.2.8"))
val smallSet = IpSet(IpRange("10.1.2.0", "10.1.2.6"))
smallSet < largeSet // true
smallSet < smallSet // false
smallSet <= largeSet // true.  Equivalent to smallSet.subsetOf(largeSet)
smallSet > largeSet // false
smallSet >= largeSet // false. Equivalent to smallSet.supersetOf(largeSet)
```

**IpSet membership**

Check if an IpAddress or IpNetwork belongs to an IpSet.
```scala
val ipSet = IpSet(IpRange("10.1.2.0", "10.1.2.8"))
ipSet.contains(IpAddress("10.1.2.6")) // true
ipSet.contains(IpAddress("10.1.2.9")) // false
ipSet.contains(IpNetwork("10.1.2.4/30")) // true
```
Check if an IpSet belongs to another IpSet
```scala
val s1 = IpSet(Seq(IpNetwork("10.1.2.6"), IpNetwork("10.1.2.8")))
val s2 = IpSet(Seq(IpNetwork("10.1.2.8"), IpNetwork("10.1.2.10")))
val s3 = IpSet(IpNetwork("10.1.2.9"))
s1.isDisjoint(s2) // false
s1.isDisjoint(s3) // true. Because s3 has nothing in common with s1.
```

**Set operations on IpSet**
```scala
val s1 = IpSet(Seq(IpNetwork("10.1.2.6"), IpNetwork("10.1.2.8")))
val s2 = IpSet(Seq(IpNetwork("10.1.2.8"), IpNetwork("10.1.2.10")))
s1 & s2 // IpSet(10.1.2.8/32). Equivalent to s1.intersect(s2)
s1 | s2 // IpSet(10.1.2.6/32, 10.1.2.8/32, 10.1.2.10/32). Equivalent to s1.union(s2)
s1 ^ s2 // IpSet(10.1.2.6/32, 10.1.2.10/32). Equivalent to s1.symmetricDiff(s2)
```

### <a name="nmap"></a>Nmap style addresses
Ipaddr library provides some helper methods to operate on [Nmap style address](https://nmap.org/book/nping-man-target-specification.html).
```scala
Nmap.validNmapRange("192.168.3-5,7.1") // true
Nmap.validNmapRange("10.2-3.4.5-8") // true
Nmap.validNmapRange("172.163.-.12") // true
Nmap.validNmapRange("10.1.2,1-3") // false
Nmap.validNmapRange("1.2.256.2") // false
Nmap.validNmapRange("17.12.12-a.3") // false
```

Generate an Iterator over Ipaddress contained in an Nmap address.
```scala
Nmap.iterNmapRange("192.168.3-5,7.1")
```
Above code returns an Iterator with IpAddresses 192.168.3.1, 192.168.4.1, 192.168.5.1, 192.168.7.1.
