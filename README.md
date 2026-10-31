# Lab 02 - Task 1

## Answers to README questions:
**1. What does the `<parent>` give you?**
The `<parent>` (spring-boot-starter-parent) provides default dependency management, plugin configuration, and a curated list of compatible library versions. This means you don't need to specify versions for most dependencies, which prevents version conflicts.

**2. Why do the starters have no `<version>`?**
Because the parent POM already defines the versions for all Spring Boot starters. It ensures all dependencies are compatible with the specific Spring Boot version you are using and reduces boilerplate code.

**3. What does `spring-boot-maven-plugin` do at package time?**
It repackages the standard `.jar` file into an executable "fat jar" (or uber jar). It bundles all the application's dependencies inside the jar and adds a manifest file so the jar can be run directly with `java -jar`.

**4. Why is the jar large?**
The jar is large because it contains the entire Spring Boot framework, Tomcat web server, and all the dependencies bundled inside it, so it can run on any machine without needing an external server or pre-installed libraries.

**5. Explain the condition evaluation report line:**
It says `DataSourceAutoConfiguration matched` because I added the JDBC and H2 dependencies. Spring Boot automatically detected that a database connection is needed, so it configured an in-memory H2 database for me without requiring any manual setup.

## Task 3 Answers

**1. Is there a Database and JdbcClient connection?**
Yes. Spring Boot automatically configured an H2 in-memory database. The `JdbcClient` is successfully injecting and executing SQL queries against it.

**2. Can you prove that the transaction rolls back?**
Yes. In the output, "Keyboard" was saved, but immediately after, an exception was thrown. The final database query showed only "Laptop" and "Mouse". This proves `@Transactional` rolled back the "Keyboard" insertion when the error occurred.