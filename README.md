# EventSource Hub [![Build Status](https://travis-ci.org/ScalaWilliam/eventsource-hub.svg?branch=master)](https://travis-ci.org/ScalaWilliam/eventsource-hub)

> Lightweight HTTP message queue using [EventSource](https://www.w3.org/TR/2012/WD-eventsource-20120426/) with file persistence.

Ideas based on: https://plus.google.com/103489630517643950426/posts/RFhSAGMP4Em and implemented in Scala. There's another similar and active project in C++ called [ssehub](https://github.com/vgno/ssehub).

# Purpose

To enable low friction [event sourcing](https://martinfowler.com/eaaDev/EventSourcing.html).

# What it does

Receives events via POST, retains them and also publishes them to all current subscribers.

# What to use it for

Event Sourcing. You push events and your clients can read either input or their own output when restarted. Then you don't need a complex relational database and all your state is reproducibly stored in memory. You can rebuild that state with [`scan` or `scanLeft`](https://www.scalawilliam.com/most-important-streaming-abstraction/).

You get both batch and reactive mode by having access to a finite historical file and an infinite update stream.

# How to use it

## Client libraries

- [eventsource for Node.js](https://www.npmjs.com/package/eventsource).
- [Alpakka's Server-sent Events Connector](http://developer.lightbend.com/docs/alpakka/current/sse.html).
- If other good libraries with recovery exist, make a PR.
- Or just plain curl if you want to access your data directly, as shown below.

## (typical) Event Sourcing usage

```
event stream = historical events + live events
```

You can also load the historical data in batch for performance reasons but that is out of scope of this document.

## Run the server
To run with [Docker](https://www.docker.com/what-docker) and a [mounted volume](https://docs.docker.com/engine/tutorials/dockervolumes/) in `events` directory (files are `.tsv`):
```
$ docker pull scalawilliam/eventsource-hub
$ mkdir -p events
$ docker run -v $(PWD)/events:/opt/docker/events -p 9000:9000 scalawilliam/eventsource-hub
```

## Query infinite stream of new events

Channel names must match pattern `[A-Za-z0-9_-]{4,64}`.

Each distinct channel name maps to a different channel, and each is backed by a different storage file `events/<name>.tsv`.

```
$ curl -H 'Accept: text/event-stream' -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/event-stream

event: <event>
id: <id>
data: <data>
... 
```

## Post a new event
```
$ echo Some data | curl -d @- http://localhost:9000/a-channel?event=type
HTTP/1.1 201 Created
...

2017-04-05T11:40:10.793Z
```

Which will yield an entry in the stream such as:

```
event: type
id: 2017-04-05T11:40:10.793Z
data: Some data

```

The query parameter `event` is optional.

ID by default is the current [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) timestamp in UTC. You may override it by specifying the `id` query parameter. It will be used as the [`Last-Event-ID`](https://www.w3.org/TR/eventsource/#last-event-id) index. We support resuming based on the last event as per specification. ID is not necessarily unique.

Behaviour is undefined if you send data in binary. Behaviour is also undefined if you send more than a multi-line request body.

Events are ordered.

## Retrieve past events
```
$ curl -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/tab-separated-values

<id><TAB><event><TAB><data>
<id><TAB><event><TAB><data>
...
<id><TAB><event><TAB><data>
```

## Event payload

You can push NDJSON, TSV, CSV or any other plain text.

This is especially perfect for time series data.

## Access control

Not in scope. You can use an [API gateway](https://en.wikipedia.org/wiki/API_management) or [nginx](https://www.nginx.com/solutions/api-gateway/) for that.

There's the [`nginx_http_http_auth_request` module](http://nginx.org/en/docs/http/ngx_http_auth_request_module.html) which you can use [with Play framework](https://groups.google.com/d/msg/play-framework/IRVgowWxE58/4SIQZ_ksCAAJ). [Example in pure nginx](https://developers.shopware.com/blog/2015/03/02/sso-with-nginx-authrequest-module/).

# Technical choices

I chose this stack because of my experience and familiarity with it.

- [Scala](http://www.scala-lang.org/news/) and [Play framework](https://www.playframework.com/documentation/2.6.x/Migration26) because I'm experienced in it. See [ActionFPS](https://github.com/ScalaWilliam/ActionFPS) and [Git Watch](http://git.watch/) which also use Event Source.
- Build tool: [SBT](https://www.scalawilliam.com/essential-sbt/) default for Play and supports Docker.
- [Docker](https://www.docker.com/what-docker) lets us easily distribute the application.
- [Travis CI](https://en.wikipedia.org/wiki/Travis_CI) for automated build & publishing of Docker artifacts because it's de facto standard for open source projects.
- This repository is based on https://github.com/ScalaWilliam/play-docker-hub-example.
- I'm taking something akin to [Bug Driven Development](https://blogs.oracle.com/toddfast/entry/bug_driven_development)

# Licence

- Copyright 2017 Apt Elements
- [GNU AGPLv3](https://choosealicense.com/licenses/agpl-3.0/), because I would like any improvements to be incorporated into this application. Anything proprietary and custom such as request filters and authentication modules should be separated anyway for SRP.
