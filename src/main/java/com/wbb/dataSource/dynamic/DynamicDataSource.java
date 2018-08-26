package com.wbb.dataSource.dynamic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;

import javax.sql.DataSource;

/**
 */
public class DynamicDataSource extends AbstractRoutingDataSource implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Connection connection = null;

    private ResultSet resultSet = null;

    public static Map<Object, Object> dataSourceMap = new HashMap<>(16);

    /**
     * 连接数据源前,调用该方法
     */
//	@Override
//	protected Object determineCurrentLookupKey() {
//		//1.获取手动设置的数据源参数DataSourceBean
//		DataSourceBean dataSourceBean = DataSourceContext.getDataSource();
//		if(dataSourceBean == null) {
//			return null;
//		}
//		try {
//			//2.获取AbstractRoutingDataSource的targetDataSources属性,该属性存放数据源属性
//			Map<Object, Object> targetSourceMap = getTargetSource();
//			synchronized(this) {
//				/*
//				 * 3.判断targetDataSources中是否已经存在要设置的数据源bean
//				 * 存在的话,则直接返回beanName
//				 *
//				 */
//				if(!targetSourceMap.keySet().contains(dataSourceBean.getBeanName())) {
//					/*不存在，则进行以下几步
//					3.1 先在spring容器中创建该数据源bean
//					*/
//					Object dataSource = createDataSource(dataSourceBean);
//					//3.2 在创建后的bean,放入到targetDataSources Map中
//					targetSourceMap.put(dataSourceBean.getBeanName(), dataSource);
//					/*
//					 * 3.3 通知spring有bean更新
//					 * 主要更新AbstractRoutingDataSource的resolvedDefaultDataSource(Map)属性,
//					 * 更新完以后,AbstractRoutingDataSource的determineTargetDataSource()中,才能找到数据源
//					 * 代码如下：
//					 * Object lookupKey = determineCurrentLookupKey();
//					   DataSource dataSource = this.resolvedDataSources.get(lookupKey);
//					 */
//					this.initializeDataSource();
//					super.afterPropertiesSet();
//				}
//			}
//			return dataSourceBean.getBeanName();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		this.initializeDataSource();
//		super.afterPropertiesSet();
//		return null;
//	}

    /**
     * 根据数据源信息在spring中创建bean,并返回
     *
     * @param dataSourceBean 数据源信息
     * @return
     * @throws IllegalAccessException
     */
    public Object createDataSource(DataSourceBean dataSourceBean) throws IllegalAccessException {
        //1.将applicationContext转化为ConfigurableApplicationContext
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        //2.获取bean工厂并转换为DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        /*
         * 3.本文用的是DruidDataSource,所有在这里我们获取的是该bean的BeanDefinitionBuilder,
         * 通过BeanDefinitionBuilder来创建bean定义
         */
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DruidDataSource.class);
        /**
         * 4.获取DataSourceBean里的属性和对应值,并将其交给BeanDefinitionBuilder创建bean的定义
         */
        Map<String, Object> propertyKeyValues = getPropertyKeyValues(DataSourceBean.class, dataSourceBean);
        for (Map.Entry<String, Object> entry : propertyKeyValues.entrySet()) {
            beanDefinitionBuilder.addPropertyValue(entry.getKey(), entry.getValue());
        }
        //5.bean定义好以后,将其交给beanFactory注册成bean对象，由spring容器管理
        beanFactory.registerBeanDefinition(dataSourceBean.getBeanName(), beanDefinitionBuilder.getBeanDefinition());
        //6.最后获取步骤5生成的bean,并将其返回
        return context.getBean(dataSourceBean.getBeanName());
    }

    //获取类属性和对应的值,放入Map中
    @SuppressWarnings("unused")
    private <T> Map<String, Object> getPropertyKeyValues(Class<T> clazz, Object object) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, Object> map = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(object));
        }
        map.remove("beanName");
        return map;
    }

    //通过反射获取AbstractRoutingDataSource的targetDataSources属性
    @SuppressWarnings("unchecked")
    public Map<Object, Object> getTargetSource() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = AbstractRoutingDataSource.class.getDeclaredField("targetDataSources");
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化数据源
     */
    private void initializeDataSource() {
        try {
            connection = this.getConnection();
            String sql = "SELECT * FROM other_datasource";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setDriverClassName("com.mysql.jdbc.Driver");
                dataSource.setUrl("jdbc:mysql://" + resultSet.getString("database_ip") + ":" + resultSet.getString("database_port") + "/" + resultSet.getString("database_name"));
                dataSource.setUsername(resultSet.getString("database_username"));
                dataSource.setPassword(resultSet.getString("database_password"));
                dataSource.setInitialSize(5);
                dataSource.setMinIdle(5);
                dataSource.setDefaultAutoCommit(Boolean.FALSE);
                dataSource.setTimeBetweenEvictionRunsMillis(60000);
                logger.info("> 配置数据库:" + resultSet.getString("datasource_name"));
                dataSourceMap.put(resultSet.getString("datasource_name"), dataSource);
            }
            this.setTargetDataSources(dataSourceMap);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.closeConnection(connection, resultSet);
        }
        logger.info("> 共装载" + dataSourceMap.size() + "个数据源,动态配置数据源完成");

    }

    /**
     * 关闭连接
     *
     * @param connection 数据库连接
     * @param resultSet  结果集
     */
    private void closeConnection(Connection connection, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        this.initializeDataSource();
        super.afterPropertiesSet();
    }

    @Override
    public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
        super.setDataSourceLookup(dataSourceLookup);
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }

    @Override
    public void setTargetDataSources(Map targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        //重点
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
//		DataSourceBean dataSourceBean = new DataSourceBean(new DataSourceBean.DataSourceBeanBuilder(
//				"ttttt",null,null,null,null,null));;
        DataSourceBean dataSourceBean = DataSourceContext.getDataSource();
        try {
            if (dataSourceBean != null) {
                Map<Object, Object> targetSourceMap = getTargetSource();
                //如果当前数据库中不包含新添加的 需要添加呀
                if (!targetSourceMap.containsKey(dataSourceBean.getBeanName())) {
                    Object dataSource = createDataSource(dataSourceBean);
                    // 在创建后的bean,放入到targetDataSources Map中
                    targetSourceMap.put(dataSourceBean.getBeanName(), dataSource);
                    System.out.println(targetSourceMap);
                    System.out.println("数据源长度：" + targetSourceMap.size());
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info("> 当前数据源:" + DataSourceContextHolder.getDataSource());
        return DataSourceContextHolder.getDataSource();
    }

}
