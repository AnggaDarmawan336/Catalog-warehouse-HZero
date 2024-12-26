package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountExtra;

import java.util.List;

/**
 * (InvCountExtra)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:28:07
 */
public interface InvCountExtraService {

    /**
     * 查询数据
     *
     * @param pageRequest    分页参数
     * @param invCountExtras 查询条件
     * @return 返回值
     */
    Page<InvCountExtra> selectList(PageRequest pageRequest, InvCountExtra invCountExtras);

    /**
     * 保存数据
     *
     * @param invCountExtras 数据
     */
    void saveData(List<InvCountExtra> invCountExtras);

}

