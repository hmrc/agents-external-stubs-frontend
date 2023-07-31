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
Every stubbed user and other data live is some test sandbox (planet). 
You have to declare existing or a new planet whenever you sign-in. Each authenticated session have planetId information. 
Stubbed and custom UIs will consider only users and data assigned to the current planet.

User authentication expires after 15 minutes and so does the bearer token.
All users and other data on each planet are removed after 12h unless marked as permanent.

## Stubbed UIs

### [BAS Gateway Frontend](https://github.com/hmrc/bas-gateway-frontend/blob/master/README.md)
#### GET         /bas-gateway/sign-in  
Initiates the login journey, eventually creating an authenticated MDTP session                                      
#### GET         /bas-gateway/sso-sign-in
Initiates the sso login flow for BAS through MDTP
#### GET         /bas-gateway/sign-out-without-state
#### GET         /bas-gateway/sign-out-with-state
#### GET         /bas-gateway/register 
Sends the user to register a new SCP account

### [Company Auth Frontend](https://github.com/hmrc/company-auth-frontend/blob/master/README.md)
#### GET /gg/sign-in?continue=:continueUrl&accountType=:accountType&origin=:origin
Shows login page.

#### POST /gg/sign-in?continue=:continueUrl&accountType=:accountType&origin=:origin
Submits user's credentials, creates session and redirects to the provided continue URL

### [Identity Verification Frontend](https://github.com/hmrc/identity-verification-frontend/blob/master/README.md) 
#### GET /mdtp/uplift?origin=:origin&confidenceLevel=:confidenceLevel&completionURL=:completionURL&failureURL=:failureURL
Shows an identity verification page for the [IV "uplift" journey](https://github.com/hmrc/identity-verification-frontend/blob/master/README.md#get-mdtpuplift), offering the choice of either a successful or unsuccessful IV journey outcome.

#### POST /mdtp/uplift?origin=:origin&confidenceLevel=:confidenceLevel&completionURL=:completionURL&failureURL=:failureURL
Submits the desired outcome of the identity verification "uplift" journey.
If a successful outcome was chosen, this will increase the confidence level of the current user to the provided confidence level and redirect to the provided completion URL.
If an unsuccessful outcome was chosen, this will just redirect to the provided failure URL.

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

    sm2 --start AWESOME_STUBS -f
    sm2 --start AWESOME_STUBS_FRONTEND -f
    
or with AGENTS_EXTERNAL_STUBS also running
    sbt run

It should then be listening on ports 9099 and 9025

    browse http://localhost:9099/

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
