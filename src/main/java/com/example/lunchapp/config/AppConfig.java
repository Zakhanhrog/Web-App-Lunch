package com.example.lunchapp.config;

import com.example.lunchapp.repository.RoleRepository;
import com.example.lunchapp.service.CategoryService;
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
import org.springframework.util.StringUtils; // Thêm import này

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
        @PropertySource("classpath:database.properties"),
        @PropertySource("classpath:application.properties")
})
public class AppConfig {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("db.driver"));

        // Ưu tiên biến môi trường cho Railway
        String railwayDbUrl = env.getProperty("SPRING_DATASOURCE_URL"); // Hoặc tên biến bạn đặt trên Railway
        String railwayDbUsername = env.getProperty("SPRING_DATASOURCE_USERNAME");
        String railwayDbPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD");

        if (StringUtils.hasText(railwayDbUrl) && StringUtils.hasText(railwayDbUsername) && StringUtils.hasText(railwayDbPassword)) {
            dataSource.setUrl(railwayDbUrl);
            dataSource.setUsername(railwayDbUsername);
            dataSource.setPassword(railwayDbPassword);
        } else {
            // Fallback về cấu hình từ database.properties cho local
            dataSource.setUrl(env.getProperty("db.url"));
            dataSource.setUsername(env.getProperty("db.username"));
            dataSource.setPassword(env.getProperty("db.password"));
        }
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
        // Ưu tiên biến môi trường cho các thuộc tính Hibernate
        String ddlAuto = env.getProperty("SPRING_JPA_HIBERNATE_DDL_AUTO", env.getProperty("hibernate.hbm2ddl.auto"));
        String dialect = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", env.getProperty("hibernate.dialect"));
        String showSql = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_SHOW_SQL", env.getProperty("hibernate.show_sql"));
        String formatSql = env.getProperty("SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL", env.getProperty("hibernate.format_sql"));

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