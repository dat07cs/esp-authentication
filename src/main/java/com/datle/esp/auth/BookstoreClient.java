package com.datle.esp.auth;

import com.google.endpoints.examples.bookstore.BookstoreGrpc;
import com.google.endpoints.examples.bookstore.BookstoreGrpc.BookstoreBlockingStub;
import com.google.endpoints.examples.bookstore.DeleteShelfRequest;
import com.google.endpoints.examples.bookstore.GetShelfRequest;
import com.google.endpoints.examples.bookstore.ListShelvesResponse;
import com.google.endpoints.examples.bookstore.Shelf;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;

public class BookstoreClient {

    public static void main(String[] args) throws IOException {
        String host = "34.143.204.26";
        int apiKeyPort = 9000;
        int authTokenPort = 9001;
        String serviceAccountFilePath = "/Users/datle/experimental-229809-32e56a607af1.json"; // dat-service-account
        String serviceName = "bookstore2.endpoints.experimental-229809.cloud.goog";

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, authTokenPort)
                .usePlaintext()
                .intercept(new AuthInterceptor(serviceAccountFilePath, serviceName, /*apiKey=*/null))
                .build();
        BookstoreBlockingStub stub = BookstoreGrpc.newBlockingStub(channel);

        try {
            ListShelvesResponse listShelvesResponse = stub.listShelves(Empty.getDefaultInstance());
            System.out.printf("listShelves=%s%n", listShelvesResponse);

            Shelf firstShelf = stub.getShelf(
                    GetShelfRequest.newBuilder().setShelf(listShelvesResponse.getShelves(0).getId()).build());
            System.out.printf("getShelf=%s%n", firstShelf);

            stub.deleteShelf(DeleteShelfRequest.newBuilder().setShelf(-1).build());
            System.out.printf("deleteShelf%n");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }
}
