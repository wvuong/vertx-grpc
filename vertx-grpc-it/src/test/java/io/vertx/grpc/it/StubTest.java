package io.vertx.grpc.it;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServiceBridge;

/**
 * @author wvuong@chariotsolutions.com on 5/23/22.
 */
public class StubTest {

  public static void main(String... args) throws Exception {
    final Vertx vertx = Vertx.vertx();
    final Router router = Router.router(vertx);

    // copied and pasted from https://vertx.io/blog/whats-new-in-vert-x-4-3/
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        System.out.println("Received request with name "  + request.getName());
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };

    GrpcServer grpcServer = GrpcServer.server(vertx);
    GrpcServiceBridge serverStub = GrpcServiceBridge.bridge(service);
    serverStub.bind(grpcServer);

    // this code from the example doesn't compile
    // router.consumes("application/grpc").handler(grpcServer);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080, "localhost");
  }
}
