# Agents External Stubs Frontend

This microservice is part of Agent Services local testing framework, 
providing necessary UI stubs complementing API stubs in [agents-external-stubs](https://github.com/hmrc/agents-external-stubs).

This app SHOULD NOT be run on QA nor Production environment.

## How requests to the stubbed UIs are handled?

To handle requests aimed at stubbed frontend microservices we provide necessary TCP proxies:

- listening on 9025 for company-auth-frontend requests

You can switch this behaviour off by setting `proxies.start` config property to `false`.

## Data Model
Every stubbed user and other data live in some test sandbox (planet). 
You have to declare existing or a new planet whenever you sign-in. Each authenticated session has planetId information. 
Stubbed and custom UIs will consider only users and data assigned to the current planet. Other apps may contain data from multiple planets and can clash, so avoid the same names for your users with concurrent planets or tidy up data on these regularly.

User authentication expires after 15 minutes and so does the bearer token.
All users and other data on each planet are removed after 12h unless marked as permanent.

## Stubbed UIs

See [stubbed UIs](docs/stubbed-ui.md)

## Running the tests

    sbt test it/test

## Running the tests with coverage

    sbt clean coverage test it/test coverageReport

## Running the app locally

    sbt run

## Running the app and associated backend service via Service Manager

    sm2 --start AGENTS_EXTERNAL_STUBS_FRONTEND
    sm2 --start AGENTS_EXTERNAL_STUBS

It should then be listening on ports 9099 and 9009. You could then start using the service at the quick start hub:

    http://localhost:9099/agents-external-stubs/quick-start-hub

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
