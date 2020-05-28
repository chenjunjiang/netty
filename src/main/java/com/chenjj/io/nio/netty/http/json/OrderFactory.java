package com.chenjj.io.nio.netty.http.json;

import java.util.ArrayList;
import java.util.List;

public class OrderFactory {
    public static Order create(long orderId) {
        Order order = new Order();
        Customer customer = new Customer();
        customer.setId(1);
        customer.setFirstName("王");
        customer.setLastName("麻子");
        List<String> midNames = new ArrayList<String>();
        midNames.add("张三");
        midNames.add("李四");
        customer.setMiddleNames(midNames);
        order.setCustomer(customer);
        Address address = new Address();
        address.setCity("成都");
        address.setCountry("中国");
        address.setState("双流");
        address.setPostCode("654321");
        order.setBillTo(address);
        order.setShipTo(address);
        return order;
    }
}
