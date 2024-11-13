# Feature - Performance Tracing

* [Introduction](#introduction)
* [API Reference](#api-reference)
  * [Start a span](#start-a-span)
  * [End a span](#end-a-span)
  * [Set span parent](#set-parent-span)
  * [Set no parent](#set-no-parent)
  * [Add checkpoint](#add-checkpoint)
  * [Using scopes](#using-scopes)
  * [Set current span](#set-current-span)
  * [Get current span](#get-current-span)
  * [Using span builder](#using-span-builder)
* [Distributed Tracing](#distributed-tracing)
  * [OkHttp example](#example-with-okhttp)
  * [OkHttp interceptor example](#example-with-okhttp-interceptor)

## Introduction

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

### Set no parent

To explicitly opt out of setting a parent to a span, start the span with `setNoParent` flag.

```kotlin
val span: Span = Measure.startSpan("span-name", setNoParent = true)
```

### Add checkpoint

```kotlin
val span: Span = Measure.startSpan("span-name").setCheckpoint("checkpoint-name")
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


## Distributed Tracing

Distributed tracing is a monitoring method that helps tracking requests as they travel through
different services in a distributed system (like microservices, serverless functions, and mobile apps).

The `traceparent` header is a key component of distributed tracing that helps track requests as they flow through
different services. It follows the [W3C Trace Context specification](https://www.w3.org/TR/trace-context/#header-name)
and consists of four parts in a single string:

Example: `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`

Where:

* `00`: Version
* `4bf92f3577b34da6a3ce929d0e0e4736`: Globally unique trace ID
* `00f067aa0ba902b7`: Parent span ID (representing the current operation)
* `01`: Trace flags (like whether sampling is enabled)

When your mobile app makes API calls, including this header allows you to correlate the client-side operations with
server-side processing, giving you end-to-end visibility of your request flow.

To get a trace parent header:

```kotlin
val span = Measure.startSpan("http")
val key = Measure.getTraceParentHeaderKey()
val value = Measure.getTraceParentHeaderValue(span)
```

#### Example with OkHttp

```kotlin
val httpSpan = Measure.startSpan("http")
try {
    okHttpClient.newCall(
        okhttp3.Request.Builder().url("https://example.com/")
            .header(
                Measure.getTraceParentHeaderKey(),
                Measure.getTraceParentHeaderValue(httpSpan),
            )
            .get().build()
    ).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            httpSpan.setStatus(SpanStatus.Error).end()
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            httpSpan.setStatus(SpanStatus.Ok).end()
        }
    })
} catch (e: IllegalStateException) {
    httpSpan.setStatus(SpanStatus.Error).end()
}
```

#### Example with OkHttp interceptor

```kotlin
// First, create a Retrofit interceptor to handle tracing
class TracingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val span = Measure.startSpan("http")
        try {
            // Add traceparent header to the request
            val originalRequest = chain.request()
            val tracedRequest = originalRequest.newBuilder()
                .header(
                    Measure.getTraceParentHeaderKey(),
                    Measure.getTraceParentHeaderValue(span)
                )
                .build()

            // Execute the request
            val response = chain.proceed(tracedRequest)

            // Set span status based on response
            if (response.isSuccessful) {
                span.setStatus(SpanStatus.Ok)
            } else {
                span.setStatus(SpanStatus.Error)
            }

            return response
        } catch (e: Exception) {
            span.setStatus(SpanStatus.Error)
            throw e
        } finally {
            span.end()
        }
    }
}

// Set up Retrofit client with the interceptor
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(TracingInterceptor())
    .build()
```
