# Trellis Linked Data Server (database)

Trellis is a scalable platform for building [linked data](https://www.w3.org/TR/ldp/) applications.
The `trellis-ext-db` project implements a persistence layer based on a relational database.

[![Build Status](https://travis-ci.com/trellis-ldp/trellis-ext-db.svg?branch=trellis-ext-db-0.8.x)](https://travis-ci.com/trellis-ldp/trellis-ext-db)
[![Coverage Status](https://coveralls.io/repos/github/trellis-ldp/trellis-ext-db/badge.svg?branch=trellis-ext-db-0.2.x)](https://coveralls.io/github/trellis-ldp/trellis-ext-db?branch=trellis-ext-db-0.2.x)
![Maven Central](https://img.shields.io/maven-central/v/org.trellisldp.ext/trellis-db.svg)

PostgreSQL and MySQL/MariaDB database connections are supported.

A [Docker container](https://hub.docker.com/r/trellisldp/trellis-ext-db/) is available via:

```
docker pull trellisldp/trellis-ext-db
```

This container assumes the presence of an external database. Additional information about
[running Trellis in a Docker container](https://github.com/trellis-ldp/trellis/wiki/Dockerized-Trellis)
can be found on the TrellisLDP wiki.

Java 8+ is required to run Trellis. To build this project, use this command:

```
$ ./gradlew install
```

For more information about Trellis, please visit either the
[main source repository](https://github.com/trellis-ldp/trellis) or the
[project website](https://www.trellisldp.org).

