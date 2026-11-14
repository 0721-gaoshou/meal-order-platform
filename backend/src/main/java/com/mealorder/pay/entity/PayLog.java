package com.mealorder.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付流水，对应表 pay_log
 * 幂等双保险（§4.11）：uk_out_trade_no 防重复发起，uk(out_trade_no, trade_no) 兜底渠道重复回调
 */
@Data
@TableName("pay_log")
public class PayLog {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String outTradeNo;

    private String tradeNo;

    /** MOCK / WECHAT */
    private String payChannel;

    /** 支付金额·分 */
    private Long amount;

    /** PAYING / PAID / CLOSED / FAILED */
    private String status;

    private String callbackBody;

    private LocalDateTime payTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}