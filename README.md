# Sample Bookstore service using ESP with authentication

## Environments
- Java 17.
- Maven 3.9.x.
- Python 3.9 with pip installed.
- [Setup Google Cloud project](https://cloud.google.com/endpoints/docs/grpc/get-started-kubernetes#before-you-begin).
  - Ensure that the required services have been enabled.
  ```shell
  gcloud services list | grep "service"
  ```
  | Name                             | Title                  |
  |----------------------------------|------------------------|
  | servicemanagement.googleapis.com | Service Management API |
  | servicecontrol.googleapis.com    | Service Control API    |

  - Enable the services if not yet enabled.
  ```shell
  gcloud services enable servicemanagement.googleapis.com
  gcloud services enable servicecontrol.googleapis.com
  ```
  
  - Create a new cluster if needed. Cluster's name and zone will be used later.

## Generate descriptor file for ESP and pb2 files for Python code
- [proto/bookstore.proto](proto/bookstore.proto) will be used in this sample.
- Update PROJECT_HOME and run the script at
  [infra/bookstore/generate_protobuf.sh](infra/bookstore/generate_protobuf.sh). 

## Deploy gRPC server and related services
- ESP v1 (nginx-based) will be used.
- The project ID in this example if `experimental-229809`, change it in the configuration files if needed.
- Create 2 service accounts
  - experimental-general: service account for testing purpose and controlling the endpoint services.
    - Create secret for this service account to use later in deployments.
    ```shell
    # 
    kubectl create secret generic gcloud-service-account \
    --from-file=experimental-general=/Users/datle/experimental-229809-a54f92e47b90.json \
    --dry-run=client -o yaml | kubectl apply -f -
    ```
  - dat-service-account: just another account for testing purpose.
- There are 2 endpoint configurations to demonstrate different
  [authentication methods](https://cloud.google.com/endpoints/docs/openapi/authentication-method):
  - [infra/bookstore/api_config.yaml](infra/bookstore/api_config.yaml):
    - API key is required to call this endpoint by setting `usage.rules.allow_unregistered_calls=false`.
    - API keys are meant to identify the source project calling the API rather than authenticating the user.
  - [infra/bookstore/api_config_auth.yaml](infra/bookstore/api_config_auth.yaml):
    - Use service accounts to authenticate by specifying authentication rules.
    - There are a limit to 100 servicer accounts per project and 10 keys per service account by default.
- Deployment and service configurations are at [infra/bookstore/grpc-bookstore.yaml](infra/bookstore/grpc-bookstore.yaml).
  - Use 2 different containers for the above 2 different endpoint services.
  - Use the sample gRPC Bookstore server image as backend
    ([github](https://github.com/GoogleCloudPlatform/python-docs-samples/tree/main/endpoints/bookstore-grpc)).
- Update environment variables and run the script at
  [infra/bookstore/deploy_apps_and_endpoints.sh](infra/bookstore/deploy_apps_and_endpoints.sh).

## Testing
### Java
- [JwtTokenGen.java](src/main/java/com/datle/esp/auth/JwtTokenGen.java)
```logs
listShelves=shelves {
  id: 1
  theme: "Fiction"
}
shelves {
  id: 2
  theme: "Fantasy"
}

getShelf=id: 1
theme: "Fiction"

UNAUTHENTICATED: JWT validation failed: Issuer not allowed 
```

### Python
- [python/bookstore_client.py](python/bookstore_client.py)
```logs
ListShelves: shelves {
  id: 1
  theme: "Fiction"
}
shelves {
  id: 2
  theme: "Fantasy"
}

GetShelf: id: 1
theme: "Fiction"

StatusCode.UNAUTHENTICATED: JWT validation failed: Issuer not allowed 
```

## Clean up
- Delete endpoints.
```shell
gcloud endpoints services delete SERVICE_NAME
```

- Delete cluster.
```shell
gcloud container clusters delete NAME --zone ZONE
```
