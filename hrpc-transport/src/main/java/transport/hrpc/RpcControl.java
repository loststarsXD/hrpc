package transport.hrpc;

import com.google.protobuf.RpcCallback;
import io.netty.channel.Channel;

/**
 * Created by wangfucong on 17/3/27.
 */
public class RpcControl implements com.google.protobuf.RpcController {
    private RpcChannel rpcChannel= null;
    private Channel channel = null;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public RpcChannel getRpcChannel() {
        return rpcChannel;
    }

    public void setRpcChannel(RpcChannel rpcChannel) {
        this.rpcChannel = rpcChannel;
    }

    public String errorText() {
        return null;
    }

    public boolean failed() {
        return false;
    }

    public boolean isCanceled() {
        return false;
    }

    public void notifyOnCancel(RpcCallback<Object> rpcCallback) {

    }

    public void reset() {

    }

    public void setFailed(String s) {

    }

    public void startCancel() {

    }
}
