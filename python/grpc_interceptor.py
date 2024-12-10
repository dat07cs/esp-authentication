import collections
import time
from asyncio import Future
from dataclasses import dataclass
from typing import Any, Callable, Optional, Tuple, Sequence

import grpc

from jwt_token import generate_jwt, AuthToken, ServiceAccountAuth


class ClientCallDetails(
    collections.namedtuple("_ClientCallDetails", ("method", "timeout", "metadata", "credentials")),
    grpc.ClientCallDetails,
):
    pass


_Request = Any
_Response = Any


_InterceptorFunction = Callable[
    [ClientCallDetails, _Request],
    tuple[ClientCallDetails, Optional[_Request], Optional[_Response]],
]


class _GenericClientInterceptor(grpc.UnaryUnaryClientInterceptor):
    def __init__(self, interceptor_function: _InterceptorFunction):
        self._fn = interceptor_function

    def intercept_unary_unary(self, continuation, client_call_details: ClientCallDetails, request: _Request):
        new_details, new_request, new_response = self._fn(
            client_call_details,
            request,
        )
        print(new_details.metadata)
        if new_response:
            future = Future()
            future.set_result(new_response)
            return future

        return continuation(new_details, new_request)


@dataclass(frozen=True)
class AuthOptions:
    api_key: Optional[str] = None
    service_account_auth: Optional[ServiceAccountAuth] = None


class AuthInterceptor(_GenericClientInterceptor):
    def __init__(self, auth_options: AuthOptions):
        super().__init__(self._create())
        self._auth_options = auth_options if auth_options else AuthOptions()
        self._cached_sa_auth_token: Optional[AuthToken] = None

    def _api_key(self):
        return self._auth_options.api_key if self._auth_options else None

    def _sa_auth_token(self):
        if not self._auth_options.service_account_auth:
            return None
        # discard current token if it's going to expire soon
        if not self._cached_sa_auth_token or self._cached_sa_auth_token.expiry_time < time.time() - 60:
            self._cached_sa_auth_token = generate_jwt(self._auth_options.service_account_auth)
        return self._cached_sa_auth_token.token

    def _create(self):
        _self = self
        def intercept(client_call_details: ClientCallDetails, request: _Request):
            metadata = []
            if client_call_details.metadata is not None:
                metadata = list(client_call_details.metadata)
            api_key = _self._api_key()
            if api_key:
                metadata.append(("x-api-key", api_key))
            sa_auth_token = _self._sa_auth_token()
            if sa_auth_token:
                metadata.append(("authorization", f"Bearer {sa_auth_token}"))
            client_call_details = ClientCallDetails(
                client_call_details.method, client_call_details.timeout, metadata, client_call_details.credentials
            )
            return client_call_details, request, None

        return intercept

    def intercept_unary_unary(self, continuation, client_call_details: ClientCallDetails, request: _Request):
        try:
            return super().intercept_unary_unary(continuation, client_call_details, request)
        except grpc.RpcError as e:
            cached_value = None
            if e.code() == grpc.StatusCode.UNAUTHENTICATED:
                cached_value = self._cached_sa_auth_token
                self._cached_sa_auth_token = None
            # retry with a newly created token in case the existing token has expired already
            if cached_value is not None:
                return super().intercept_unary_unary(continuation, client_call_details, request)
            else:
                raise e


def insecure_channel(
        target: str,
        auth_options: Optional[AuthOptions] = None,
        channel_options: Optional[Sequence[Tuple[str, Any]]] = None
) -> grpc.Channel:
    """
    Args:
        target: The server address.
        auth_options: An optional authentication options object for gRPC requests.
        channel_options: An optional list of key-value pairs (:term:`channel_arguments`
        in gRPC Core runtime) to configure the channel.
    """
    interceptors = []
    if auth_options:
        interceptors.append(AuthInterceptor(auth_options))

    return grpc.intercept_channel(grpc.insecure_channel(target, options=channel_options), *interceptors)
