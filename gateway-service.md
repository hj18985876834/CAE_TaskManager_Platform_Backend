# 四、gateway-service 项目结构设计

网关职责单一，所以结构要轻。

## 1. 推荐结构

```text
gateway-service/
└── src/main/java/com/yourorg/gateway/
    ├── GatewayApplication.java
    ├── config/
    ├── filter/
    ├── handler/
    ├── router/
    └── support/
```

## 2. 目录职责

### `config/`

- `GatewayRouteConfig`
- `CorsConfig`
- `SecurityWhiteListConfig`

### `filter/`

- `JwtAuthFilter`
- `RequestLogFilter`
- `TraceIdFilter`

### `handler/`

- `GatewayExceptionHandler`

### `router/`

如果你后期想做 Java 配置路由，可以放这里；如果全用 yml，也可以不要。

### `support/`

放一些辅助类：

- `TokenParser`
- `PathMatcherSupport`

## 3. 网关不该做什么

网关**不要写业务逻辑**，只做：

- 路由
- 鉴权
- Header 注入
- 全局异常
- 请求日志

---------

# 四、gateway-service 完整包树

你的设计里已经把网关职责限定为“路由、鉴权、日志、异常处理”，这是对的。网关不应写业务逻辑。

## 1. 完整结构

```text
gateway-service/
└── src/main/java/com/example/cae/gateway/
    ├── GatewayApplication.java
    ├── config/
    │   ├── GatewayRouteConfig.java
    │   ├── CorsConfig.java
    │   └── WhiteListConfig.java
    ├── filter/
    │   ├── JwtAuthFilter.java
    │   ├── RequestLogFilter.java
    │   └── TraceIdFilter.java
    ├── handler/
    │   └── GatewayExceptionHandler.java
    ├── router/
    │   └── RouteDefinitionLoader.java
    └── support/
        ├── TokenParser.java
        ├── PathMatcherSupport.java
        └── GatewayResponseWriter.java
```

## 2. 每个类作用

### `GatewayApplication`

启动类。

### `GatewayRouteConfig`

统一定义路由规则，或者读取 yml 后做补充配置。

### `CorsConfig`

统一跨域。

### `WhiteListConfig`

定义无需登录的路径，比如：

- `/api/auth/login`
- `/api/auth/logout`

### `JwtAuthFilter`

负责：

- 读取 `Authorization: Bearer xxx`
- 校验 token
- 解析 `userId` 和 `roleCode`
- 注入 `X-User-Id`、`X-Role-Code`

### `RequestLogFilter`

打印请求方法、路径、耗时。

### `TraceIdFilter`

给每个请求加 traceId，方便联调查日志。

### `GatewayExceptionHandler`

统一把异常转成 `Result`。



-----------

# 三、gateway-service 初始化代码骨架清单

网关职责最轻，但一定先搭好，因为它是统一入口。你当前后端设计里已经明确：网关只做路由、鉴权、日志和异常，不写业务。

## 1. 必建类

### `GatewayApplication.java`

职责：启动类

---

### `config/GatewayRouteConfig.java`

职责：路由配置

建议方法：

* `customRouteLocator(RouteLocatorBuilder builder)`

---

### `config/CorsConfig.java`

职责：跨域配置

建议方法：

* `corsWebFilter()`

---

### `config/WhiteListConfig.java`

职责：白名单路径维护

建议方法：

* `getWhiteListPaths()`

---

### `filter/JwtAuthFilter.java`

职责：

* 校验 Bearer Token
* 白名单放行
* 解析 `userId / roleCode`
* 写入下游请求头

建议方法：

* `filter(ServerWebExchange exchange, GatewayFilterChain chain)`
* `isWhitePath(String path)`
* `resolveToken(ServerHttpRequest request)`
* `writeUserHeaders(ServerHttpRequest request, Long userId, String roleCode)`

---

### `filter/RequestLogFilter.java`

职责：记录请求日志

建议方法：

