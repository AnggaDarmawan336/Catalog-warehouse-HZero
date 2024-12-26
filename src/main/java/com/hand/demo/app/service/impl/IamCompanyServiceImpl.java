package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.IamCompanyService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.IamCompany;
import com.hand.demo.domain.repository.IamCompanyRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (IamCompany)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:27:26
 */
@Service
public class IamCompanyServiceImpl implements IamCompanyService {
    @Autowired
    private IamCompanyRepository iamCompanyRepository;

    @Override
    public Page<IamCompany> selectList(PageRequest pageRequest, IamCompany iamCompany) {
        return PageHelper.doPageAndSort(pageRequest, () -> iamCompanyRepository.selectList(iamCompany));
    }

    @Override
    public void saveData(List<IamCompany> iamCompanys) {
        List<IamCompany> insertList = iamCompanys.stream().filter(line -> line.getCompanyId() == null).collect(Collectors.toList());
        List<IamCompany> updateList = iamCompanys.stream().filter(line -> line.getCompanyId() != null).collect(Collectors.toList());
        iamCompanyRepository.batchInsertSelective(insertList);
        iamCompanyRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

