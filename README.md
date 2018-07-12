# Agents External Stubs Frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs-frontend/_latestVersion)

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENT_MTD -f
    sm --stop AGENTS_EXTERNAL_STUBS_FRONTEND
    sbt run

It should then be listening on port 9099

    browse http://localhost:9099/agents-external-stubs

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
