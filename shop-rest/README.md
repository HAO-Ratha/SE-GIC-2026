# shop-rest (Lab 04)

Jersey (JAX-RS) on an embedded Grizzly server, with an in-memory product repository.

## Run

```
mvn -q compile exec:java
```

The server listens on http://localhost:8080/api/

## Task 1 notes

The resource class has no mutable fields because Jersey creates a new instance for each request, so it only holds a `final` reference to the shared, thread-safe repository and keeps no per-request state of its own.