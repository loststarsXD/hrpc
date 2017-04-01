package transport.hrpc.example.echo;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * Created by fucong on 4/1/17.
 */
public class EchoService extends Echo.EchoService {
    @Override
    public void echo(RpcController controller, Echo.Request request, RpcCallback<Echo.Response> done) {
        Echo.Response response = Echo.Response.newBuilder().setMsg(request.getMsg()).build();
        done.run(response);
    }
}
