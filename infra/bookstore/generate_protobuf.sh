export PROJECT_HOME=/Users/datle/Workspace/palexy/gcp/esp-authentication

# Generate API descriptor and .pb2 files
cd $PROJECT_HOME || exit & \
python3 -m grpc_tools.protoc \
    --include_imports \
    --include_source_info \
    --proto_path=proto \
    --descriptor_set_out=infra/bookstore/bookstore_api_descriptor.pb \
    --python_out=python \
    --grpc_python_out=python \
    bookstore.proto
