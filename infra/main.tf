provider "aws" {
  region = "us-east-1"
  profile = "default"
  max_retries = 3
}

resource "aws_cognito_user_pool" "pool" {
  name = "standardUserPool"
}
