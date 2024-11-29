#!/usr/bin/env python

"""Example of generating a JWT signed from a service account file."""

import json
import time

import google.auth.crypt
import google.auth.jwt

"""Max lifetime of the token (one hour, in seconds)."""
MAX_TOKEN_LIFETIME_SECS = 3600


def generate_jwt(service_account_file: str, service_name: str, issuer: str = None) -> str:
    """Generates a signed JSON Web Token using a Google API Service Account."""
    with open(service_account_file) as fh:
        service_account_info = json.load(fh)

    signer = google.auth.crypt.RSASigner.from_string(
        service_account_info["private_key"], service_account_info["private_key_id"]
    )

    client_email = service_account_info["client_email"]
    if not issuer:
        issuer = client_email

    now = int(time.time())

    payload = {
        "iat": now,
        "exp": now + MAX_TOKEN_LIFETIME_SECS,
        "aud": service_name,
        # iss must match 'issuer' in the security configuration
        "iss": issuer,
        # sub is mapped to the user email
        "sub": client_email,
    }

    return google.auth.jwt.encode(signer, payload).decode("utf-8")


if __name__ == "__main__":
    _service_account_file = "/Users/datle/experimental-229809-32e56a607af1.json"
    _service_name = "bookstore2.endpoints.experimental-229809.cloud.goog"

    _signed_jwt = generate_jwt(_service_account_file, _service_name)
    print(_signed_jwt)
