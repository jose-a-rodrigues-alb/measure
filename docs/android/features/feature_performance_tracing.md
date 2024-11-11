# Feature - Performance Tracing

Tracing helps understand how long certain operations take to complete, from the moment they begin until they finish,
including all the intermediate steps, dependencies, and parallel activities that occur during execution.

A **trace** represents the entire operation, which could be a complete user journey like onboarding, further divided
into multiple steps like login, create profile, etc. A trace is represented by a `trace_id`.

A **span** is the fundamental building block of a trace. A span represents a single unit of work. This could be a HTTP
request, a database query, a function call, etc. Each span contains information about the operation — when it started,
how long it took and whether it completed successfully or not. A span is identified using a `span_id` and a user
defined `name`.

To achieve this, spans in a trace are organized as a Directed Acyclic Graph (DAG). Which means spans can have a parent
span and each span can have multiple children. This is done by adding a **parent_id** to each span, whose value is
the `span_id` of its parent.

Example *app_startup* trace:

![app-startup-trace.png](../images/app-startup-trace.png)

## API Reference

### Start a span

A span can be started using `startSpan` function.

```kotlin
val span: Span = Measure.startSpan("span-name")
```

A span can also be started by providing the start time, this is useful in cases where a certain operation has already
started but there wasn't any way to access the Measure APIs in that part of the code.

```kotlin
val span: Span = Measure.startSpan("span-name", timestamp = System.currentTimeMillis())
```

### End a span

A span can be ended using `end` function. Status is mandatory to set when ending a span.

```kotlin
val span: Span = Measure.startSpan("span-name")
span.end(Status.Ok)
```

A span can also be ended by providing the end time, this is useful in cases where a certain operation has already ended
but there wasn't any way to access the Measure APIs in that part of the code.

```kotlin
val span: Span = Measure.startSpan("span-name")
span.end(Status.Ok, timestamp = System.currentTimeMillis())
```

### Set parent span

```kotlin
val parentSpan: Span = Measure.startSpan("parent-span")
val childSpan: Span = Measure.startSpan("child-span").setParent(parentSpan)
```

### Add checkpoint to a span

```kotlin
val span: Span = Measure.startSpan("span-name").setCheckpoint("checkpoint-name")
```

### Add attribute to a span

```kotlin
val span: Span = Measure.startSpan("span-name").setAttribute("attr-key", value)
```

### Set current span

Spans are stored in thread local storage for easy access across the codebase, without having to pass instances of spans
across different layers.

```kotlin
val spanScope: Scope = Measure.startSpan("span-name").makeCurrent()
```

### Get current span

To get access to the current span use the `getSpan` function. Note that `getSpan` will return the current span in scope
set previously by a `span.makeCurrent` call. Each thread has its own current span.

```kotlin
val span: Span? = Measure.getSpan()
```

### Using scopes

A span can be put in *scope*. Putting in scope means putting the span in thread local. When a span is in
scope it is automatically added as a parent span for all spans created within it's scope. In the following example the
child span will automatically have *spanA* set as it's parent, without having to call
`setParent` explicitly.

```kotlin
val spanA: Span = Measure.startSpan("spanA")
Measure.withScope(spanA) {
    val childSpan = Measure.startSpan("child-span")
    childSpan.end()
}
spanA.end()
```

### Set no parent

To explicitly opt out of setting a parent to a span, start the span with `setNoParent` flag.

```kotlin
val span: Span = Measure.startSpan("span-name", setNoParent = true)
```

### Using span builder

The span builder API allows pre-configuring a span without starting it immediately.

```kotlin
val spanBuilder: SpanBuilder = Measure.createSpan("span-name")
val span: Span = spanBuilder.startSpan()
```

The span builder also allows setting parent or no parent directly using the builder:

```kotlin
val spanBuilder: SpanBuilder = Measure.createSpan("span-name")
    .setNoParent()
val span: Span = spanBuilder.startSpan()
```

```kotlin
val spanBuilder: SpanBuilder = Measure.createSpan("span-name")
    .setParent(parentSpan)
val span: Span = spanBuilder.startSpan()
```

## Automatically collected spans

### **`msr.app_startup`**

App startup span starts when the process starts and ends on the first interaction with the app.

Get the app startup span using `getAppStartupSpan`. This returns the span or null if the span has already ended.

This span can contain the following child spans:

#### ** `msr.cold_launch.ttid` **

Measures the cold launch TTID (Time to Initial Display). This span starts when the process starts and ends when the
first frame for the activity is drawn.

#### ** `msr.cold_launch.ttfi` **

Measures the cold launch TTFI (Time to First Interaction). This span starts when the process starts and ends when the
user interacts with the app for the fist time, i.e. when user clicks, long clicks or scrolls a view.

```kotlin
Measure.getAppStartupSpan()?.let {
    it.setAttribute("key", "value")
}
```

### ** `msr.current_activity` **

A span which tracks the visible activity. This span starts when `onActivityStarted` for an activity is called and ends
when `onActivityPaused` is called.

Get the current activity span using `getCurrentActivitySpan`. This returns the span or null if the span has already
ended.

```kotlin
Measure.getCurrentActivitySpan()?.let {
    it.setAttribute("key", "value")
}
```

### ** `msr.activity_transition` **

A span which tracks duration of activity transitions. This span starts when `onActivityPaused` is called and ends when
first frame of the next activity is drawn.

### ** `msr.hot_launch.ttid` **

A span which measures the duration of a hot launch. This span starts when app `onActivityStarted` is called and ends
when first frame of the activity is drawn.

### ** `msr.warm_launch.ttid` **

A span which measures the duration of a warm launch. This span starts when app `onActivityCreated`
or `onActivityStarted` is called and ends when first frame of the activity is drawn.
