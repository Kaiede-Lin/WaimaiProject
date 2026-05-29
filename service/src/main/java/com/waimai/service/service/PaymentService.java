package com.waimai.service.service;

public interface PaymentService {

    String mockPay(Long orderId);

    boolean isPayProcessed(String payNo);
}
