package com.example.lunchapp.config;

import com.example.lunchapp.repository.RoleRepository;
import com.example.lunchapp.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
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
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {
        "com.example.lunchapp.service",
        "com.example.lunchapp.repository",
})
@EnableJpaRepositories(basePackages = "com.example.lunchapp.repository")
@EnableTransactionManagement
@EnableAspectJAutoProxy
@PropertySources({
        @PropertySource(value = "classpath:database.properties", ignoreResourceNotFound = true),
        @PropertySource("classpath:application.properties")
})
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String driverClassName = env.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        dataSource.setDriverClassName(driverClassName);
        logger.info("Using JDBC Driver: {}", driverClassName);

        String railwayDbUrl = env.getProperty("SPRING_DATASOURCE_URL");
        String railwayDbUsername = env.getProperty("SPRING_DATASOURCE_USERNAME");
        String railwayDbPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD");

        logger.info("Attempting to read Railway DB URL from env: {}", railwayDbUrl);
        logger.info("Attempting to read Railway DB Username from env: {}", railwayDbUsername);

        if (StringUtils.hasText(railwayDbUrl) && StringUtils.hasText(railwayDbUsername) && railwayDbPassword != null) { // railwayDbPassword có thể là chuỗi rỗng, nên chỉ check null
            logger.warn("SUCCESS: Using Railway database configuration from environment variables.");
            dataSource.setUrl(railwayDbUrl);
            dataSource.setUsername(railwayDbUsername);
            dataSource.setPassword(railwayDbPassword);
        } else {
            logger.warn("FAILURE: Railway environment variables for DB not fully set or not found. Falling back to properties file (if available).");
            String localDbUrl = env.getProperty("db.url");
            String localDbUsername = env.getProperty("db.username");
            String localDbPassword = env.getProperty("db.password");

            logger.info("Local DB URL from properties: {}", localDbUrl);
            logger.info("Local DB Username from properties: {}", localDbUsername);

            if (StringUtils.hasText(localDbUrl)) {
                dataSource.setUrl(localDbUrl);
                dataSource.setUsername(localDbUsername);
                dataSource.setPassword(localDbPassword);
            } else {
                logger.error("CRITICAL: No database configuration found, neither from environment variables nor from properties file!");
                // Có thể ném một exception ở đây nếu muốn ứng dụng không khởi động khi không có DB config
            }
        }
        logger.info("Final DataSource URL configured: {}", dataSource.getUrl());
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

        String ddlAuto = env.getProperty("SPRING_JPA_HIBERNATE_DDL_AUTO", env.getProperty("hibernate.hbm2ddl.auto", "validate"));
        String dialect = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", env.getProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect"));
        String showSql = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_SHOW_SQL", env.getProperty("hibernate.show_sql", "false"));
        String formatSql = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL", env.getProperty("hibernate.format_sql", "false"));

        logger.info("Hibernate ddl.auto: {}", ddlAuto);
        logger.info("Hibernate dialect: {}", dialect);
        logger.info("Hibernate show_sql: {}", showSql);
        logger.info("Hibernate format_sql: {}", formatSql);

        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.show_sql", showSql);
        properties.setProperty("hibernate.format_sql", formatSql);
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