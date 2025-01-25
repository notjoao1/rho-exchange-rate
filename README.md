# RHO Challenge - Currency Exchange API

## System Outline

- Users want to check conversion rates between currencies A and B, and we have to provide this data efficiently.

- We also need to have security in our system, meaning rate limiting + authn/authz

- We should cache currency conversions. After a user requests conversion from currency A to currency B, we should save (for a configurable amount of time) the conversion between those currencies. (probably save A ---> B and B ---> A, as they are the same, only inverted).

- Another optimization is to query available currencies only a single time and cache that data for a longer time. They almost never change, and we can easily prevent users from querying for unexistent currencies, which would trigger unnecessary external API calls.

- Unit tests with mocked dependencies should be employed

```
Currency A -> B: 1.2

That means

Currency B -> A: 1/1.2 (0.8333333).

Proof:

a = 1.2b ==> b = a/1.2 ==> b = (1/1.2)a
```

## Architecture

- **Backend:** Spring Boot
- **Persistent Storage:** SQLite - why? Good enough for the challenge (a small dependency)
- **Caching:** Redis - why? Because if we end up scaling our backend to multiple instances, they could all share a single cache, which reduces the overall number of external API requests

## Plan

1. Implement basic currency API with no caching or security

2. Cache API calls with Redis for a configurable amount of time

2.1. Cache existing currencies to prevent useless requests that might eat up external API calls

3. Deal with user management - creating accounts and associating API keys to those accounts - the plan is to simply generate a 32 byte long string, encoding it in Base 64 and using that. Users should have an associated API key upon account creation, and can invalidate it by passing their password, which makes the old one invalid and creates a new one.

4. Rate limiting for API keys. Manage this through Redis, so that, once again, multiple instances of the API could all talk to the same Redis instance and manage rate limiting efficiently (note: use Spring Boot Filters for this)

5. Code styling


## TODOs

(above -> higher priority)
- Rate limiting/auth - the plan is simple - user/password sign up + users have an API key + we rate limit on API key with Spring Filters after validating the API keys actually exist.
- Unit tests
- Swagger documentation
- `Dockerfile` requires that the JAR file is already built, need to automate it so that running the whole system from scratch is as simple as running a single docker compose command
- LOW PRIORITY: Optimizing queries by querying list of currencies ahead of time and saving it to either Redis or a persistent store. If redis - use lazy loaded singleton that queries all currencies and keeps them in an immutable. Otherwise, just save to DB statically (maybe flyway migration to initialize the data), index column, and query when requests are made

## Notes

- Code is formatted using the [`Spotless`](https://github.com/diffplug/spotless/tree/main/plugin-maven) Maven plugin, following [Google Java Style](https://google.github.io/styleguide/javaguide.html), with the `AOSP` variation.

- To check formatting:

```sh
mvn spotless:check # or just mvn verify
```

- To apply formatting:

```sh
mvn spotless:apply
```