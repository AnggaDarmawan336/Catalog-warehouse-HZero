package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvMaterial;
import com.hand.demo.domain.repository.InvMaterialRepository;
import com.hand.demo.infra.mapper.InvMaterialMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvMaterial)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:28:46
 */
@Component
public class InvMaterialRepositoryImpl extends BaseRepositoryImpl<InvMaterial> implements InvMaterialRepository {
    @Resource
    private InvMaterialMapper invMaterialMapper;

    @Override
    public List<InvMaterial> selectList(InvMaterial invMaterial) {
        return invMaterialMapper.selectList(invMaterial);
    }

    @Override
    public InvMaterial selectByPrimary(Long materialId) {
        InvMaterial invMaterial = new InvMaterial();
        invMaterial.setMaterialId(materialId);
        List<InvMaterial> invMaterials = invMaterialMapper.selectList(invMaterial);
        if (invMaterials.size() == 0) {
            return null;
        }
        return invMaterials.get(0);
    }

}

