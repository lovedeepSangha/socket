package com.Sidrit.socket;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.listener.DataListener;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SocketApplication {


    public static void main(String[] args) {
        SpringApplication.run(SocketApplication.class, args);
    }

    @Bean
    public SocketIOServer createSocketIOConnection() {

        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);

        return new SocketIOServer(config);
    }


}

@Component
class ServerCommandLineRunner implements CommandLineRunner {

    private final SocketIOServer server;

    @Autowired
    public ServerCommandLineRunner(SocketIOServer server) {
        this.server = server;
    }

    @Override
    public void run(String... args) throws Exception {
        server.start();
    }
}

@Getter
@Setter
class Customer {
    private String customerId;
    private String advisorId;
    private String roomId;
    private Boolean isAdvisorConnected;
    private Boolean isCustomer;
    private Boolean isGetCustomerData;
}


class Room {
    private int roomId;

}

@Component
class SocketIOEvents {


    private List<Customer> customerList = new ArrayList<Customer>();
    @Autowired
    private SocketIOServer socketIOServer;


    @OnEvent(value = "join")
    public void userConnected(SocketIOClient client, AckRequest request, Customer data) {
        // here is the adding the user in connection
        client.joinRoom(data.getRoomId());
        if (data.getIsCustomer()) {
            customerList.add(data);
            socketIOServer.getBroadcastOperations().sendEvent("newCustomerAdded", data);
        }

        if (!data.getIsCustomer() && data.getIsGetCustomerData()) {
            var checkCustomerPresent = customerList.stream().filter(customer -> customer.getCustomerId().equals(data.getAdvisorId())).findAny();
            if (checkCustomerPresent.isPresent()) {
                checkCustomerPresent.get().setIsAdvisorConnected(true);
                checkCustomerPresent.get().setAdvisorId(data.getAdvisorId());
                socketIOServer.getBroadcastOperations().sendEvent("sendCustomerList", customerList);
            }
            // here is the room
            
            socketIOServer.getRoomOperations(data.getRoomId()).sendEvent("newAdvisorConnected", data);

        }

    }


    @OnEvent(value = "disconnect")
    public void diconnectUser(SocketIOClient client, AckRequest request, Customer data){
        customerList.removeIf(customer -> customer.getCustomerId().equals(data.getCustomerId()));
        // we can write any logic to disconnect
    }


}