# EventSource Hub [![Build Status](https://travis-ci.org/ScalaWilliam/eventsource-hub.svg?branch=master)](https://travis-ci.org/ScalaWilliam/eventsource-hub)

> Lightweight HTTP message queue using [EventSource](https://www.w3.org/TR/2012/WD-eventsource-20120426/) with file persistence.

Ideas based on: https://plus.google.com/103489630517643950426/posts/RFhSAGMP4Em

# Purpose

To enable low friction event sourcing.

# What it does

To run:
```
$ docker run -p 9000 -it scalawilliam/eventsource-hub
```

To query all channel's historical data (ie replay):
```
$ curl -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/tab-separated-values

<id>TAB<event>TAB<data>
<id>TAB<event>TAB<data>
...
<id>TAB<event>TAB<data>
```

To query new data: 
```
$ curl -H 'Accept: text/event-stream' -i http://localhost:9000/a-channel
HTTP/1.1 200 OK
Content-type: text/event-stream

id: <id>
event: <event>
data: <data>
... 
```

To post an event:
```
$ echo Some data | curl -d @- http://localhost:9000/a-channel
```

ID by default is the current ISO timestamp. You may override it by specifying the `id` query parameter. It will be used as the `Last-ID` index.

If you want to specify the `event` field which is optional, use the `event` query parameter.

Behaviour is undefined if you send data in binary.

If you send multiple lines, they will be turned into multiple events each with the same ID (ID is indicative but not unique).

Based on https://github.com/ScalaWilliam/play-docker-hub-example.

Access control is not in scope. Please use an API gateway or nginx for that.
