package com.mealorder.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.order.entity.Order;
import com.mealorder.order.mapper.OrderMapper;
import com.mealorder.pay.dto.PayDtos.MockCallbackRequest;
import com.mealorder.pay.dto.PayDtos.PayCreateVO;
import com.mealorder.pay.dto.PayDtos.PayResultVO;
import com.mealorder.pay.entity.PayLog;
import com.mealorder.pay.mapper.PayLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Mock 支付网关（M4）：发起支付落 pay_log(PAYING)，模拟回调驱动订单状态机
 * 回调幂等：pay_log 状态判重 + 订单 CAS（PENDING_PAY→PAID），重复回调返回同一结果
 */
@Service
@RequiredArgsConstructor
public class PayService {

    private final OrderMapper orderMapper;
    private final PayLogMapper payLogMapper;

    @Transactional
    public PayCreateVO create(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!"PENDING_PAY".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单当前状态不可支付");
        }
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单已超时，请取消后重新下单");
        }
        // 同一订单复用未支付的支付单，避免重复发起产生多笔流水
        PayLog payLog = payLogMapper.selectOne(new LambdaQueryWrapper<PayLog>()
                .eq(PayLog::getOrderId, orderId).eq(PayLog::getStatus, "PAYING")
                .orderByDesc(PayLog::getCreatedAt).last("LIMIT 1"));
        if (payLog == null) {
            LocalDateTime now = LocalDateTime.now();
            payLog = new PayLog();
            payLog.setOrderId(orderId);
            payLog.setOutTradeNo("PT" + IdWorker.getIdStr());
            payLog.setPayChannel("MOCK");
            payLog.setAmount(order.getPayAmount());
            payLog.setStatus("PAYING");
            payLog.setCreatedAt(now);
            payLog.setUpdatedAt(now);
            payLogMapper.insert(payLog);
        }
        return new PayCreateVO(orderId, order.getOrderNo(), payLog.getOutTradeNo(),
                payLog.getAmount(), payLog.getPayChannel());
    }

    @Transactional(rollbackFor = Exception.class)
    public PayResultVO mockCallback(MockCallbackRequest request) {
        PayLog payLog = payLogMapper.selectOne(new LambdaQueryWrapper<PayLog>()
                .eq(PayLog::getOutTradeNo, request.outTradeNo()));
        if (payLog == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付单不存在");
        }
        // 已支付：重复回调直接返回当前结果（幂等）
        if ("PAID".equals(payLog.getStatus())) {
            Order order = orderMapper.selectById(payLog.getOrderId());
            return new PayResultVO(payLog.getOrderId(), order == null ? "PAID" : order.getStatus(),
                    payLog.getOutTradeNo(), payLog.getTradeNo(), payLog.getAmount());
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.markPaid(payLog.getOrderId(), now);
        Order order = orderMapper.selectById(payLog.getOrderId());
        if (rows == 0) {
            // 并发回调：另一笔已经改成 PAID → 幂等返回；订单被取消等 → 拒绝回调
            if (order != null && "PAID".equals(order.getStatus())) {
                return new PayResultVO(order.getId(), order.getStatus(), payLog.getOutTradeNo(),
                        request.tradeNo(), payLog.getAmount());
            }
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态不是待支付，无法完成支付（可能已取消）");
        }

        payLog.setStatus("PAID");
        payLog.setTradeNo(request.tradeNo());
        payLog.setPayTime(now);
        payLog.setCallbackBody("{\"outTradeNo\":\"" + request.outTradeNo() + "\",\"tradeNo\":\"" + request.tradeNo() + "\"}");
        payLog.setUpdatedAt(now);
        try {
            payLogMapper.updateById(payLog);
        } catch (DuplicateKeyException e) {
            // uk(out_trade_no, trade_no)：同一渠道单号被不同支付单复用
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "渠道单号已被其他支付单使用");
        }
        return new PayResultVO(order.getId(), order.getStatus(), payLog.getOutTradeNo(),
                payLog.getTradeNo(), payLog.getAmount());
    }
}