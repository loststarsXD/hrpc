package transport.hrpc.example.echo;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import transport.hrpc.Rpc;
import transport.hrpc.ServiceRegistry;

class Transport extends transport.hrpc.Transport {

    public Transport(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Echo.EchoService.newStub(getRpcChannel()).echo(getRpcControl(), Echo.Request.newBuilder().setMsg("hello").build(), new RpcCallback<Echo.Response>() {
            public void run(Echo.Response response) {
                System.out.format(response.getMsg());
            }
        });
    }
}

public final class EchoClient {
        static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.git
        final SslContext sslCtx;
        if (SSL) {
            sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        final ServiceRegistry serviceRegistry = new ServiceRegistry();
        Service service = new EchoService();
        serviceRegistry.registerService(service);


        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(new LengthFieldBasedFrameDecoder(10000, 0, 4, 0, 4));
                     p.addLast(new ProtobufDecoder(Rpc.Packet.getDefaultInstance()));
                     p.addLast("frameEncoder", new LengthFieldPrepender(4));
                     p.addLast("protobufEncoder", new ProtobufEncoder());
                     p.addLast(new Transport(serviceRegistry));
                 }
             });

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

}





