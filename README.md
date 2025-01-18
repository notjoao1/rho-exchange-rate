# RHO Challenge - Currency Exchange API

## System Outline

- Users want to check conversion rates between currencies A and B, and we have to provide this data efficiently.

- We also need to have security in our system, meaning rate limiting + authn/authz

- We should cache currency conversions. After a user requests conversion from currency A to currency B, we should save (for a configurable amount of time) the conversion between those currencies. (probably save A ---> B and B ---> A, as they are the same, only inverted).

- Another optimization is to query available currencies only a single time and persist that data. They almost never change, and we can easily prevent users from querying for unexistent currencies, which would trigger unnecessary external API calls.

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
