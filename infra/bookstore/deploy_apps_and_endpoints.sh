#!/bin/bash

export PROJECT_HOME=/Users/datle/Workspace/example/gcp/esp-authentication
export GCP_PID=experimental-229809
export CLUSTER_ID=autopilot-cluster-1
export ZONE=asia-southeast1
export SERVICE_ACCOUNT_NAME=experimental-general

cd $PROJECT_HOME/infra/bookstore || exit

gcloud config set project $GCP_PID
gcloud container clusters get-credentials $CLUSTER_ID --zone $ZONE

# Deploy endpoints.
gcloud endpoints services deploy bookstore_api_descriptor.pb api_config.yaml
gcloud endpoints services deploy bookstore_api_descriptor.pb api_config_auth.yaml

# This account is used as service controller and checking the status of the endpoint
gcloud endpoints services add-iam-policy-binding bookstore.endpoints.$GCP_PID.cloud.goog \
  --member serviceAccount:$SERVICE_ACCOUNT_NAME@$GCP_PID.iam.gserviceaccount.com \
  --role roles/servicemanagement.admin

# Deploy the gRPC server
kubectl create -f grpc-bookstore.yaml --dry-run=client -o yaml | kubectl apply -f -
