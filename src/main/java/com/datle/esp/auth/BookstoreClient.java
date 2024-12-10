package com.datle.esp.auth;

import com.datle.esp.auth.AuthInterceptor.AuthOptions;
import com.datle.esp.auth.JwtToken.ServiceAccountAuth;
import com.google.endpoints.examples.bookstore.BookstoreGrpc;
import com.google.endpoints.examples.bookstore.BookstoreGrpc.BookstoreBlockingStub;
import com.google.endpoints.examples.bookstore.DeleteShelfRequest;
import com.google.endpoints.examples.bookstore.GetShelfRequest;
import com.google.endpoints.examples.bookstore.ListShelvesResponse;
import com.google.endpoints.examples.bookstore.Shelf;
import com.google.protobuf.Empty;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BookstoreClient {

    private static final Deadline TIMEOUT = Deadline.after(3, TimeUnit.SECONDS);

    public static void main(String[] args) throws IOException {
        String host = "34.143.204.26";
        int apiKeyPort = 9000;
        int authTokenPort = 9001;
        String serviceAccountFilePath = "/Users/datle/experimental-229809-32e56a607af1.json"; // dat-service-account
        String serviceName = "bookstore2.endpoints.experimental-229809.cloud.goog";

        ServiceAccountAuth serviceAccountAuth = ServiceAccountAuth.fromFile(serviceAccountFilePath, serviceName, 120);
        AuthOptions authOptions = new AuthOptions(/*apiKey=*/null, serviceAccountAuth);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, authTokenPort)
                .usePlaintext()
                .intercept(new AuthInterceptor(authOptions))
                .build();
        BookstoreBlockingStub stub = BookstoreGrpc.newBlockingStub(channel);

        try {
            ListShelvesResponse listShelvesResponse = stub.withDeadline(TIMEOUT)
                    .listShelves(Empty.getDefaultInstance());
            System.out.printf("listShelves=%s%n", listShelvesResponse);

            Shelf firstShelf = stub.withDeadline(TIMEOUT)
                    .getShelf(GetShelfRequest.newBuilder().setShelf(listShelvesResponse.getShelves(0).getId()).build());
            System.out.printf("getShelf=%s%n", firstShelf);

            stub.withDeadline(TIMEOUT).deleteShelf(DeleteShelfRequest.newBuilder().setShelf(-1).build());
            System.out.printf("deleteShelf%n");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getMessage());
            stub.withDeadline(TIMEOUT).listShelves(Empty.getDefaultInstance());
        } finally {
            channel.shutdownNow();
        }
    }
}
