package org.slowcoders.basecamp.db;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.slowcoders.basecamp.db.mybatis.handler.UuidTypeHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackages = {
        // mapper class scan path 명시적 지정. 로딩 속도 향상.
        "org.slowcoders.basecamp.app.mapper",
    }, sqlSessionFactoryRef = "sqlSessionFactory")
public class DataSourceConfig {

    //    @Primary
    @Bean(name = "mainDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.main.hikari")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "replicaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.replica.hikari")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @DependsOn({"mainDataSource", "replicaDataSource"})
    public DataSource routingDataSource(
            @Qualifier("mainDataSource") final DataSource masterDataSource,
            @Qualifier("replicaDataSource") final DataSource slaveDataSource) {

        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(ReplicationRoutingDataSource.MAIN, masterDataSource);
        dataSourceMap.put(ReplicationRoutingDataSource.REPLICA, slaveDataSource);
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        return routingDataSource;

    }

    @Bean
    @Primary
    @DependsOn("routingDataSource")
    public DataSource dataSource(DataSource routingDataSource) {
        /** LazyConnectionDataSourceProxy: SQL 실행 시점까지 Transaction 생성 보류.
         * 1) 멀티 데이터소스 사용 시 생성 권잠됨. (효율성 및 정확성 높이기)
         * 2) Cache 사용 효율성 극대화.
         */
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

//    @Bean
//    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
//        return configuration -> {
//            configuration.addInterceptor(new LogInterceptor());
//            configuration.getTypeAliasRegistry().registerAlias("MANAGED", HpmsTransactionFactory.class);
//        };
//    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisProperties properties) throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        properties.getConfiguration().applyTo(configuration);

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setConfiguration(configuration);
        sqlSessionFactoryBean.setMapperLocations(properties.resolveMapperLocations());
        sqlSessionFactoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        sqlSessionFactoryBean.setConfigurationProperties(properties.getConfigurationProperties());

        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setTypeHandlers(new UuidTypeHandler());
        sqlSessionFactoryBean.setTransactionFactory(new ManagedTransactionFactory());
        return sqlSessionFactoryBean.getObject();
    }


    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        return transactionManager;
    }

}
