package com.atguigu.springcloud.controller;

import com.atguigu.springcloud.entities.CommonResult;
import com.atguigu.springcloud.entities.Payment;
import com.atguigu.springcloud.lb.MyLoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;

/**
 * @author lixiaolong
 * @date 2020/12/19 11:31
 * @description
 */
@RestController
@RequestMapping("consumer")
public class OrderController {

//    public static final String PAYMENT_URL = "http://localhost:8001";

    /**
     * Eureka 上生产者的服务名。
     * 同时在 restTemplate 配置上加上 @LoadBalanced 注解，负载均衡请求。
     * */
    public static final String PAYMENT_URL = "http://CLOUD-PAYMENT-SERVICE";

    @Resource
    private RestTemplate restTemplate;

    // 注入自定义的负载均衡规则
    @Resource
    private MyLoadBalancer myLoadBalancer;

    @Resource
    private DiscoveryClient discoveryClient;

    /**
     * post
     * RequestBody 入参
     * request url:
     * http://localhost:80/consumer/payment/create
     * body -> raw: {"serial":"22"}
     * */
    @PostMapping("/payment/create")
    public CommonResult<Payment> create(@RequestBody Payment payment) {
        return restTemplate.postForObject(PAYMENT_URL + "/payment/create", payment, CommonResult.class);
    }

    /**
     * get
     * request url:
     * http://localhost:80/consumer/payment/get/11
     * */
    @GetMapping("/payment/get/{id}")
    public CommonResult<Payment> getPayment(@PathVariable("id") Long id) {
        return restTemplate.getForObject(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
    }

    @GetMapping("/payment/queryAllByLimit")
    public CommonResult queryAllByLimit(@RequestParam(defaultValue = "0") int offset,
                                        @RequestParam(defaultValue = "10") int limit) {
        return restTemplate.getForObject(
                PAYMENT_URL + "/payment/queryAllByLimit?offset=" + offset + "&limit=" + limit, CommonResult.class);
    }

    @GetMapping("/payment/getForEntity/{id}")
    public CommonResult<Payment> getPayment2(@PathVariable("id") Long id) {
        ResponseEntity<CommonResult> entity = restTemplate.getForEntity(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
        if (entity.getStatusCode().is2xxSuccessful()) {
            return entity.getBody();
        } else {
            return new CommonResult<>(444, "操作失败");
        }
    }

    /**
     * @author lixiaolong
     * @date 2020/12/23 10:27
     * @description 测试自定义的负载均衡规则
     */
    @GetMapping(value = "/payment/lb")
    public String getPaymentLB() {
        // 获取注册中心上服务名为 CLOUD-PAYMENT-SERVICE 的实例
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        // 调用自定义的负载均衡策略
        ServiceInstance serviceInstance = myLoadBalancer.instances(instances);
        URI uri = serviceInstance.getUri();
        return restTemplate.getForObject(uri + "/payment/lb", String.class);

    }

    // ====================> zipkin+sleuth
    @GetMapping("/payment/zipkin")
    public String paymentZipkin() {
        String result = restTemplate.getForObject("http://localhost:8001" + "/payment/zipkin/", String.class);
        return result;
    }
}
