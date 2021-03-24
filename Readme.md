

# Useful Links
# Postgres
https://docs.boundlessgeo.com/suite/1.1.1/dataadmin/pgGettingStarted/firstconnect.html
https://scala-slick.org/doc/3.3.0/gettingstarted.html
# Stripe
https://stripe.com/docs/connect/collect-then-transfer-guide
# Status
![Scala CI](https://github.com/sdrafahl/gardenShare/workflows/Scala%20CI/badge.svg)

# Setup Locally
1. Install Postgres, the password and username must both be "postgres" to run locally.
2. Install SBT, https://www.scala-sbt.org/
3. Get AWS credentials
4. Install Terraform CLI
5. Go to the infra directory in a terminal, then enter ``terraform init`` then ``terraform apply``
6. In the root of the project run "sbt run"

   
# Apply To Be Seller

1. Apply to become a seller, include the address of where you will be selling from, also include a return URL and a refresh URL for where you want the user to be redirected to after filling out Stripe flow. 
It will return a URL to the Stripe flow.

```
POST
<Root>/user/apply-to-become-seller
Headers: JWT token
Body: {
	Address,
    refreshUrl,
    returnUrl
}

```

2. After the user has been redirected the server will need to verify the user has completed the flow to create them as a seller in the system. You will need to call to verify when the user returns to wherever the refresh url brings them.

```
POST 
<Root>/user/verify-user-as-seller
Headers: JWT token
Body: {
	Address
}
```
