# EventSource Hub [![Build Status](https://travis-ci.org/ScalaWilliam/eventsource-hub.svg?branch=master)](https://travis-ci.org/ScalaWilliam/eventsource-hub)

> Lightweight HTTP message queue using [EventSource](https://www.w3.org/TR/2012/WD-eventsource-20120426/) with file persistence.

Ideas based on: https://plus.google.com/103489630517643950426/posts/RFhSAGMP4Em and implemented in Scala - you can find another similar software called [ssehub](https://github.com/vgno/ssehub), written in C++.

# Purpose

To enable low friction [event sourcing](https://martinfowler.com/eaaDev/EventSourcing.html).

# What it does

To run:
```
$ docker pull scalawilliam/eventsource-hub
$ docker run -p 9000:9000 scalawilliam/eventsource-hub
```

To query all channel's historical data (ie replay):
```
$ curl -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/tab-separated-values

<id><TAB><event><TAB><data>
<id><TAB><event><TAB><data>
...
<id><TAB><event><TAB><data>
```

This data is stored in the same format in `events/` directory as a TSV file. In a Docker container, it's `/opt/docker/events/`.

To query new data as an infinite stream: 
```
$ curl -H 'Accept: text/event-stream' -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/event-stream

event: <event>
id: <id>
data: <data>
... 
```

To post an event:
```
$ echo Some data | curl -d @- http://localhost:9000/a-channel?event=type
HTTP/1.1 201 Created
...

2017-04-05T11:40:10.793Z
```

Which will yield in the stream:

```
event: type
id: 2017-04-05T11:40:10.793Z
data: Some data

```

ID by default is the current [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) timestamp in UTC. You may override it by specifying the `id` query parameter. It will be used as the [`Last-Event-ID`](https://www.w3.org/TR/eventsource/#last-event-id) index. We support resuming based on the last event as per specification.

If you want to specify the `event` field which is optional, use the `event` query parameter.

Behaviour is undefined if you send data in binary.

If you send multiple lines, they will be turned into multiple events each with the same ID (ID is indicative of order but otherwise not unique).

Access control is not in scope. Please use an [API gateway](https://en.wikipedia.org/wiki/API_management) or [nginx](https://www.nginx.com/solutions/api-gateway/) for that.

You can push NDJSON, TSV, CSV or any other plain text.

# Technical choices

I chose this stack because of my experience and familiarity.

- [Scala](http://www.scala-lang.org/news/) and [Play framework](https://www.playframework.com/documentation/2.6.x/Migration26) because I'm experienced in it. See [ActionFPS](https://github.com/ScalaWilliam/ActionFPS) and [Git Watch](http://git.watch/) which also use Event Source.
- Build tool: [SBT](https://www.scalawilliam.com/essential-sbt/) default for Play and supports Docker.
- [Docker](https://www.docker.com/what-docker) lets us easily distribute the application.
- [Travis CI](https://en.wikipedia.org/wiki/Travis_CI) for automated build & publishing of Docker artifacts because it's de facto standard for open source projects.

This repository is based on https://github.com/ScalaWilliam/play-docker-hub-example.
