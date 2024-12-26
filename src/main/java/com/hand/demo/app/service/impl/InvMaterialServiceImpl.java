package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvMaterialService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvMaterial;
import com.hand.demo.domain.repository.InvMaterialRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvMaterial)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:28:46
 */
@Service
public class InvMaterialServiceImpl implements InvMaterialService {
    @Autowired
    private InvMaterialRepository invMaterialRepository;

    @Override
    public Page<InvMaterial> selectList(PageRequest pageRequest, InvMaterial invMaterial) {
        return PageHelper.doPageAndSort(pageRequest, () -> invMaterialRepository.selectList(invMaterial));
    }

    @Override
    public void saveData(List<InvMaterial> invMaterials) {
        List<InvMaterial> insertList = invMaterials.stream().filter(line -> line.getMaterialId() == null).collect(Collectors.toList());
        List<InvMaterial> updateList = invMaterials.stream().filter(line -> line.getMaterialId() != null).collect(Collectors.toList());
        invMaterialRepository.batchInsertSelective(insertList);
        invMaterialRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

