package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvCountExtra;
import com.hand.demo.domain.repository.InvCountExtraRepository;
import com.hand.demo.infra.mapper.InvCountExtraMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvCountExtra)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:28:07
 */
@Component
public class InvCountExtraRepositoryImpl extends BaseRepositoryImpl<InvCountExtra> implements InvCountExtraRepository {
    @Resource
    private InvCountExtraMapper invCountExtraMapper;

    @Override
    public List<InvCountExtra> selectList(InvCountExtra invCountExtra) {
        return invCountExtraMapper.selectList(invCountExtra);
    }

    @Override
    public InvCountExtra selectByPrimary(Long extraInfoId) {
        InvCountExtra invCountExtra = new InvCountExtra();
        invCountExtra.setExtraInfoId(extraInfoId);
        List<InvCountExtra> invCountExtras = invCountExtraMapper.selectList(invCountExtra);
        if (invCountExtras.size() == 0) {
            return null;
        }
        return invCountExtras.get(0);
    }

}

