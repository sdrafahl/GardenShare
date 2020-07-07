#!/bin/bash

openssl genrsa -out priv.pem 2048
openssl rsa -in priv.pem -pubout -out public.pub
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in priv.pem -out private.pem
