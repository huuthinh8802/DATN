package com.watch.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.watch.PayConfig;
import com.watch.models.Order;
import com.watch.models.OrderDetail;
import com.watch.service.OrderDetailService;
import com.watch.service.OrderService;

import jakarta.servlet.http.HttpServletResponse;
@RestController
public class PayController {
	@Autowired
    private OrderService orderSevice;
    @Autowired
    private OrderDetailService orderDetailSevice;

    @GetMapping("/pay/{id}")
    public String getPay(HttpServletResponse response, @PathVariable("id") Integer id) throws UnsupportedEncodingException {
        Order order = this.orderSevice.findById(id);
        List<OrderDetail> orderDetails = this.orderDetailSevice.getByOrder(order);
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amount = calculateTotalAmount(orderDetails).longValue()*100;
        String bankCode = "NCB";

        String vnp_TxnRef = PayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";

        String vnp_TmnCode = PayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);

        vnp_Params.put("vnp_Locale", "vn");
        System.out.println(order.getId());
        vnp_Params.put("vnp_ReturnUrl", PayConfig.vnp_ReturnUrl);
        System.out.println(PayConfig.vnp_ReturnUrl+order.getId());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = PayConfig.hmacSHA512(PayConfig.secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = PayConfig.vnp_PayUrl + "?" + queryUrl;

        try {
            response.sendRedirect(paymentUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return PayConfig.vnp_PayUrl;
    }
//    @RequestMapping("/success/{id}")
//    public String Build(@PathVariable("id") Integer id){
//        return "/index";
//    }

    private BigDecimal calculateTotalAmount(List<OrderDetail> orderDetails) {
        // Tính tổng số tiền của các đơn hàng
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail orderDetail : orderDetails) {
            BigDecimal price = BigDecimal.valueOf(orderDetail.getPrice()*orderDetail.getQuantity());
            total = total.add(price);
        }
        return total;
    }
}
