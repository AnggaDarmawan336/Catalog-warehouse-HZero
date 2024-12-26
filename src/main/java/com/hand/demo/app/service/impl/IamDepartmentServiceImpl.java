package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.IamDepartmentService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.IamDepartment;
import com.hand.demo.domain.repository.IamDepartmentRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (IamDepartment)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:27:41
 */
@Service
public class IamDepartmentServiceImpl implements IamDepartmentService {
    @Autowired
    private IamDepartmentRepository iamDepartmentRepository;

    @Override
    public Page<IamDepartment> selectList(PageRequest pageRequest, IamDepartment iamDepartment) {
        return PageHelper.doPageAndSort(pageRequest, () -> iamDepartmentRepository.selectList(iamDepartment));
    }

    @Override
    public void saveData(List<IamDepartment> iamDepartments) {
        List<IamDepartment> insertList = iamDepartments.stream().filter(line -> line.getDepartmentId() == null).collect(Collectors.toList());
        List<IamDepartment> updateList = iamDepartments.stream().filter(line -> line.getDepartmentId() != null).collect(Collectors.toList());
        iamDepartmentRepository.batchInsertSelective(insertList);
        iamDepartmentRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

