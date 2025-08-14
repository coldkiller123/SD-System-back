package org.example.erp.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring上下文持有工具类
 * 用于在非Spring管理的类中获取Spring容器中的Bean实例
 * 实现ApplicationContextAware接口，使Spring容器启动时自动注入ApplicationContext
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    /**
     * 静态的Spring应用上下文对象，全局共享
     */
    private static ApplicationContext applicationContext;

    /**
     * Spring容器初始化时自动调用，注入应用上下文
     * 该方法由Spring框架调用，将ApplicationContext实例传入
     *
     * @param context Spring应用上下文对象
     * @throws BeansException 当获取应用上下文失败时抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 根据类类型从Spring容器中获取对应的Bean实例
     *
     * @param clazz 要获取的Bean的类类型
     * @param <T>   泛型参数，指定Bean的类型
     * @return 容器中对应类型的Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}