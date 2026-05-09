package com.silky.starter.redis.stream.support;

/**
 * Redis Stream 消息存储 Field 名称常量
 *
 * @author zy
 * @date 2026-05-09
 */
public final class StreamFieldConstants {

    /**
     * Stream Record 中存储 payload JSON 的 field 名
     */
    public static final String FIELD_PAYLOAD = "payload";

    /**
     * Stream Record 中存储发送时间戳的 field 名
     */
    public static final String FIELD_TIMESTAMP = "timestamp";

    /**
     * Stream Record 中存储 headers JSON 的 field 名
     */
    public static final String FIELD_HEADERS = "headers";

    private StreamFieldConstants() {
    }
}
