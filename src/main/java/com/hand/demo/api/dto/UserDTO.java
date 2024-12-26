package com.hand.demo.api.dto;

import lombok.Getter;
import lombok.Setter;
import org.hzero.common.HZeroCacheKey;
import org.hzero.core.cache.CacheValue;
import org.hzero.core.cache.Cacheable;

@Getter
@Setter
public class UserDTO implements Cacheable {
    private Long userId;

    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "userId", searchKey = "realName",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String realName;

    @CacheValue(key = HZeroCacheKey.USER,
            primaryKey = "userId",
            searchKey = "tenantNum",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String tenantCode;

    private String creator;
}