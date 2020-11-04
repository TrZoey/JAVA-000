package io.github.kimmking.gateway.router;

import java.util.List;
import java.util.Random;

/**
 * @description:
 * @author: Xu Shiwei
 * @create: 2020-11-04
 **/
public class RandomHttpEndpointRouter implements HttpEndpointRouter{
    @Override
    public String route(List<String> endpoints) {
        return endpoints.get(new Random().nextInt(endpoints.size()));
    }
}