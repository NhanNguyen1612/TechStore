package com.techstore.payment.gateway;

import com.techstore.payment.config.MomoProperties;
import com.techstore.payment.dto.request.MomoCreateApiRequest;
import com.techstore.payment.dto.response.MomoCreateApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MomoGatewayClient {

    private final RestClient restClient;
    private final MomoProperties properties;

    public MomoGatewayClient(RestClient momoRestClient, MomoProperties properties) {
        this.restClient = momoRestClient;
        this.properties = properties;
    }

    public MomoCreateApiResponse createPayment(MomoCreateApiRequest request) {
        return restClient.post()
                .uri(properties.createEndpoint())
                .body(request)
                .retrieve()
                .body(MomoCreateApiResponse.class);
    }
}
