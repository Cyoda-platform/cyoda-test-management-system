package org.cyoda.cloud.api.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: cyoda-cloud-api.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CloudEventsServiceGrpc {

  private CloudEventsServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "org.cyoda.cloud.api.grpc.CloudEventsService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getStartStreamingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "startStreaming",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getStartStreamingMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getStartStreamingMethod;
    if ((getStartStreamingMethod = CloudEventsServiceGrpc.getStartStreamingMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getStartStreamingMethod = CloudEventsServiceGrpc.getStartStreamingMethod) == null) {
          CloudEventsServiceGrpc.getStartStreamingMethod = getStartStreamingMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "startStreaming"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("startStreaming"))
              .build();
        }
      }
    }
    return getStartStreamingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityModelManageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "entityModelManage",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityModelManageMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getEntityModelManageMethod;
    if ((getEntityModelManageMethod = CloudEventsServiceGrpc.getEntityModelManageMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getEntityModelManageMethod = CloudEventsServiceGrpc.getEntityModelManageMethod) == null) {
          CloudEventsServiceGrpc.getEntityModelManageMethod = getEntityModelManageMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "entityModelManage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("entityModelManage"))
              .build();
        }
      }
    }
    return getEntityModelManageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityManageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "entityManage",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityManageMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getEntityManageMethod;
    if ((getEntityManageMethod = CloudEventsServiceGrpc.getEntityManageMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getEntityManageMethod = CloudEventsServiceGrpc.getEntityManageMethod) == null) {
          CloudEventsServiceGrpc.getEntityManageMethod = getEntityManageMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "entityManage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("entityManage"))
              .build();
        }
      }
    }
    return getEntityManageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityManageCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "entityManageCollection",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntityManageCollectionMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getEntityManageCollectionMethod;
    if ((getEntityManageCollectionMethod = CloudEventsServiceGrpc.getEntityManageCollectionMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getEntityManageCollectionMethod = CloudEventsServiceGrpc.getEntityManageCollectionMethod) == null) {
          CloudEventsServiceGrpc.getEntityManageCollectionMethod = getEntityManageCollectionMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "entityManageCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("entityManageCollection"))
              .build();
        }
      }
    }
    return getEntityManageCollectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntitySearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "entitySearch",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntitySearchMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getEntitySearchMethod;
    if ((getEntitySearchMethod = CloudEventsServiceGrpc.getEntitySearchMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getEntitySearchMethod = CloudEventsServiceGrpc.getEntitySearchMethod) == null) {
          CloudEventsServiceGrpc.getEntitySearchMethod = getEntitySearchMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "entitySearch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("entitySearch"))
              .build();
        }
      }
    }
    return getEntitySearchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntitySearchCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "entitySearchCollection",
      requestType = io.cloudevents.v1.proto.CloudEvent.class,
      responseType = io.cloudevents.v1.proto.CloudEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent,
      io.cloudevents.v1.proto.CloudEvent> getEntitySearchCollectionMethod() {
    io.grpc.MethodDescriptor<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent> getEntitySearchCollectionMethod;
    if ((getEntitySearchCollectionMethod = CloudEventsServiceGrpc.getEntitySearchCollectionMethod) == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        if ((getEntitySearchCollectionMethod = CloudEventsServiceGrpc.getEntitySearchCollectionMethod) == null) {
          CloudEventsServiceGrpc.getEntitySearchCollectionMethod = getEntitySearchCollectionMethod =
              io.grpc.MethodDescriptor.<io.cloudevents.v1.proto.CloudEvent, io.cloudevents.v1.proto.CloudEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "entitySearchCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.cloudevents.v1.proto.CloudEvent.getDefaultInstance()))
              .setSchemaDescriptor(new CloudEventsServiceMethodDescriptorSupplier("entitySearchCollection"))
              .build();
        }
      }
    }
    return getEntitySearchCollectionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CloudEventsServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceStub>() {
        @java.lang.Override
        public CloudEventsServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CloudEventsServiceStub(channel, callOptions);
        }
      };
    return CloudEventsServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CloudEventsServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceBlockingStub>() {
        @java.lang.Override
        public CloudEventsServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CloudEventsServiceBlockingStub(channel, callOptions);
        }
      };
    return CloudEventsServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CloudEventsServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CloudEventsServiceFutureStub>() {
        @java.lang.Override
        public CloudEventsServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CloudEventsServiceFutureStub(channel, callOptions);
        }
      };
    return CloudEventsServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> startStreaming(
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getStartStreamingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityModelImportRequest, Output: EntityModelImportResponse
     * Input: EntityModelExportRequest, Output: EntityModelExportResponse
     * Input: EntityModelTransitionRequest, Output: EntityModelTransitionResponse
     * Input: EntityModelDeleteRequest, Output: EntityModelDeleteResponse
     * Input: EntityModelGetAllRequest, Output: EntityModelGetAllResponse
     * </pre>
     */
    default void entityModelManage(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEntityModelManageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityCreateRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateRequest, Output: EntityTransactionResponse
     * Input: EntityDeleteRequest, Output: EntityDeleteResponse
     * Input: EntityTransitionRequest, Output: EntityTransitionResponse
     * </pre>
     */
    default void entityManage(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEntityManageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityDeleteAllRequest, Output: EntityDeleteAllResponse
     * Input: EntityCreateCollectionRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateCollectionRequest, Output: EntityTransactionResponse
     * </pre>
     */
    default void entityManageCollection(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEntityManageCollectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntitySnapshotSearchRequest, Output: EntitySnapshotSearchResponse
     * Input: SnapshotGetStatusRequest, Output: EntitySearchResponse
     * Input: SnapshotCancelRequest, Output: EntitySearchResponse
     * Input: EntityGetRequest, Output: EntityResponse
     * </pre>
     */
    default void entitySearch(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEntitySearchMethod(), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityGetAllRequest, Output: EntityResponse
     * Input: SnapshotGetRequest, Output: EntityResponse
     * Input: EntitySearchRequest, Output: EntityResponse
     * Input: EntityStatsGetRequest, Output: EntityStatsResponse
     * Input: EntityStatsByStateGetRequest, Output: EntityStatsByStateResponse
     * Input: EntityChangesMetadataGetRequest, Output: EntityChangesMetadataResponse
     * </pre>
     */
    default void entitySearchCollection(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEntitySearchCollectionMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CloudEventsService.
   */
  public static abstract class CloudEventsServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CloudEventsServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CloudEventsService.
   */
  public static final class CloudEventsServiceStub
      extends io.grpc.stub.AbstractAsyncStub<CloudEventsServiceStub> {
    private CloudEventsServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudEventsServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudEventsServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> startStreaming(
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getStartStreamingMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityModelImportRequest, Output: EntityModelImportResponse
     * Input: EntityModelExportRequest, Output: EntityModelExportResponse
     * Input: EntityModelTransitionRequest, Output: EntityModelTransitionResponse
     * Input: EntityModelDeleteRequest, Output: EntityModelDeleteResponse
     * Input: EntityModelGetAllRequest, Output: EntityModelGetAllResponse
     * </pre>
     */
    public void entityModelManage(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEntityModelManageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityCreateRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateRequest, Output: EntityTransactionResponse
     * Input: EntityDeleteRequest, Output: EntityDeleteResponse
     * Input: EntityTransitionRequest, Output: EntityTransitionResponse
     * </pre>
     */
    public void entityManage(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEntityManageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityDeleteAllRequest, Output: EntityDeleteAllResponse
     * Input: EntityCreateCollectionRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateCollectionRequest, Output: EntityTransactionResponse
     * </pre>
     */
    public void entityManageCollection(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getEntityManageCollectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntitySnapshotSearchRequest, Output: EntitySnapshotSearchResponse
     * Input: SnapshotGetStatusRequest, Output: EntitySearchResponse
     * Input: SnapshotCancelRequest, Output: EntitySearchResponse
     * Input: EntityGetRequest, Output: EntityResponse
     * </pre>
     */
    public void entitySearch(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEntitySearchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityGetAllRequest, Output: EntityResponse
     * Input: SnapshotGetRequest, Output: EntityResponse
     * Input: EntitySearchRequest, Output: EntityResponse
     * Input: EntityStatsGetRequest, Output: EntityStatsResponse
     * Input: EntityStatsByStateGetRequest, Output: EntityStatsByStateResponse
     * Input: EntityChangesMetadataGetRequest, Output: EntityChangesMetadataResponse
     * </pre>
     */
    public void entitySearchCollection(io.cloudevents.v1.proto.CloudEvent request,
        io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getEntitySearchCollectionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CloudEventsService.
   */
  public static final class CloudEventsServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CloudEventsServiceBlockingStub> {
    private CloudEventsServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudEventsServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudEventsServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityModelImportRequest, Output: EntityModelImportResponse
     * Input: EntityModelExportRequest, Output: EntityModelExportResponse
     * Input: EntityModelTransitionRequest, Output: EntityModelTransitionResponse
     * Input: EntityModelDeleteRequest, Output: EntityModelDeleteResponse
     * Input: EntityModelGetAllRequest, Output: EntityModelGetAllResponse
     * </pre>
     */
    public io.cloudevents.v1.proto.CloudEvent entityModelManage(io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEntityModelManageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityCreateRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateRequest, Output: EntityTransactionResponse
     * Input: EntityDeleteRequest, Output: EntityDeleteResponse
     * Input: EntityTransitionRequest, Output: EntityTransitionResponse
     * </pre>
     */
    public io.cloudevents.v1.proto.CloudEvent entityManage(io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEntityManageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityDeleteAllRequest, Output: EntityDeleteAllResponse
     * Input: EntityCreateCollectionRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateCollectionRequest, Output: EntityTransactionResponse
     * </pre>
     */
    public java.util.Iterator<io.cloudevents.v1.proto.CloudEvent> entityManageCollection(
        io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getEntityManageCollectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntitySnapshotSearchRequest, Output: EntitySnapshotSearchResponse
     * Input: SnapshotGetStatusRequest, Output: EntitySearchResponse
     * Input: SnapshotCancelRequest, Output: EntitySearchResponse
     * Input: EntityGetRequest, Output: EntityResponse
     * </pre>
     */
    public io.cloudevents.v1.proto.CloudEvent entitySearch(io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEntitySearchMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityGetAllRequest, Output: EntityResponse
     * Input: SnapshotGetRequest, Output: EntityResponse
     * Input: EntitySearchRequest, Output: EntityResponse
     * Input: EntityStatsGetRequest, Output: EntityStatsResponse
     * Input: EntityStatsByStateGetRequest, Output: EntityStatsByStateResponse
     * Input: EntityChangesMetadataGetRequest, Output: EntityChangesMetadataResponse
     * </pre>
     */
    public java.util.Iterator<io.cloudevents.v1.proto.CloudEvent> entitySearchCollection(
        io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getEntitySearchCollectionMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CloudEventsService.
   */
  public static final class CloudEventsServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<CloudEventsServiceFutureStub> {
    private CloudEventsServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudEventsServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudEventsServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityModelImportRequest, Output: EntityModelImportResponse
     * Input: EntityModelExportRequest, Output: EntityModelExportResponse
     * Input: EntityModelTransitionRequest, Output: EntityModelTransitionResponse
     * Input: EntityModelDeleteRequest, Output: EntityModelDeleteResponse
     * Input: EntityModelGetAllRequest, Output: EntityModelGetAllResponse
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.cloudevents.v1.proto.CloudEvent> entityModelManage(
        io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEntityModelManageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntityCreateRequest, Output: EntityTransactionResponse
     * Input: EntityUpdateRequest, Output: EntityTransactionResponse
     * Input: EntityDeleteRequest, Output: EntityDeleteResponse
     * Input: EntityTransitionRequest, Output: EntityTransitionResponse
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.cloudevents.v1.proto.CloudEvent> entityManage(
        io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEntityManageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Following event pairs supported:
     * Input: EntitySnapshotSearchRequest, Output: EntitySnapshotSearchResponse
     * Input: SnapshotGetStatusRequest, Output: EntitySearchResponse
     * Input: SnapshotCancelRequest, Output: EntitySearchResponse
     * Input: EntityGetRequest, Output: EntityResponse
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.cloudevents.v1.proto.CloudEvent> entitySearch(
        io.cloudevents.v1.proto.CloudEvent request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEntitySearchMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ENTITY_MODEL_MANAGE = 0;
  private static final int METHODID_ENTITY_MANAGE = 1;
  private static final int METHODID_ENTITY_MANAGE_COLLECTION = 2;
  private static final int METHODID_ENTITY_SEARCH = 3;
  private static final int METHODID_ENTITY_SEARCH_COLLECTION = 4;
  private static final int METHODID_START_STREAMING = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ENTITY_MODEL_MANAGE:
          serviceImpl.entityModelManage((io.cloudevents.v1.proto.CloudEvent) request,
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
          break;
        case METHODID_ENTITY_MANAGE:
          serviceImpl.entityManage((io.cloudevents.v1.proto.CloudEvent) request,
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
          break;
        case METHODID_ENTITY_MANAGE_COLLECTION:
          serviceImpl.entityManageCollection((io.cloudevents.v1.proto.CloudEvent) request,
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
          break;
        case METHODID_ENTITY_SEARCH:
          serviceImpl.entitySearch((io.cloudevents.v1.proto.CloudEvent) request,
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
          break;
        case METHODID_ENTITY_SEARCH_COLLECTION:
          serviceImpl.entitySearchCollection((io.cloudevents.v1.proto.CloudEvent) request,
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_START_STREAMING:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.startStreaming(
              (io.grpc.stub.StreamObserver<io.cloudevents.v1.proto.CloudEvent>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getStartStreamingMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_START_STREAMING)))
        .addMethod(
          getEntityModelManageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_ENTITY_MODEL_MANAGE)))
        .addMethod(
          getEntityManageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_ENTITY_MANAGE)))
        .addMethod(
          getEntityManageCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_ENTITY_MANAGE_COLLECTION)))
        .addMethod(
          getEntitySearchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_ENTITY_SEARCH)))
        .addMethod(
          getEntitySearchCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.cloudevents.v1.proto.CloudEvent,
              io.cloudevents.v1.proto.CloudEvent>(
                service, METHODID_ENTITY_SEARCH_COLLECTION)))
        .build();
  }

  private static abstract class CloudEventsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CloudEventsServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.cyoda.cloud.api.grpc.CyodaCloudApi.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CloudEventsService");
    }
  }

  private static final class CloudEventsServiceFileDescriptorSupplier
      extends CloudEventsServiceBaseDescriptorSupplier {
    CloudEventsServiceFileDescriptorSupplier() {}
  }

  private static final class CloudEventsServiceMethodDescriptorSupplier
      extends CloudEventsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CloudEventsServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CloudEventsServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CloudEventsServiceFileDescriptorSupplier())
              .addMethod(getStartStreamingMethod())
              .addMethod(getEntityModelManageMethod())
              .addMethod(getEntityManageMethod())
              .addMethod(getEntityManageCollectionMethod())
              .addMethod(getEntitySearchMethod())
              .addMethod(getEntitySearchCollectionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
