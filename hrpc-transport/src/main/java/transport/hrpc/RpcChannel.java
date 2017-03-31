package transport.hrpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Created by fucong on 17/3/26.
 */
public class RpcChannel implements com.google.protobuf.RpcChannel {
    private Transport transport = null;

    RpcChannel(Transport transport) {
        this.transport = transport;
    }

    public void callMethod(Descriptors.MethodDescriptor methodDescriptor, RpcController rpcController, Message message, Message message1, final RpcCallback<Message> rpcCallback) {
        Descriptors.ServiceDescriptor serviceDescriptor =  methodDescriptor.getService();
        int  serviceId  = serviceDescriptor.getIndex();
        int methodId = methodDescriptor.getIndex();

        Future<Message> future = transport.request(serviceId, methodId, message, message1);
        if (future != null) {
            future.addListener(new GenericFutureListener<Future<Message>>() {
                public void operationComplete(Future<Message> future) throws Exception {
                    rpcCallback.run(future.get());
                }
            });
        }
    }
}
