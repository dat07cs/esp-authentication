import json
import time
from dataclasses import dataclass
from types import SimpleNamespace
from typing import Mapping

import google.auth.crypt
import google.auth.jwt
import google.auth.transport.requests
import google.oauth2.id_token

"""Max lifetime of the token (one hour, in seconds)."""
MAX_TOKEN_LIFETIME_SECS = 3600


@dataclass(frozen=True)
class ServiceAccountInfo:
    client_email: str
    private_key_id: str
    private_key: str


@dataclass(frozen=True)
class ServiceAccountAuth:
    service_account_info: ServiceAccountInfo
    service_name: str
    max_token_lifetime_seconds: int = MAX_TOKEN_LIFETIME_SECS

    @staticmethod
    def of(service_account_path: str, service_name: str,
            max_token_lifetime_seconds: int = MAX_TOKEN_LIFETIME_SECS) -> 'ServiceAccountAuth':
        with open(service_account_path) as f:
            service_account_info = json.load(f, object_hook=lambda d: SimpleNamespace(**d))
        return ServiceAccountAuth(service_account_info, service_name, max_token_lifetime_seconds)


@dataclass(frozen=True)
class AuthToken:
    token: str
    expiry_time: int  # seconds since epoch


def generate_jwt(service_account_auth: ServiceAccountAuth, *, issuer: str = None) -> AuthToken:
    """ Generates a signed JSON Web Token using a Google API Service Account.

    Args:
        service_account_auth (ServiceAccountAuth): Service account information for authentication.
        issuer (str): An optional issuer of the token.
            If not specified, client email of the service account will be used.

    Returns:
        AuthToken: The generated authentication object.
    """
    service_account_info = service_account_auth.service_account_info
    signer = google.auth.crypt.RSASigner.from_string(
        service_account_info.private_key, service_account_info.private_key_id
    )

    client_email = service_account_info.client_email
    if not issuer:
        issuer = client_email

    now = int(time.time())
    expiry_time = now + service_account_auth.max_token_lifetime_seconds

    payload = {
        "iat": now,
        "exp": expiry_time,
        "aud": service_account_auth.service_name,
        # iss must match 'issuer' in the security configuration
        "iss": issuer,
        # sub is mapped to the user email
        "sub": client_email,
    }

    signed_token = google.auth.jwt.encode(signer, payload).decode("utf-8")
    return AuthToken(signed_token, expiry_time)


def verify(service_account_auth: ServiceAccountAuth, token: str) -> Mapping[str, str]:
    client_email = service_account_auth.service_account_info.client_email
    jwks_uri = f"https://www.googleapis.com/robot/v1/metadata/x509/{client_email}"
    return google.oauth2.id_token.verify_token(token,
                                               request=google.auth.transport.requests.Request(),
                                               audience=service_account_auth.service_name,
                                               certs_url=jwks_uri)


if __name__ == "__main__":
    _service_account_file = "/Users/datle/experimental-229809-32e56a607af1.json"
    _service_name = "bookstore2.endpoints.experimental-229809.cloud.goog"
    _token = ("eyJraWQiOiIzMmU1NmE2MDdhZjE3YzQ0NGViNTg4MjMwZGM4YjBmY2Y2Yjc2NTM2IiwiYWxnIjoiUlMyNTYiLCJ0eXAiOiJKV1QifQ"
              ".eyJpc3MiOiJkYXQtc2VydmljZS1hY2NvdW50QGV4cGVyaW1lbnRhbC0yMjk4MDkuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLCJzd"
              "WIiOiJkYXQtc2VydmljZS1hY2NvdW50QGV4cGVyaW1lbnRhbC0yMjk4MDkuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLCJhdWQiOiJ"
              "ib29rc3RvcmUyLmVuZHBvaW50cy5leHBlcmltZW50YWwtMjI5ODA5LmNsb3VkLmdvb2ciLCJpYXQiOjE3MzM4MDEzNjUsImV4cCI6M"
              "TczMzgwMTQ4NX0.hlff8t0x8mqhpp9hKZQ1VintZqMrM4CozFyr2UQpdeFGspZ5sp-rMEvSty7FUlEQtAp6USQre6F7GBP_MU6IKPa"
              "LdNdISovddSZdIsATxRCsioNkSU1TvPFhrE9BKbpkwkFabrPvYU5_A4XXuLnQj-IQiTH79bG8IYSg3GQyc-sLCDoPWVdiQsQCWKN18"
              "08x-br-7UYcWnp8D-6TLQOIWaBEx34xE18TeuHq7iwPy_zYsJopGmwOGPVqkhuXkVNuq6nEac2hnE0C7UPD3VNggUyP6eVBosq93Ym"
              "VMqlDc82K2tnFNPHGXxD0Xjybc9S2XWn-ROxMQjH8WcBxdpPeyA")
    _service_account_auth = ServiceAccountAuth.of(_service_account_file, _service_name)
    try:
        verify(_service_account_auth, _token)
    except Exception as e:
        print(e)

    _auth_token = generate_jwt(_service_account_auth)
    print(_auth_token)
    print(verify(_service_account_auth, _auth_token.token))
