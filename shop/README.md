# Lab 2: Dependency Injection

## How to run
- Default: `./mvnw spring-boot:run`  -> EmailNotifier is the @Primary bean
- Dev: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev`
  -> ConsoleNotifier is used
- Tests: `./mvnw test` (no Spring context started)

## Which notifier is used?
| Profile | Primary bean    | Used by `Notifier` injection |
|---------|-----------------|------------------------------|
| default | EmailNotifier   | EmailNotifier                |
| dev     | ConsoleNotifier | ConsoleNotifier              |

`WelcomeService` always gets `SmsNotifier` for the urgent path because
`@Qualifier("smsNotifier")` overrides `@Primary`.

## Two @Primary beans: what decides?
`@Primary` only breaks a tie among candidates. If exactly one active
candidate is primary, it wins. If two or more active candidates are
primary (EmailNotifier and ConsoleNotifier with `dev` and no `@Profile("!dev")`),
Spring cannot choose and fails at startup with `NoUniqueBeanDefinitionException`.
Fix: make only one primary bean active per profile (`@Profile("!dev")`
on EmailNotifier) or use `@Qualifier`, which always beats `@Primary`.

## Prototype scope
`ReportBuilder` is `@Scope("prototype")`. `ObjectProvider.getObject()`
returns a new instance each call; the identity hash codes differ.
Injecting it directly into a singleton would give only one instance.