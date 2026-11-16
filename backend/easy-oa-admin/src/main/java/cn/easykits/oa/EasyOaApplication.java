package cn.easykits.oa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * easy-oa 管理后台接口服务启动类。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EasyOaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyOaApplication.class, args);
    }
}
