package transport.hrpc;

import java.util.Map;
import java.util.HashMap;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;

public class ServiceRegistry {

    private class InnerHelp {
        private Map<Integer, MethodDescriptor> methodIndexMap = new HashMap<Integer, MethodDescriptor>();
        private Service service = null;
        InnerHelp(Service service) {
            this.service = service;
            for (MethodDescriptor methodDescriptor: service.getDescriptorForType().getMethods()) {
                methodIndexMap.put(methodDescriptor.getIndex(), methodDescriptor);
            }
        }

        public Service getService() {
            return this.service;
        }

        public MethodDescriptor getMethodByIndex(int index) {
            return methodIndexMap.get(index);
        }
    }

   private Map<Integer, InnerHelp> serviceIndexMap = new HashMap<Integer, InnerHelp>();


   public void registerService(Service service) {
       ServiceDescriptor serviceDescriptor = service.getDescriptorForType();
       serviceIndexMap.put(serviceDescriptor.getIndex(), new InnerHelp(service));
   }

   public void unRegisterService(Service service) {
       ServiceDescriptor serviceDescriptor = service.getDescriptorForType();
       serviceIndexMap.remove(serviceDescriptor.getIndex());
   }

   public Service getServiceByIndex(Integer index) {
       return serviceIndexMap.get(index).getService();
   }
   public MethodDescriptor getMethodByIndex(int serviceIndex, int methodIndex) {
       InnerHelp innerHelp = serviceIndexMap.get(serviceIndex);
       return innerHelp.getMethodByIndex(methodIndex);
   }

   public int getServiceCount() {
       return serviceIndexMap.size();
   }
}
