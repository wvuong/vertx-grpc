package io.vertx.grpc.it;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServiceBridge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author wvuong@chariotsolutions.com on 5/24/22.
 */
@RunWith(VertxUnitRunner.class)
public class Stub2Test {

  private Vertx vertx;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext should) throws Exception {
    vertx.close(should.asyncAssertSuccess());
  }

  @Test
  public void router(TestContext should) {
    final Router router = Router.router(vertx);

    // copied and pasted from https://vertx.io/blog/whats-new-in-vert-x-4-3/
    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
      @Override
      public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        responseObserver.onCompleted();
      }
    };

    GrpcServer grpcServer = GrpcServer.server(vertx);
    GrpcServiceBridge serverStub = GrpcServiceBridge.bridge(service);
    serverStub.bind(grpcServer);

    router.route().consumes("application/grpc").handler(rc -> {
      grpcServer.handle(rc.request());
    });

    final Future<HttpServer> future = vertx.createHttpServer()
      .requestHandler(router) // using the router does not!
      //.requestHandler(grpcServer) // using grpcServer as the handler works!
      .listen(8080, "localhost");

    Async test = should.async();
    future.onComplete(should.asyncAssertSuccess(server -> {
      GrpcClient.client(vertx)
        .request(SocketAddress.inetSocketAddress(8080, "localhost"), GreeterGrpc.getSayHelloMethod())
        .onComplete(should.asyncAssertSuccess(callRequest -> {
          callRequest.response().onComplete(should.asyncAssertSuccess(response -> {
            response.handler(reply -> {
              System.out.println("Received message: " + reply.getMessage());
            });

            response.endHandler(v -> {
              test.complete();
            });
          }));

          callRequest.end(HelloRequest.newBuilder().setName("Daniel Tiger").build());
        }));
    }));

    test.await(1000L);
  }
}
