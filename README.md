# Agents External Stubs Frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs-frontend/_latestVersion)

This microservice is part of Agent Services local testing framework, 
providing necessary UI stubs complementing API stubs in [agents-external-stubs](https://github.com/hmrc/agents-external-stubs).

This app SHOULD NOT be run on QA nor Production environment.

## How requests to the stubbed UIs are handled?

To handle requests aimed at stubbed frontend microservices we provide necessary TCP proxies:

- listening on 9025 for company-auth-frontend requests

You can switch this behaviour off by setting `proxies.start` config property to `false`.

## Data Model
Every stubbed user and other data live on some test planet. 
You have to declare existing or a new planet whenever you sign-in. Each authenticated session have planetId information. 
Stubbed and custom UIs will consider only users and data assigned to the current planet.

## Stubbed UIs

### [Company Auth Frontend](https://github.com/hmrc/company-auth-frontend/blob/master/README.md)
#### GET /gg/sign-in?continue=:continueUrl&accountType=:accountType&origin=:origin
Shows login page.

#### POST /gg/sign-in?continue=:continueUrl&accountType=:accountType&origin=:origin
Submits user's credentials, creates session and redirects to the provided continue URL

## Custom UI

#### GET /agents-external-stubs/user
Shows current user auth settings

#### GET /agents-external-stubs/user/edit
Displays form to edit current user auth settings

#### POST /agents-external-stubs/user/edit
Submits amended user auth settings

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENTS_EXTERNAL_STUBS_FRONTEND -f
    
or
    
    sbt run

It should then be listening on ports 9099 and 9025

    browse http://localhost:9099/

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
