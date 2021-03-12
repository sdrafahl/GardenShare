

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

   
