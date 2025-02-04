import grpc
from google.protobuf import empty_pb2

import bookstore_pb2
import bookstore_pb2_grpc
import hello_pb2
import hello_pb2_grpc
from grpc_interceptor import create_channel, AuthOptions, ServiceAccountAuth


def run(host: str, port: int, *,
        api_key: str = None, service_account_path: str = None, service_name: str = None,
        insecure: bool = False, cacert_path: str = None):
    timeout = 3  # seconds
    auth_options = AuthOptions(
        api_key=api_key,
        service_account_auth=ServiceAccountAuth.of(
            service_account_path=service_account_path,
            service_name=service_name
        ) if service_account_path else None
    )
    cacert = None
    if cacert_path:
        with open(cacert_path, "rb") as f:
            cacert = f.read()
    channel = create_channel(f"{host}:{port}", auth_options=auth_options, insecure=insecure, cacert=cacert)
    stub = bookstore_pb2_grpc.BookstoreStub(channel)

    try:
        shelves = stub.ListShelves(empty_pb2.Empty(), timeout)
        print(f"ListShelves: {shelves}")

        first_shelf = stub.GetShelf(bookstore_pb2.GetShelfRequest(shelf=shelves.shelves[0].id), timeout)
        print(f"GetShelf: {first_shelf}")

        stub.DeleteShelf(bookstore_pb2.DeleteShelfRequest(shelf=-1), timeout)
        print("DeleteShelf")
    except grpc.RpcError as rpc_error:
        print(f"{rpc_error.code()}: {rpc_error.details()}")
        # raise rpc_error
    finally:
        channel.close()


def hello():
    timeout = 3  # seconds
    channel = create_channel("examples.http-client.intellij.net:443")
    stub = hello_pb2_grpc.HelloServiceStub(channel)

    try:
        resp = stub.SayHello(hello_pb2.HelloRequest(greeting="This is greeting"), timeout)
        print(f"SayHello: {resp}")
    except grpc.RpcError as rpc_error:
        print(f"{rpc_error.code()}: {rpc_error.details()}")
        raise rpc_error
    finally:
        channel.close()


if __name__ == "__main__":
    _host = "34.87.33.91"
    _api_key_port = 9000
    _api_key_ssl_port = 443
    _auth_token_port = 9001
    _api_key = None
    _service_account_file = "/Users/datle/experimental-229809-32e56a607af1.json"  # dat-service-account
    _cacert_file = "/Users/datle/Downloads/bookstore.example.com.pem"
    _service_name = "bookstore2.endpoints.experimental-229809.cloud.goog"

    # hello()
    # run(_host, _api_key_port, insecure=True)
    # run(_host, _api_key_ssl_port, api_key=_api_key, cacert_path=_cacert_file)
    run(_host, _auth_token_port, service_account_path=_service_account_file, service_name=_service_name,
        cacert_path=_cacert_file)
