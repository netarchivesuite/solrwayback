# Extended Solr guide

This document describes non-trivial Solr details. It should not be necessary for people who are only trying out the SolrWayback or running small-scale (less than 100 million records) installations.

It is written from the perspective of net archives and from experience at the Royal Danish Library. It might be applicable elsewhere, but is not intended as a general Solr guide.

## Prerequisites

Familiarity with running the SolrWayback bundle in "standard" mode is presumed. The reader does not need to have detailed knowledge of running Solr in production.

Solr requires Java and runs on at least MacOS, Linux and Windows. While this guide assumes Linux, it should be fairly easy to adapt to MacOS and Windows.

## Basic Solr

Solr logistics can be seen as collections made up of shards having replicas. Updates and queries are directed to a collection and Solr takes care of routing.

Solr is capable of running in two modes: Standalone and Cloud.

Solr 7 Standalone was used in the previous SolrWayback Bundle and is a constrained setup with a single shard per collection. When running the Solrwayback bundle there is only a single collection `netarchivebuilder`. A single shard is fine for 100 million documents and can work for maybe 500 million documents. Beyond that is problematic because of internal Solr limits. Standalone does not support [streaming expressions](https://solr.apache.org/guide/8_7/streaming-expressions.html) and distributed setup.

Solr 9 Cloud is the new default for SolrWayback Bundle. It is installed with a single shard and only one replica, essentially the same as the old Solr 7 Standalone except for [streaming expressions](https://solr.apache.org/guide/solr/9_1/query-guide/streaming-expressions.html) being available. It can be [extended with more shards and replicas](https://solr.apache.org/guide/solr/9_1/deployment-guide/cluster-types.html) later on if needed.

## Upgrading Solr

### Standalone -> Cloud

TODO: Write this

### Version X -> Y

Solr only supports backward compatibility over 1 major version and even then it is highly recommended to do a [full re-index](https://solr.apache.org/guide/solr/9_1/upgrade-notes/major-changes-in-solr-9.html#reindexing-after-upgrade) instead of running the old data files under a new version.

Opinionated advice: Stick to a major Solr version until security concerns or technical needs dictate an upgrade, then start from a blank setup and do a full re-index.

## Solr hardware

For a webscale Solr index, the primary problem is performance. For Solr standard searches, random access to index data is _the bottleneck_: CPU power is mostly secondary and helps mostly with large exports or streaming expressions. The basic "works everywhere" strategy to improve random access performance is _"Buy more RAM"_, but at webarchive scale this can be a rather expensive affair. A basic and relatively inexpensive advice is to use SSDs instead of spinning drives, but this is 2023 so who uses spinning drives for anything performance related?

How much hardware? Well, that really depends on requirements and index size. At the Royal Danish Library the requirements are low and the index size large (40+ billion records) so hardware has been dialed as far down as possible. Some details at [70TB, 16b docs, 4 machines, 1 SolrCloud](https://sbdevel.wordpress.com/2016/11/30/70tb-16b-docs-4-machines-1-solrcloud/) but it can be roughly condensed to being 140 shards, each with 300 million records or 900GB of index data, with each shard having 0.64 CPU and 16GB RAM available to serve it. This should be seen as minimum requirements as there are plans for doubling the amount of memory.

Rough rule of thumb for a setup with low level of use (1-3 concurrent searches): 100 million records ~ 300GB Solr index data ~ 0.5 CPU ~ 10GB of RAM.

## Going to production (hundreds of millions to a few billion records)

At this scale a basic Solr Cloud works well: Deploy a cloud according to the [Solr documentation](https://solr.apache.org/guide/solr/9_1/deployment-guide/taking-solr-to-production.html), create a single collection with with a shard count so that each shard holds 100-300 million records of the expected total record count.


The big decision is whether to add shard replicas or not.

 * Pro: Replicas on separate hardware ensures continued operation in case of hardware failure
 * Pro: Although not a substitute for proper backup, replicas can be seen as [RAID 1](https://en.wikipedia.org/wiki/Standard_RAID_levels#RAID_1)
 * Con: Replicas double the RAM requirements for the same search latency, although they do also double throughput if CPUs are scaled OR
 * Con: Replicas lower overall performance if RAM is not doubled

## Scaling up (tens of billions of records)



## Scaling beyond (100+ billion records)

