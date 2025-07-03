package com.example.lunchapp.config;

import com.example.lunchapp.repository.RoleRepository;
import com.example.lunchapp.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {
        "com.example.lunchapp.service",
        "com.example.lunchapp.repository",
        "com.example.lunchapp.controller"
})
@EnableJpaRepositories(basePackages = "com.example.lunchapp.repository")
@EnableTransactionManagement
@EnableAspectJAutoProxy
@PropertySources({
        @PropertySource("classpath:database.properties"),
        @PropertySource("classpath:application.properties")
})
@Import(WebSocketConfig.class)
public class AppConfig {

    @Autowired
    private Environment env;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String dbUrl = System.getenv("DATABASE_URL") != null
                ? System.getenv("DATABASE_URL")
                : env.getProperty("db.url");

        String dbUsername = System.getenv("DATABASE_USER") != null
                ? System.getenv("DATABASE_USER")
                : env.getProperty("db.username");

        String dbPassword = System.getenv("DATABASE_PASSWORD") != null
                ? System.getenv("DATABASE_PASSWORD")
                : env.getProperty("db.password");

        dataSource.setDriverClassName(env.getProperty("db.driver"));
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("com.example.lunchapp.model.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaProperties(additionalProperties());
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();

        String hbm2ddl = System.getenv("HIBERNATE_HBM2DDL_AUTO") != null
                ? System.getenv("HIBERNATE_HBM2DDL_AUTO")
                : env.getProperty("hibernate.hbm2ddl.auto");

        properties.setProperty("hibernate.hbm2ddl.auto", hbm2ddl);
        properties.setProperty("hibernate.dialect", env.getProperty("hibernate.dialect"));
        properties.setProperty("hibernate.show_sql", env.getProperty("hibernate.show_sql"));
        properties.setProperty("hibernate.format_sql", env.getProperty("hibernate.format_sql"));

        if (System.getenv("DATABASE_URL") != null) {
            properties.setProperty("hibernate.connection.url", System.getenv("DATABASE_URL") + "?serverTimezone=UTC&useSSL=false");
        }

        return properties;
    }

    @Bean
    public InitializingBean initializeDefaultData(CategoryService categoryService, RoleRepository roleRepository) {
        return () -> {
            categoryService.createDefaultCategoriesIfNotExist();
            if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
                roleRepository.save(new com.example.lunchapp.model.entity.Role("ROLE_ADMIN"));
            }
            if (roleRepository.findByName("ROLE_USER").isEmpty()) {
                roleRepository.save(new com.example.lunchapp.model.entity.Role("ROLE_USER"));
            }
        };
    }
}