package com.mealorder.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退款单，对应表 refund_order
 * refund_no 唯一索引 = 退款回调幂等锚点（§4.11）；状态 REFUNDING→REFUNDED/FAILED CAS
 */
@Data
@TableName("refund_order")
public class RefundOrder {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String refundNo;

    private Long orderId;

    /** 退款金额·分（M4 整单全额退，部分退款按 item 分摊退留后续） */
    private Long refundAmount;

    /** REFUNDING / REFUNDED / FAILED */
    private String status;

    private String reason;

    private LocalDateTime refundTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}