package com.wbb.dataSource.dynamic;

public class DataSourceContextHolder {

    public static final String DEFAULT_DATA_SOURCE = "defaultDataSource";

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程数据源
     *
     * @param dbName 数据源名称
     */
    public static void setDataSource(String dbName) {
        CONTEXT_HOLDER.set(dbName);
    }

    /**
     * 获取当前数据源
     *
     * @return 返回数据源名称
     */
    public static String getDataSource() {

        String dbName = CONTEXT_HOLDER.get();

        return dbName == null ? DEFAULT_DATA_SOURCE : dbName;
    }

    /**
     * 清空当前数据源
     */
    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
    }

}