* `filter(...)`
* `buildAccessLog(...)`

---

### `filter/TraceIdFilter.java`

职责：注入 traceId

建议方法：

* `filter(...)`
* `generateTraceId()`

---

### `handler/GatewayExceptionHandler.java`

职责：统一异常响应

建议方法：

* `handle(ServerWebExchange exchange, Throwable ex)`

--------

下面我继续给你 **`gateway-service` 的完整初始化代码骨架**，保持和前面 `task-service / scheduler-service / node-agent / common-lib` 一样的粒度，并严格对齐你已经统一后的后端设计口径：

- `gateway-service` 是**统一入口**
- 只负责：**路由、鉴权、异常处理、请求日志**
- **不写业务逻辑**
- 使用 `Authorization: Bearer {token}`
- 解析后向下游透传 `X-User-Id / X-Role-Code`
- 白名单至少包含登录等无需鉴权接口

------

# 一、gateway-service 的定位

`gateway-service` 在整个系统里的职责非常克制，只做 4 件事：

1. 路由转发
2. JWT 鉴权
3. 统一异常处理
4. 请求日志/链路标识

它**不负责**：

- 用户登录逻辑
- 任务业务逻辑
- 权限业务判断
- 数据查询

所以网关最怕两个坑：

- 把权限业务写进网关
- 把网关写成半个后端服务

正确做法是让它保持“轻量、稳定、边界清晰”。

------

# 二、gateway-service 最终推荐包树

```text
gateway-service/
└── src/main/java/com/example/cae/gateway/
    ├── GatewayApplication.java
    ├── config/
    │   ├── GatewayRouteConfig.java
    │   ├── CorsConfig.java
    │   ├── WhiteListConfig.java
    │   └── GatewayBeanConfig.java
    ├── filter/
    │   ├── JwtAuthFilter.java
    │   ├── RequestLogFilter.java
    │   └── TraceIdFilter.java
    ├── handler/
    │   └── GatewayExceptionHandler.java
    ├── router/
    │   └── RouteDefinitionLoader.java
    ├── support/
    │   ├── TokenParser.java
    │   ├── PathMatcherSupport.java
    │   ├── GatewayRequestMutator.java
    │   └── GatewayResponseWriter.java
    └── properties/
        └── GatewaySecurityProperties.java
```

这个结构和你前面已经定下来的“gateway-service 只做路由、JWT 鉴权、全局异常和日志”的后端设计是一致的。

------

# 三、gateway-service 的核心调用链

## 1. 请求进入链路

```
TraceIdFilter -> RequestLogFilter -> JwtAuthFilter -> Route
```

## 2. 登录/白名单请求

```
TraceIdFilter -> RequestLogFilter -> 白名单放行 -> Route
```

## 3. 异常处理链

```
Filter/Route Exception -> GatewayExceptionHandler -> Result
```

这样设计后，链路清晰，不容易互相污染。

------

# 四、配置类骨架

------

## 1. GatewayApplication.java

