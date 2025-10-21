package io.zhc1.realworld.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "io.zhc1.realworld.model")
@EnableJpaRepositories(basePackages = "io.zhc1.realworld.persistence")
public class TestConfiguration {}
