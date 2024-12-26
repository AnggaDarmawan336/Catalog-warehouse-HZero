package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.IamDepartment;
import com.hand.demo.domain.repository.IamDepartmentRepository;
import com.hand.demo.infra.mapper.IamDepartmentMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (IamDepartment)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:27:41
 */
@Component
public class IamDepartmentRepositoryImpl extends BaseRepositoryImpl<IamDepartment> implements IamDepartmentRepository {
    @Resource
    private IamDepartmentMapper iamDepartmentMapper;

    @Override
    public List<IamDepartment> selectList(IamDepartment iamDepartment) {
        return iamDepartmentMapper.selectList(iamDepartment);
    }

    @Override
    public IamDepartment selectByPrimary(Long departmentId) {
        IamDepartment iamDepartment = new IamDepartment();
        iamDepartment.setDepartmentId(departmentId);
        List<IamDepartment> iamDepartments = iamDepartmentMapper.selectList(iamDepartment);
        if (iamDepartments.size() == 0) {
            return null;
        }
        return iamDepartments.get(0);
    }

}

