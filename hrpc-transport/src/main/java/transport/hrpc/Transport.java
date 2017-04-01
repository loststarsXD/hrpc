package transport.hrpc;


import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.RpcCallback;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.HashMap;


@Sharable
public class Transport extends ChannelInboundHandlerAdapter  {

    private class ResponseIdentity {
        private Promise<Message> future = null;
        private Message message = null;
        ResponseIdentity(Message message, Promise<Message> future) {
            this.future = future;
            this.message = message;
        }
    }

    private ServiceRegistry serviceRegistry = null;
    private ChannelHandlerContext ctx = null;
    private Map<Integer, ResponseIdentity> futureIdentityMap = new HashMap<Integer, ResponseIdentity>();
    private int identity = 1;
    private RpcChannel rpcChannel = null;
    private RpcControl rpcControl = null;


    public Transport(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public RpcChannel getRpcChannel() {
        return rpcChannel;
    }

    public RpcControl getRpcControl() {
        return rpcControl;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        this.rpcChannel = new RpcChannel(this);
        this.rpcControl = new RpcControl();
        this.rpcControl.setRpcChannel(this.rpcChannel);
        this.rpcControl.setChannel(ctx.channel());
    }

    @Override
    public void channelRead (final ChannelHandlerContext ctx, Object msg) throws Exception {
        Rpc.Packet packet = (Rpc.Packet) msg;
        final int identity = packet.getIdentity();
        ByteString serizies = packet.getSerialized();
        if (packet.getType() == Rpc.PacketType.REQUEST) {
            Rpc.Request request = Rpc.Request.parseFrom(serizies);
            processRequest(identity, request);
        } else if (packet.getType() == Rpc.PacketType.RESPONSE){
            Rpc.Response response = Rpc.Response.parseFrom(serizies);
            processResponse(identity, response);
        } else if (packet.getType() == Rpc.PacketType.ERROR) {
            Rpc.Error error = Rpc.Error.parseFrom(serizies);
            processError(identity, error);
        } else if (packet.getType() == Rpc.PacketType.WARNING) {
            Rpc.Warning warning = Rpc.Warning.parseFrom(serizies);
            processWarning(identity, warning);
        } else if (packet.getType() == Rpc.PacketType.HEARTBEAT) {
            processHeartbeat(identity);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                  Rpc.Packet packet = Rpc.Packet.newBuilder()
                            .setIdentity(0)
                            .setVersion(1)
                            .setType(Rpc.PacketType.HEARTBEAT)
                            .setSerialized(Rpc.Heartbeat.newBuilder().build().toByteString()).build();
                  ctx.writeAndFlush(packet);
            }
        }
    }

    private void processRequest(final int identity, Rpc.Request request) throws Exception {
            ByteString message = request.getRequest();
            Service service = serviceRegistry.getServiceByIndex(request.getService());
            MethodDescriptor methodDescriptor = serviceRegistry.getMethodByIndex(request.getService(), request.getMethod());
            Message body = service.getRequestPrototype(methodDescriptor).newBuilderForType().mergeFrom(message).build();

            service.callMethod(methodDescriptor, rpcControl, body, new RpcCallback<Message>() {
                public void run(Message message) {
                    Rpc.Response response = Rpc.Response.newBuilder().setResponse(message.toByteString()).build();
                    Rpc.Packet packet = Rpc.Packet.newBuilder()
                            .setIdentity(identity)
                            .setVersion(1)
                            .setType(Rpc.PacketType.RESPONSE)
                            .setSerialized(response.toByteString()).build();
                    ctx.writeAndFlush(packet);
                }
            });
    }

    private void processResponse(int identity, Rpc.Response response) throws Exception {
        ResponseIdentity responseIdentity = futureIdentityMap.remove(identity);
        Message message = responseIdentity.message.getParserForType().parseFrom(response.getResponse());
        responseIdentity.future.setSuccess(message);
    }

    private void processError(int identity, Rpc.Error error) throws Exception {
    }

    private void processWarning(int identity, Rpc.Warning warning) throws Exception {
    }

    private void processHeartbeat(int identity) {
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    public Future<Message> request(int serviceId, int methodId, Message message, Message response) {
        Rpc.Request request = Rpc.Request.newBuilder()
                .setService(serviceId)
                .setMethod(methodId)
                .setRequest(message.toByteString()).build();

        int newIdentity = 0;
        if (response != Null.Void.getDefaultInstance()) {
            newIdentity = newIdentity();
        }
        Rpc.Packet packet = Rpc.Packet.newBuilder()
                .setIdentity(newIdentity)
                .setType(Rpc.PacketType.REQUEST)
                .setVersion(1)
                .setSerialized(request.toByteString()).build();
        ctx.writeAndFlush(packet);

        Promise<Message> future = null;
        if (response != Null.Void.getDefaultInstance()) {
            future = ctx.channel().eventLoop().newPromise();
            futureIdentityMap.put(newIdentity, new ResponseIdentity(response, future));
        }
        return future;
    }

    private int newIdentity() {
        int newIdentity = identity;
        identity += 1;
        if (identity >= 1<<32){
            identity = 0;
        }
        return newIdentity;
    }
}
