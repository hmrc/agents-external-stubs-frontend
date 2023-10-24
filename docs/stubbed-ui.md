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
