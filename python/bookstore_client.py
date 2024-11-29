from google.protobuf import empty_pb2
import grpc

import bookstore_pb2
import bookstore_pb2_grpc
from python.jwt_token_gen import generate_jwt


def run(host: str, port: int, api_key: str = None, auth_token: str = None):
    timeout = 10  # seconds
    channel = grpc.insecure_channel(f"{host}:{port}")
    stub = bookstore_pb2_grpc.BookstoreStub(channel)
    metadata = []
    if api_key:
        metadata.append(("x-api-key", api_key))
    if auth_token:
        metadata.append(("authorization", f"Bearer {auth_token}"))

    try:
        shelves = stub.ListShelves(empty_pb2.Empty(), timeout, metadata=metadata)
        print(f"ListShelves: {shelves}")

        first_shelf = stub.GetShelf(bookstore_pb2.GetShelfRequest(shelf=shelves.shelves[0].id), timeout, metadata=metadata)
        print(f"GetShelf: {first_shelf}")

        stub.DeleteShelf(bookstore_pb2.DeleteShelfRequest(shelf=-1), timeout, metadata=metadata)
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

    _signed_jwt = generate_jwt(_service_account_file, _service_name)

    run(_host, _auth_token_port, auth_token=_signed_jwt)
