# Agents External Stubs Frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs-frontend/_latestVersion)

This microservice is part of Agent Services local testing framework, 
providing dynamic stubs for some UI journeys.


This app SHOULD NOT be run on QA and Production environment.

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENTS_EXTERNAL_STUBS_FRONTEND -f
    
or
    
    sbt run

It should then be listening on port 9099

    browse http://localhost:9099/

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
