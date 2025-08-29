package com.silky.starter.oss.adapter.registry;

import com.silky.starter.oss.adapter.OssProviderAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * oss 适配器注册
 *
 * @author zy
 * @date 2025-08-12 13:57
 **/
public class OssAdapterRegistry {

    private final Map<String, OssProviderAdapter> adapters = new ConcurrentHashMap<>();

    /**
     * 注册适配器
     *
     * @param provider 供应商
     * @param adapter  适配器
     */
    public void register(String provider, OssProviderAdapter adapter) {
        adapters.put(provider, adapter);
    }

    /**
     * 获取适配器
     *
     * @param provider 供应商
     * @return 适配器
     */
    public OssProviderAdapter getAdapter(String provider) {
        return adapters.get(provider);
    }

    /**
     * 获取所有适配器
     *
     * @return 适配器映射
     */
    public Map<String, OssProviderAdapter> getAllAdapters() {
        return this.adapters;
    }
}
