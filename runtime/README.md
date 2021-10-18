# Runtime
The runtime component is responsible for evaluating Snippets,
it exposes the
[SnippetRuntime](../protocol/src/main/proto/jsheets/api/snippet_runtime.proto)
as a [gRpc Service](https://grpc.io).
Snippets are currently evaluated by an instrumented version of
[JShell](https://docs.oracle.com/javase/9/jshell/introduction-jshell.htm) that
is modified for secure execution of script-like code.
Apart from the active *JShell* instances, the *Runtime* is **Stateless**,
allowing it to be scaled
horizontally. This is beneficial, as user code evaluation poses a chance
for *JVM crashes*.

### Running
This component should be run using the docker image, since it requires
special configuration for all features to work and does now bundle its
dependencies in the build *jar archive*.
#### Docker
The image tag is `ehenoma/jsheets-runtime:latest` and is located in the
[deploy](./deploy) folder.

Following [Docker Compose](https://docs.docker.com/compose/) file is sufficient
to run a configured instance of the runtime.

```yml
version: "3.7"
services:
  runtime:
    image: ehenoma/jsheets-runtime:latest
    container_name: jsheets-runtime
    hostname: jsheets-runtime
    environment:
      JSHEETS_RUNTIME_SERVER_FEATURES_ENABLE_GRPC_REFLECTION: "true"
      JSHEETS_RUNTIME_SERVER_SERVICE_ID: "my-only-service"
   ports:
    - "8080:8080"
```

#### Manual
If you wish to run it manually, ensure that all required libraries are provided
(in the runtime classpath) and open the `jdk.jshell` module to all unnamed modules:
`--add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED`, the latter is required to use
the *exhaustive execution* feature.

For more information inspect the image's [run script](./deploy/entrypoint.sh).

### Configuration

**NOTE** That every environment variable is prefixed with `JSHEETS_RUNTIME_`,
thus `SERVER_PORT` has to be specified as `JSHEETS_RUNTIME_SERVER_PORT`.

| Key | Environment Suffix | Default | Description |
|-----|-------------|---------|-------------|
| server.port | `SERVER_PORT` | `8080` | gRpc Server Port |
| server.features.enableHealthService | `FEATURES_ENABLE_HEALTH_CHECK` | `true` | Toggles the Health Service |
| server.features.enableGrpcReflection | `FEATURES_ENABLE_GRPC_REFLECTION` | `false` | Toggles the Health Service |
|
| service.id | `SERVICE_ID` | *generated* | Id that this service is advertised with |
| service.advertisedHost | `SERVICE_ADVERTISED_HOST` | none | The endpoint that is advertised in the service discovery |
| evaluation.sandbox.disable | `EVALUATION_SANDBOX_DISABLE` | `false` | Disables the sandbox for code execution **dangerous** |
| zookeeper.connectionString | `ZOOKEEPER_CONNECTION_STRING` | none | Connection string to zookeeper |
| zookeeper.connectBackoff | `ZOOKEEPER_CONNECT_BACKOFF` | `1000` | Initial backoff after failed zookeeper connection |
| monitoring.backend | `MONITORING_BACKEND` | none | Backend used for monitoring. If no backend is configured, monitoring is disabled |
| monitoring.influx.userName | `MONITORING_INFLUX_USER_NAME` | none | Influx user name |
| monitoring.influx.authToken | `MONITORING_INFLUX_AUTH_TOKEN` | none | Influx auth token |
| monitoring.influx.uri | `MONITORING_INFLUX_URI` | `http://localhost:8086` | URI of the influx service |
| monitoring.influx.org | `MONITORING_INFLUX_ORG` | none | Influx org |
| monitoring.influx.bucket | `MONITORING_INFLUX_BUCKET ` | `jsheets` | Influx bucket |
| monitoring.influx.step | `MONITORING_INFLUX_STEP` | `10` | Influx reporting interval in seconds |

### Sandboxing
The JVM itself is a sufficient sandbox, if we restrict the methods
that can be called to that of classes without side effects to the system
and prevent `java.lang.reflect` and `java.lang.invoke`, code can
barely do any direct harm (other than using too many resources).

The [evaluation](../evaluation) module provides the
`jsheets.evaluation.sandbox.access` library, that is used to restrict
access to a given list of methods, fields and classes. It is configured
using a text file that looks similar to a `.gitignore`:

```
java.util.List
java.util.Collection
java.lang.Thread#currentThread
!java.lang.Object#wait
```

#### Format
Method signatures can be written as follows:

- `java.lang.Object#equals(java.lang.Object):boolean` is a full signature with
parameter list and return type. It only matches methods that have the exact
same class name, name, parameter and return types.

- `java.lang.Object#equals(*):boolean` or `java.lang.Object#equals:boolean`
has a wildcard parameter list. It matches any method that has the same class
name, name and return type. This is especially useful for methods with many
overloads.

- `java.lang.Object#equals(java.lang.Object):*` or
`java.lang.Object#equals(java.lang.Object)` has a wildcard return type. It
matches any method that has the same class name, name and parameter types.

- `java.lang.Object#equals:*` or `java.lang.Object#equals` has a wildcard
parameter list and return type. It matches any method that has the same class
name and name.

#### Example
Given the following class `Library` in package `evilcorp.coolib`
```java
package evilcorp.coolib;

class Library {
  int count(int[] integers) { /*...*/ }

  int count(double[] doubles) { /*...*/ }

  int count(float[] floats) { /*...*/ }

  long count(int[][] twoDimensionIntegers) { /*...*/ }

  void quit() {
    System.exit(-1);
  }

  void quit(String message) {
    System.err.println(message);
    System.exit(-1);
  }
}
```

we can write the following access graph configs:

```
evilcorp.coolib.Library#count(*)*
```
Here we just enable the methods that we wish to call, but this would be
cumbersome, if we had to do it for every method in a big library.
Instead, we want to exclude the methods that are not allowed:
```
evilcorp.coolib
!evilcorp.coolib.Library#quit
```
Now every class within `evilcorp.coolib` and every of their methods are
allowed, with exception to any method in `evilcorp.coolib.Library` that
is named `quit`.

If this sounds too out of context to you, picture the `java.lang.System` class,
which is a very central and useful class in java's standard library, it contains
fields and methods that are essential to some programs that do not pose any
security risk (like `System#identityHashCode(Object)`), but also methods like
`System#exit(int)`, we would thus be very careful with granting access to
this class.

### Scaling
Since the *runtime* does not save any data and its state only consists of
the active evaluations, it can be scaled horizontally to **thousands** of
instances.

It is important to keep the evaluations per instance fairly low to
reduce the amount of evaluations that are affected by crashes and
lower usage of system resources (such as processors and memory).

### Monitoring
Monitoring can be enabled by specifying a monitoring backend.
Currently, only `influx` is supported.

Set the `JSHEETS_RUNTIME_MONITORING_BACKEND=influx` and configure influx
credentials using
```dotenv
JSHEETS_RUNTIME_MONITORING_INFLUX_USER_NAMKE=${YOUR_USER_NAME}
JSHEETS_RUNTIME_MONITORING_INFLUX_AUTH_TOKEN=${YOUR_TOKEN}
JSHEETS_RUNTIME_MONITORING_INFLUX_BUCKET=${YOUR_BUCKET}
```


### Handling Crashes
If the *runtime* crashes, it is taken out of the service discovery and will not
receive any further requests from the *backend*. Current evaluations will time
out and the backend can choose to retry them or report an error to the client.
Given the deployment strategy, the instance may be recreated immediately
afterwards and put back into the service discovery.

### Future Planning

The current evaluation model is limited:
- Code is restricted (in what libraries and methods it uses)
to prevent security issues and simulate a sandbox.
- Crashes of individual evaluations result in a crash of the entire instance,
thus preempting all other evaluations.

But it provides a somewhat solid and definitely scalable solution for the
first version of *JShell*.

Future implementations may spawn [Containers](https://linuxcontainers.org/)
for every *Sheet* or *Snippet* or *User*. Some features could also be
limited to those who have a *user account*. This container could load some kind
of state, provide access to a small (possibly persistent) file system and more.