import grpc
from google.protobuf import empty_pb2

import bookstore_pb2
import bookstore_pb2_grpc
from grpc_interceptor import insecure_channel, AuthOptions, ServiceAccountAuth


def run(host: str, port: int, api_key: str = None, service_account_path: str = None, service_name: str = None):
    timeout = 3  # seconds
    auth_options = AuthOptions(
        api_key=api_key,
        service_account_auth=ServiceAccountAuth.of(
            service_account_path=service_account_path,
            service_name=service_name
        )
    )
    channel = insecure_channel(f"{host}:{port}", auth_options=auth_options)
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
    finally:
        channel.close()


if __name__ == "__main__":
    _host = "34.143.204.26"
    _api_key_port = 9000
    _auth_token_port = 9001
    _service_account_file = "/Users/datle/experimental-229809-32e56a607af1.json"  # dat-service-account
    _service_name = "bookstore2.endpoints.experimental-229809.cloud.goog"

    run(_host, _auth_token_port, service_account_path=_service_account_file, service_name=_service_name)
