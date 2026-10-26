package cn.lims;

import cn.lims.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class LimsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LimsApplication.class, args);
    }
}

