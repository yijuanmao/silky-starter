package com.silky.starter.redis.test.redission.result;

import lombok.Data;

/**
 * PaymentResult
 *
 * @author zy
 * @date 2025-10-23 15:05
 **/
@Data
public class PaymentResult {


    private Long paymentId;

    public PaymentResult() {

    }

    public PaymentResult(Long paymentId) {
        this.paymentId = paymentId;
    }

}
