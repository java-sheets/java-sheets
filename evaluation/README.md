## Events
When building the `EvaluationEngine` you can pass an `EventSink`, it will then
post events type `EvaluationEvent`. Those events can be used to record
metrics and run custom code with pretty loose coupling.

If no `EventSink` is configured, the engine will typically not attempt to
create any events, thus no overhead is produced.

Following events are currently posted:

| Name  | Posted by | When   |
|-------|-----------|--------|
| `EvaluationStartEvent` | `EvaluationEngine` | An evaluation begins
| `EvaluationStopEvent` | `EvaluationEngine` | An evaluation completes or fails
| `BoxLifecycleEvent` | `ForkedExecutionEnvironment` | A JVM is starting/ready/stopping |