package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.IamCompany;
import com.hand.demo.domain.repository.IamCompanyRepository;
import com.hand.demo.infra.mapper.IamCompanyMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (IamCompany)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:27:26
 */
@Component
public class IamCompanyRepositoryImpl extends BaseRepositoryImpl<IamCompany> implements IamCompanyRepository {
    @Resource
    private IamCompanyMapper iamCompanyMapper;

    @Override
    public List<IamCompany> selectList(IamCompany iamCompany) {
        return iamCompanyMapper.selectList(iamCompany);
    }

    @Override
    public IamCompany selectByPrimary(Long companyId) {
        IamCompany iamCompany = new IamCompany();
        iamCompany.setCompanyId(companyId);
        List<IamCompany> iamCompanys = iamCompanyMapper.selectList(iamCompany);
        if (iamCompanys.size() == 0) {
            return null;
        }
        return iamCompanys.get(0);
    }

}

