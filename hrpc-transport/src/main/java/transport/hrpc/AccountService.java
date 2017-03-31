package transport.hrpc;

import com.google.protobuf.RpcCallback;


/**
 * Created by fucong on 3/23/17.
 */
public class AccountService extends Service.AccountService1 {

      public  void enterHundred1(
          com.google.protobuf.RpcController controller,
          Service.EnterHundredRequest request,
          com.google.protobuf.RpcCallback<Service.EnterHundredResponse> done) {
            System.out.format("user_id: " + request.getUserId());
            RpcControl rpcControl = (RpcControl) controller;
            Service.UpdateCurrencyRequest  request1 = Service.UpdateCurrencyRequest.newBuilder().setCurrency(1).setDelta(1).setUserId(1000).build();
            Service.PlayerBroadcastService.newStub(((RpcControl) controller).getRpcChannel()).updateCurrency(controller, request1, new RpcCallback<Service.UpdateCurrencyRequest>() {
                  public void run(Service.UpdateCurrencyRequest updateCurrencyRequest) {
                        System.out.format("get response: " + updateCurrencyRequest.toString());
                  }
            });

            done.run(Service.EnterHundredResponse.newBuilder().setAlreadyInHundred(true).build());
      }
      public  void leaveHundred1(
          com.google.protobuf.RpcController controller,
          Service.EnterHundredRequest request,
          com.google.protobuf.RpcCallback<Service.EnterHundredResponse> done) {
      }

}