```java
package com.example.cae.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

------

## 2. GatewayRouteConfig.java

如果你采用 Java 方式声明路由，可以先用这一版；如果你更习惯 yml，也可以把这个类留空或只做补充配置。

```java
package com.example.cae.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/auth/**", "/api/users/**")
                        .uri("lb://user-service"))
                .route("solver-service", r -> r.path("/api/solvers/**", "/api/profiles/**")
                        .uri("lb://solver-service"))
                .route("task-service", r -> r.path("/api/tasks/**", "/api/admin/tasks/**")
                        .uri("lb://task-service"))
                .route("scheduler-service", r -> r.path("/api/nodes/**", "/api/schedules/**")
                        .uri("lb://scheduler-service"))
                .build();
    }
}
```

### 说明

这里路由划分要与接口设计、后端设计中的服务边界保持一致。

------

## 3. CorsConfig.java

```java
package com.example.cae.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
```

------

## 4. WhiteListConfig.java

职责：维护无需登录的路径。

```java
package com.example.cae.gateway.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WhiteListConfig {

    public List<String> getWhiteListPaths() {
        return List.of(
                "/api/auth/login",
                "/api/auth/logout"
        );
    }
}
```

### 建议

最开始只保留最少白名单，后面需要再补。
不要一开始把太多接口放白名单。

------

## 5. GatewayBeanConfig.java

如果你后面需要统一提供一些辅助 bean，例如 `AntPathMatcher`，可以放这里。

```java
package com.example.cae.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

@Configuration
public class GatewayBeanConfig {

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }
}
```

------

## 6. GatewaySecurityProperties.java

如果你希望白名单和 token 配置通过配置文件维护，建议加这个属性类。

```java
package com.example.cae.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private List<String> whiteList;
}
```

后面可以替代 `WhiteListConfig` 的硬编码。

------

# 五、Filter 层完整骨架

这是网关最核心的一层。

------

## 1. TraceIdFilter.java

职责：给每个请求生成 traceId，便于日志串联。

```java
package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String traceId = UUID.randomUUID().toString().replace("-", "");
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(HeaderConstants.TRACE_ID, traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -300;
    }
}
```

------

## 2. RequestLogFilter.java

职责：记录请求路径、方法、耗时、traceId。

```java
package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        long start = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null ? "UNKNOWN"
                : exchange.getRequest().getMethod().name();
        String traceId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.TRACE_ID);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long cost = System.currentTimeMillis() - start;
                    log.info("gateway request, traceId={}, method={}, path={}, cost={}ms",
                            traceId, method, path, cost);
                });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
```

------

## 3. JwtAuthFilter.java

这是最关键的过滤器。

职责：

- 白名单放行
- 提取 Bearer Token
- 校验 token
- 解析 userId / roleCode
- 写入下游 header

```java
package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.constant.SecurityConstants;
import com.example.cae.common.exception.UnauthorizedException;
import com.example.cae.common.utils.JwtUtil;
import com.example.cae.gateway.config.WhiteListConfig;
import com.example.cae.gateway.support.GatewayRequestMutator;
import com.example.cae.gateway.support.PathMatcherSupport;
import com.example.cae.gateway.support.TokenParser;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final WhiteListConfig whiteListConfig;
    private final PathMatcherSupport pathMatcherSupport;
    private final TokenParser tokenParser;
    private final GatewayRequestMutator gatewayRequestMutator;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (pathMatcherSupport.matchAny(path, whiteListConfig.getWhiteListPaths())) {
            return chain.filter(exchange);
        }

        String rawToken = tokenParser.resolveToken(exchange.getRequest());

        if (rawToken == null || !JwtUtil.validateToken(rawToken)) {
            return Mono.error(new UnauthorizedException("token 无效或缺失"));
        }

        Long userId = JwtUtil.parseUserId(rawToken);
        String roleCode = JwtUtil.parseRoleCode(rawToken);

        ServerHttpRequest mutatedRequest = gatewayRequestMutator.withUserContext(
                exchange.getRequest(), userId, roleCode
        );

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

### 说明

这里的 Bearer Token 和 `X-User-Id / X-Role-Code` 透传，是你当前统一后设计口径里的关键点。

------

# 六、Support 层完整骨架

这一层是为了让 Filter 更干净，不把小逻辑都塞进去。

------

## 1. TokenParser.java

```java
package com.example.cae.gateway.support;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.constant.SecurityConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class TokenParser {

    public String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HeaderConstants.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        if (!authorization.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return null;
        }

        return authorization.substring(SecurityConstants.TOKEN_PREFIX.length());
    }
}
```

------

## 2. PathMatcherSupport.java

```java
package com.example.cae.gateway.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PathMatcherSupport {

    private final AntPathMatcher antPathMatcher;

    public boolean matchAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }
}
```

------

## 3. GatewayRequestMutator.java

职责：给下游写用户上下文 header。

```java
package com.example.cae.gateway.support;

import com.example.cae.common.constant.HeaderConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class GatewayRequestMutator {

    public ServerHttpRequest withUserContext(ServerHttpRequest request, Long userId, String roleCode) {
        return request.mutate()
                .header(HeaderConstants.X_USER_ID, String.valueOf(userId))
                .header(HeaderConstants.X_ROLE_CODE, roleCode)
                .build();
    }
}
```

------

## 4. GatewayResponseWriter.java

如果后面你希望在过滤器中直接输出 JSON，可以留这个工具类。
当前版本先预留即可。

```java
package com.example.cae.gateway.support;

import org.springframework.stereotype.Component;

@Component
public class GatewayResponseWriter {
}
```

------

# 七、异常处理骨架

------

## 1. GatewayExceptionHandler.java

职责：把网关层抛出的异常统一转成 JSON。

```java
package com.example.cae.gateway.handler;

import com.example.cae.common.exception.ForbiddenException;
import com.example.cae.common.exception.UnauthorizedException;
import com.example.cae.common.response.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(org.springframework.web.server.ServerWebExchange exchange, Throwable ex) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result;
        HttpStatus status;

        if (ex instanceof UnauthorizedException) {
            result = Result.fail(401, ex.getMessage());
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ForbiddenException) {
            result = Result.fail(403, ex.getMessage());
            status = HttpStatus.FORBIDDEN;
        } else {
            result = Result.fail(500, ex.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":500,\"message\":\"gateway error\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

------

# 八、router 层骨架

如果你后面要把路由从配置文件动态读取，可以预留这个类；当前可以先不用。

## 1. RouteDefinitionLoader.java

```java
package com.example.cae.gateway.router;

import org.springframework.stereotype.Component;

@Component
public class RouteDefinitionLoader {
}
```

------

# 九、gateway-service 需要的 yml 样例

建议你至少配一版这样的 `application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true

gateway:
  security:
    white-list:
      - /api/auth/login
      - /api/auth/logout
```

如果你用 Nacos/注册中心，可以再补注册发现配置。

------

# 十、最小可运行实现顺序

不要一次把网关写太大，按这个顺序最稳。

## 第一批

先建：

- `GatewayApplication`
- `CorsConfig`
- `WhiteListConfig`
- `JwtAuthFilter`
- `TokenParser`

先做到：

- 网关能启动
- 白名单接口能放行
- 非白名单接口必须带 token

------

## 第二批

再建：

- `TraceIdFilter`
- `RequestLogFilter`
- `PathMatcherSupport`
- `GatewayRequestMutator`

做到：

- 请求日志完整
- traceId 可贯穿
- 用户上下文可透传下游

------

## 第三批

再建：

- `GatewayExceptionHandler`
- `GatewayRouteConfig`

做到：

- 网关异常统一 JSON 化
- 路由明确可控

------

# 十一、你现在最该先建的 8 个类

如果你想马上开工，我建议 gateway 先建这 8 个：

1. `GatewayApplication`
2. `GatewayRouteConfig`
3. `CorsConfig`
4. `WhiteListConfig`
5. `JwtAuthFilter`
6. `TokenParser`
7. `TraceIdFilter`
8. `RequestLogFilter`

这 8 个一出来，网关主骨架就有了。

------

# 十二、最终建议

`gateway-service` 一定要保持“薄”：

- **Filter 做认证**
- **Config 做路由**
- **Handler 做异常**
- **Support 做小工具**
- **绝不写业务**

你现在这版骨架已经把最容易失控的部分都拆开了：

- `JwtAuthFilter` 专管鉴权
- `TraceIdFilter` 专管链路标识
- `RequestLogFilter` 专管日志
- `GatewayExceptionHandler` 专管异常
- `GatewayRequestMutator` 专管 header 透传

这就是比较规范、稳定、易维护的 gateway 结构，而且和你前面已经定下来的后端整体设计完全一致。



------

