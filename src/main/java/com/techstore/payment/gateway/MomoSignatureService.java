package com.techstore.payment.gateway;

import com.techstore.payment.config.MomoProperties;
import com.techstore.payment.dto.request.MomoCreateApiRequest;
import com.techstore.payment.dto.request.MomoResultRequest;
import com.techstore.payment.dto.response.MomoCreateApiResponse;
import com.techstore.payment.exception.PaymentErrorCode;
import com.techstore.payment.exception.PaymentException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MomoSignatureService {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final MomoProperties properties;

    public MomoSignatureService(MomoProperties properties) {
        this.properties = properties;
    }

    public String signCreate(
            long amount,
            String extraData,
            String orderId,
            String orderInfo,
            String requestId
    ) {
        String raw = "accessKey=" + properties.accessKey()
                + "&amount=" + amount
                + "&extraData=" + value(extraData)
                + "&ipnUrl=" + properties.ipnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.partnerCode()
                + "&redirectUrl=" + properties.redirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + properties.requestType();
        return hmac(raw);
    }

    public boolean verifyCreateResponse(MomoCreateApiResponse response) {
        if (response == null || response.signature() == null) {
            return false;
        }
        String raw = "accessKey=" + properties.accessKey()
                + "&amount=" + value(response.amount())
                + "&message=" + value(response.message())
                + "&orderId=" + value(response.orderId())
                + "&partnerCode=" + value(response.partnerCode())
                + "&payUrl=" + value(response.payUrl())
                + "&requestId=" + value(response.requestId())
                + "&responseTime=" + value(response.responseTime())
                + "&resultCode=" + value(response.resultCode());
        return secureEquals(hmac(raw), response.signature());
    }

    public boolean verifyResult(MomoResultRequest request) {
        if (request == null || request.signature() == null) {
            return false;
        }
        String raw = "accessKey=" + properties.accessKey()
                + "&amount=" + value(request.amount())
                + "&extraData=" + value(request.extraData())
                + "&message=" + value(request.message())
                + "&orderId=" + value(request.orderId())
                + "&orderInfo=" + value(request.orderInfo())
                + "&orderType=" + value(request.orderType())
                + "&partnerCode=" + value(request.partnerCode())
                + "&payType=" + value(request.payType())
                + "&requestId=" + value(request.requestId())
                + "&responseTime=" + value(request.responseTime())
                + "&resultCode=" + value(request.resultCode())
                + "&transId=" + value(request.transId());
        return secureEquals(hmac(raw), request.signature());
    }

    public MomoCreateApiRequest createRequest(
            long amount,
            String orderId,
            String orderInfo,
            String requestId
    ) {
        String extraData = "";
        return new MomoCreateApiRequest(
                properties.partnerCode(),
                properties.requestType(),
                properties.ipnUrl(),
                properties.redirectUrl(),
                orderId,
                amount,
                orderInfo,
                requestId,
                extraData,
                signCreate(amount, extraData, orderId, orderInfo, requestId),
                properties.language()
        );
    }

    private String hmac(String raw) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(
                    properties.secretKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA_256
            ));
            return HexFormat.of().formatHex(
                    mac.doFinal(raw.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new PaymentException(
                    PaymentErrorCode.MOMO_REQUEST_FAILED,
                    exception
            );
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
