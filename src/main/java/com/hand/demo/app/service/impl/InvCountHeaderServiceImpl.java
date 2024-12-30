package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hand.demo.api.dto.*;
import com.hand.demo.domain.entity.*;
import com.hand.demo.domain.repository.*;
import com.hand.demo.infra.constant.Constants;
import com.hand.demo.infra.mapper.InvCountHeaderMapper;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.apache.commons.collections.CollectionUtils;
import org.hzero.boot.apaas.common.userinfo.domain.UserVO;
import org.hzero.boot.apaas.common.userinfo.infra.feign.IamRemoteService;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.boot.platform.profile.ProfileClient;
import org.hzero.boot.workflow.WorkflowClient;
import org.hzero.boot.workflow.dto.RunInstance;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.hzero.core.base.BaseConstants;
import org.hzero.mybatis.common.Criteria;
import org.hzero.mybatis.domian.Condition;
import org.mule.mvel2.util.Make;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountHeaderService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * (InvCountHeader)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:28:19
 */
@Service
public class InvCountHeaderServiceImpl implements InvCountHeaderService {
    @Autowired
    InvCountHeaderRepository invCountHeaderRepository;

    private static final Logger logger = Logger.getLogger(InvCountHeaderServiceImpl.class.getName());

    @Autowired
    CodeRuleBuilder codeRuleBuilder;

    @Autowired
    InvCountLineRepository invCountLineRepository;

    @Autowired
    InvCountHeaderMapper invCountHeaderMapper;

    @Autowired
    InvWarehouseRepository invWarehouseRepository;

    @Autowired
    IamCompanyRepository iamCompanyRepository;

    @Autowired
    InvStockRepository invStockRepository;

    @Autowired
    IamDepartmentRepository iamDepartmentRepository;

    @Autowired
    IamRemoteService iamRemoteService;

    @Autowired
    InvMaterialRepository invMaterialRepository;

    @Autowired
    InvBatchRepository invBatchRepository;

    @Autowired
    InvCountExtraRepository invCountExtraRepository;

    @Autowired
    ProfileClient profileClient;

    @Autowired
    WorkflowClient workflowClient;

    @Autowired
    Utils utils;

    @Override
    public Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeader) {
        // get the data for UserVO
        ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();

        // Configure ObjectMapper for date handling
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Initialize UserVO
        UserVO userVO;
        try {
            userVO = objectMapper.readValue(stringResponseEntity.getBody(), UserVO.class);
            logger.info(stringResponseEntity.getBody());
        } catch (Exception e) {
            throw new CommonException(e);
        }
        // Check tenant admin flag and set tenantId or createdBy accordingly
        if (Boolean.FALSE.equals(userVO.getTenantAdminFlag())) {
            return PageHelper.doPageAndSort(pageRequest, ArrayList::new);
        }

        // Page Helper
        Page<InvCountHeaderDTO> pageResult = PageHelper.doPageAndSort(pageRequest, () ->
                invCountHeaderMapper.selectList(invCountHeader));

        // For Get Supervisor ID
        for (InvCountHeaderDTO invCountHeaderDTO : pageResult) {
            String[] supervisorId = invCountHeaderDTO.getSupervisorIds().split(",");
            List<UserDTO> userDTOList = new ArrayList<>();
            for (String supervisorIds : supervisorId) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(Long.parseLong(supervisorIds));
                userDTOList.add(userDTO);
            }
            invCountHeaderDTO.setSupervisorIdList(userDTOList);
        }
        return pageResult;
    }

    @Override
    public List<InvCountHeaderDTO> manualSave(List<InvCountHeaderDTO> invCountHeaderDTO) {
        // Checking manualSave
//        InvCountInfoDTO checkHeader = manualSaveCheck(invCountHeaderDTO);
//        if (checkHeader.getTotalErrorMsg() != null) {
//            throw new CommonException(checkHeader.getTotalErrorMsg());
//        }

        // Separate Header Insert dan Update
        List<InvCountHeader> insertList = invCountHeaderDTO.stream()
                .filter(line -> line.getCountHeaderId() == null)
                .collect(Collectors.toList());
        List<InvCountHeader> updateList = invCountHeaderDTO.stream()
                .filter(line -> line.getCountHeaderId() != null)
                .collect(Collectors.toList());

        // Insert Header Process
        processInsertHeaders(insertList);

        // Update Header Process
        processUpdateHeaders(updateList);

        return invCountHeaderDTO;
    }


    @Override
    public InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaderDTO) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();

        // Get The Data for Insert and Update
        List<InvCountHeaderDTO> insertList = invCountHeaderDTO.stream()
                .filter(header -> header.getCountHeaderId() == null)
                .collect(Collectors.toList());

        invCountInfoDTO.setSuccessList(insertList);

        List<InvCountHeaderDTO> updateList = invCountHeaderDTO.stream()
                .filter(header -> header.getCountHeaderId() != null)
                .collect(Collectors.toList());

        invCountInfoDTO.setSuccessList(updateList);

        // Get User Details
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long currentUser = userDetails.getUserId();
        StringBuilder errorMessage = new StringBuilder();

        // Validate the new Header
        for (InvCountHeader invCountHeader : updateList) {
            // get the newest Data
            InvCountHeader existingHeader = invCountHeaderRepository.selectByPrimary(invCountHeader.getCountHeaderId());

            // Validate header
            validation(invCountHeader);

            // Validate Current User
            if (!currentUser.equals(existingHeader.getCreatedBy()) && !invCountHeader.getCountStatus().equals("DRAFT")) {
                errorMessage.append("Document in draft status can only be modified by the document creator");
            }

            // Get List Supervisor ID
            List<String> supervisorIds = Arrays.asList(invCountHeader.getSupervisorIds().split(","));
            String[] counterId = invCountHeader.getCounterIds().split(",");

            // Get a data Warehouse
            InvWarehouse invWarehouse = invWarehouseRepository.selectByPrimaryKey(invCountHeader.getWarehouseId());

            // validate Status
            String[] validateStatus = {"INCOUNTING", "REJECTED", "WITHDRAWN"};
            if (invCountHeader.getCountStatus().equals(validateStatus[0]) ||
                    invCountHeader.getCountStatus().equals(validateStatus[1]) ||
                    invCountHeader.getCountStatus().equals(validateStatus[2])) {
                if (invWarehouse.getIsWmsWarehouse().equals(1) && supervisorIds.contains(currentUser.toString())) {
                    errorMessage.append("The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate.");
                }
                if (!invCountHeader.getCreatedBy().equals(currentUser) &&
                    !Arrays.asList(supervisorIds).contains(currentUser.toString()) &&
                    !Arrays.asList(counterId).contains(currentUser.toString())) {
                    errorMessage.append("Only the document creator, counter, and supervisor can modify the document for the status  of in counting, rejected, withdrawn.");
                }
            }

            // Get Data Line
            List<InvCountLine> linesToUpdate = invCountHeader.getInvCountLines();
            // Separate insert and update
            List<InvCountLine> linesToInsert = new ArrayList<>();
            List<InvCountLine> linesToUpdateExisting = new ArrayList<>();

            // Process for Insert
            if (linesToUpdate != null && !linesToUpdate.isEmpty()) {
                for (InvCountLine line : linesToUpdate) {
                    if (line.getCountLineId() != null) {
                        linesToUpdateExisting.add(line);
                    } else {
                        // If the line is new, set the countHeaderId and add it to the list for inserting
                        line.setCountHeaderId(invCountHeader.getCountHeaderId());
                        linesToInsert.add(line);
                    }
                }
            }

            // For Error
            invCountInfoDTO.setFailedList(invCountHeaderDTO);
            invCountInfoDTO.setTotalErrorMsg(errorMessage.toString());
            return invCountInfoDTO;
        }
        invCountInfoDTO.setSuccessList(invCountHeaderDTO);
        return invCountInfoDTO;
    }

    @Override
    public List<InvCountHeaderDTO> orderSave(List<InvCountHeaderDTO> invCountHeaders) {
        this.manualSaveCheck(invCountHeaders);
        this.manualSave(invCountHeaders);
        return invCountHeaders;
    }

    @Override
    public InvCountInfoDTO checkAndRemove(List<InvCountHeaderDTO> invCountHeaders) {
        // To Get Detail User
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long currentUser = userDetails.getUserId();

        List<InvCountHeaderDTO> failedList = new ArrayList<>();
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<String> errorMessages = new ArrayList<>();

        // Fetching Data by headerId
        Set<String> headerIds = invCountHeaders.stream()
                .map(header -> header.getCountHeaderId().toString())
                .collect(Collectors.toSet());

        // Fetching data for deletion
        List<InvCountHeader> fetchedUpdateInvCountHeaders =
                invCountHeaderRepository.selectByIds(String.join(", ", headerIds));

        for (InvCountHeader invCountHeader : fetchedUpdateInvCountHeaders) {
            // Status verification
            boolean isValid = true;
            if (!"DRAFT".equals(invCountHeader.getCountStatus())) {
                errorMessages.add("Only allow draft status to be deleted");
                isValid = false;
            }
            // Current user verification
            if (!currentUser.equals(invCountHeader.getCreatedBy())) {
                errorMessages.add("Only current user is document creator allow delete document");
                isValid = false;
            }
            if (!isValid) {
                InvCountHeaderDTO failedDTO = new InvCountHeaderDTO();
                failedDTO.setCountHeaderId(invCountHeader.getCountHeaderId());
                failedList.add(failedDTO);
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new CommonException(String.join(",", errorMessages));
        }

        // Delete data from repository
        for (InvCountHeaderDTO invCountHeader : invCountHeaders) {
            invCountHeaderRepository.deleteByPrimaryKey(invCountHeader.getCountHeaderId());
        }

        invCountInfoDTO.setSuccessList(invCountHeaders);
        invCountInfoDTO.setFailedList(failedList);
        invCountInfoDTO.setTotalErrorMsg(errorMessages.toString());
        return invCountInfoDTO;
    }

    @Override
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public InvCountHeaderDTO detail(Long countHeaderId) {
        // Get the Data
        InvCountHeaderDTO invCountHeaderDTO = invCountHeaderRepository.selectByPrimary(countHeaderId);

        // Get counterIds
        String[] counterIds = invCountHeaderDTO.getCounterIds().split(",");
        List<UserDTO> userDTOList = new ArrayList<>();
        for (String counterId : counterIds) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(Long.parseLong(counterId));
            userDTOList.add(userDTO);
        }
        invCountHeaderDTO.setCounterIdList(userDTOList);

        // Get supervisorIds
        String[] supervisorIds = invCountHeaderDTO.getSupervisorIds().split(",");
        List<UserDTO> supervisorIdList = new ArrayList<>();
        for (String supervisorId : supervisorIds) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(Long.parseLong(supervisorId));
            supervisorIdList.add(userDTO);
        }
        invCountHeaderDTO.setSupervisorIdList(supervisorIdList);

        // Get IsWMSWarehouse
        InvWarehouse invWarehouse = invWarehouseRepository.selectByPrimaryKey(invCountHeaderDTO.getWarehouseId());
        if (invWarehouse.getIsWmsWarehouse().equals(1)){
            invCountHeaderDTO.setIsWMSWarehouse(true);
        }else {
            invCountHeaderDTO.setIsWMSWarehouse(false);
        }

        // Get MaterialId and materialCode
        List<InvMaterial> invMaterial = invMaterialRepository.selectByIds(invCountHeaderDTO.getSnapshotMaterialIds());
        List<MaterialDTO> materialIdList = new ArrayList<>();
        invMaterial.forEach(invMaterial1 -> {
            MaterialDTO materialDTO = new MaterialDTO();
            materialDTO.setMaterialId(invMaterial1.getMaterialId());
            materialDTO.setMaterialCode(invMaterial1.getMaterialCode());
            materialIdList.add(materialDTO);
        });
        invCountHeaderDTO.setSnapshotMaterialList(materialIdList);

        // Get batchId and batchCode
        List<InvBatch> invBatches = invBatchRepository.selectByIds(invCountHeaderDTO.getSnapshotBatchIds());
        List<BatchDTO> batchIdList = new ArrayList<>();
        invBatches.forEach(invBatch1 -> {
            BatchDTO batchDTO = new BatchDTO();
            batchDTO.setBatchId(invBatch1.getBatchId());
            batchDTO.setBatchCode(invBatch1.getBatchCode());
            batchIdList.add(batchDTO);
        });
        invCountHeaderDTO.setSnapshotBatchList(batchIdList);

        // Set TenantAdminFlag, SnapshotMaterialList and SnapshotBatchList
        invCountHeaderDTO.setTenantAdminFlag(Boolean.TRUE.equals(invCountHeaderDTO.getTenantAdminFlag()));

        // For Comparing between CountLine and CountLineDTO
        InvCountLineDTO invCountLineDTO = (InvCountLineDTO) new InvCountLineDTO().setCountHeaderId(countHeaderId);
        List<InvCountLineDTO> invCountLineDTOList = invCountLineRepository.selectList(invCountLineDTO);
        invCountLineDTOList.sort(Comparator.comparing(InvCountLineDTO::getCountLineId));
        invCountHeaderDTO.setCountOrderLineList(invCountLineDTOList);

        return invCountHeaderDTO;
    }

    @Override
    public InvCountInfoDTO executeCheck(List<InvCountHeaderDTO> invCountHeaders) {
        // Create Array for successList, failedList and totalErrorMsg
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        List<InvCountHeaderDTO> failedList = new ArrayList<>();
        StringBuilder totalErrorMsg = new StringBuilder();
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();

        // Fetch Data
        List<InvCountHeaderDTO> headerDTO = invCountHeaders.stream()
                .filter(line -> line.getCountHeaderId() != null)
                .collect(Collectors.toList());

        // Loop through each header
        for (InvCountHeaderDTO header : headerDTO){
            boolean hasError = false;
            StringBuilder errorMsg = new StringBuilder();

            // 1. Count Status Validation
            if (!"DRAFT".equalsIgnoreCase(header.getCountStatus())){
                errorMsg.append("Only draft status can execute");
                hasError = true;
            }

            // 2. Current User Validation
            CustomUserDetails userDetails = DetailsHelper.getUserDetails();
            Long currentUser = userDetails.getUserId();
            if (!currentUser.equals(header.getCreatedBy())){
                errorMsg.append("Only the document creator can execute");
                hasError = true;
            }

            // 3. Company, Warehouse, Department Validation
            // Company Validation
            if (ObjectUtils.isEmpty(iamCompanyRepository.selectByPrimary(header.getCompanyId()))) {
               errorMsg.append("There is no company with ID " + header.getCompanyId());
               hasError = true;
            }
            // Department Validation
            if (ObjectUtils.isEmpty(iamDepartmentRepository.selectByPrimary(header.getDepartmentId()))){
                errorMsg.append("There is no department with ID " + header.getDepartmentId());
                hasError = true;
            }
            // Warehouse Validation
            if (ObjectUtils.isEmpty(invWarehouseRepository.selectByPrimary(header.getWarehouseId()))){
                errorMsg.append("There is no warehouse with ID " + header.getWarehouseId());
                hasError = true;
            }

            // 4. Validation on Hand Quantity Validation
            if (header.getStockList() != null) {
                try {
                    Condition condition = new Condition(InvStock.class);
                    condition.createCriteria().andEqualTo(InvStock.FIELD_TENANT_ID, header.getTenantId());
                    condition.and().andEqualTo(InvStock.FIELD_COMPANY_ID, header.getCompanyId());
                    condition.and().andEqualTo(InvStock.FIELD_DEPARTMENT_ID, header.getDepartmentId());
                    condition.and().andEqualTo(InvStock.FIELD_WAREHOUSE_ID, header.getWarehouseId());
                    condition.and().andEqualTo("snapshotMaterialIds", header.getSnapshotMaterialIds());
                    condition.and().andEqualTo("snapshotBatchIds", header.getSnapshotBatchIds());
                    condition.and().andGreaterThan(InvStock.FIELD_AVAILABLE_QUANTITY, 0);

                    invStockRepository.selectByCondition(condition);
                } catch (Exception e) {
                    errorMsg.append("Unable to query on hand quantity data" + e.getMessage());
                    hasError = true;
                }
            }

            if (header.getCountType().equals("MONTH")) {
                if (!header.getCountTimeStr().matches("\\d{4}-\\d{2}")) {
                    errorMsg.append("For countType MONTH, limit format must be yyyy-mm");
                    hasError = true;
                }
            }

            if (header.getCountType().equals("YEAR")) {
                if (!header.getCountTimeStr().matches("\\d{4}")) {
                    errorMsg.append("For countType YEAR, limit format must be yyyy");
                    hasError = true;
                }
            }

            // Collect Error
            if (hasError) {
                header.setErrorMsg(errorMsg.toString());
                failedList.add(header);
                totalErrorMsg.append(errorMsg).append("\n");
            } else {
                successList.add(header);
            }
        }
        invCountInfoDTO.setFailedList(failedList);
        invCountInfoDTO.setTotalErrorMsg(totalErrorMsg.toString());
        invCountInfoDTO.setSuccessList(successList);

        return invCountInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaderDTO){
        // Get Data Header
        List<InvCountHeaderDTO> headerDTO = invCountHeaderDTO.stream()
             .filter(line -> line.getCountHeaderId() != null)
             .collect(Collectors.toList());

        // To get User Details
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long tenantId = userDetails.getTenantId();

        ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();

        // TODO jika tidak perlu dihapus aja
        // Format Date from String to Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try{
            // To get Employee Details
            UserVO userVO = objectMapper.readValue(stringResponseEntity.getBody(), UserVO.class);
            for (InvCountHeaderDTO header : headerDTO) {
                 header.setCountStatus("INCOUNTING");
                 header.setEmployeeNumber(userVO.getLoginName());
                 header.setTenantId(tenantId);

                 // To get InvStock
                 List<InvStockDTO> invStock = getInvStock(header);

                 // Insert Line
                 List<InvCountLine> lineList = new ArrayList<>();
                 int lineNumber = 1;
                 for (InvStockDTO stock : invStock){
                    InvCountLine line = new InvCountLine();
                    line.setTenantId(tenantId);
                    line.setCountHeaderId(header.getCountHeaderId());
                    line.setCounterIds(header.getCounterIds());
                    line.setLineNumber(lineNumber);

                    line.setBatchId(stock.getBatchId());
                    line.setMaterialId(stock.getMaterialId());
                    line.setSnapshotUnitQty(stock.getSummary());
                    line.setUnitCode(stock.getUnitCode());
                    line.setWarehouseId(stock.getWarehouseId());
                    lineNumber++;

                    lineList.add(line);
                }
                invCountLineRepository.batchInsertSelective(lineList);

                invCountHeaderRepository.updateOptional(header,
                         InvCountHeader.FIELD_COUNT_STATUS,
                         InvCountHeader.FIELD_TENANT_ID);
            }
        } catch (Exception e){
            throw new CommonException("Error processing headers: " + e.getMessage());
        }
    return headerDTO;
    }

    @Override
    public List<InvCountHeaderDTO> orderExecution(List<InvCountHeaderDTO> invCountHeaders) {
        // TODO untuk yang check karena return nya inCountInfo harus di consider

        this.manualSaveCheck(invCountHeaders).getSuccessList();
        this.manualSave(invCountHeaders);
        this.executeCheck(invCountHeaders).getSuccessList();
        this.execute(invCountHeaders);
        this.countSyncWms(invCountHeaders);
        return invCountHeaders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfo = new InvCountInfoDTO();

        List<InvCountHeaderDTO> successList = new ArrayList<>();
        List<InvCountHeaderDTO> failedList = new ArrayList<>();
        StringBuilder totalErrorMsg = new StringBuilder();

        // Loop through each header
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            StringBuilder errorMessage = new StringBuilder();

            //check and set warehouse
            InvWarehouse invWarehouse = new InvWarehouse();
            invWarehouse.setTenantId(invCountHeaderDTO.getTenantId());
            invWarehouse.setWarehouseId(invCountHeaderDTO.getWarehouseId());

            // a. Determine whether the warehouse data exists by tenantId and warehouseCode
            List<InvWarehouse> invWarehouseList = invWarehouseRepository.selectList(invWarehouse);
            if (CollectionUtils.isEmpty(invWarehouseList)) {
                failedList.add(invCountHeaderDTO);
                totalErrorMsg.append(errorMessage).append("\n");
            }

            Condition condition = new Condition(InvCountExtra.class);
            condition.createCriteria().andEqualTo("sourceId", invCountHeaderDTO.getCountHeaderId());

            // b. Get the extended table data based on the counting header ID
            List<InvCountExtra> existingExtra = invCountExtraRepository.selectByCondition(condition);

            // Fetch Data for syncStatusExtra
            InvCountExtra syncStatusExtra = existingExtra.stream()
                    .filter(extra -> "wms_sync_status".equals(extra.getProgramKey()))
                    .findFirst()
                    .orElseGet(() -> createInvCountExtra(invCountHeaderDTO, "wms_sync_status"));

            // Fetch Data for syncMsgExtra
            InvCountExtra syncMsgExtra = existingExtra.stream()
                    .filter(extra -> "wms_sync_error_message".equals(extra.getProgramKey()))
                    .findFirst()
                    .orElseGet(() -> createInvCountExtra(invCountHeaderDTO, "wms_sync_error_message"));

            if (!CollectionUtils.isEmpty(invWarehouseList)) {
                //get WMS Warehouse status
                int isWmsWarehouse = invWarehouseList.stream()
                        .filter(value -> value.getWarehouseId().equals(invCountHeaderDTO.getWarehouseId())) // Filter by warehouseID
                        .mapToInt(InvWarehouse::getIsWmsWarehouse) // Map to the isWmsWarehouse (int value)
                        .findFirst()
                        .orElse(0);

                ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
                ObjectMapper objectMapper = new ObjectMapper();

                // Change Variable to Date from String
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                objectMapper.setDateFormat(dateFormat);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // WMS Sync
                if (isWmsWarehouse == 1) {
                    try {
                        UserVO userVO = objectMapper.readValue(stringResponseEntity.getBody(), UserVO.class);
                        invCountHeaderDTO.setEmployeeNumber(userVO.getLoginName());
                        invCountHeaderDTO.setTenantId(userVO.getTenantId());
                        String invCountHeaderString = objectMapper.writeValueAsString(invCountHeaderDTO);
                        //call interface [S]
                        ResponsePayloadDTO responsePayloadDTO = utils.invokeInterface(
                                "HZERO",
                                "FEXAM_WMS",
                                "fexam-wms-api.thirdAddCounting",
                                invCountHeaderString,
                                MediaType.APPLICATION_JSON);
                        Map<String, String> response = objectMapper.readValue(responsePayloadDTO.getPayload(), Map.class);

                        // The Logic for Success and Failed
                        if (response.get("returnStatus").equals("S")) {
                            syncStatusExtra.setProgramValue("SUCCESS");
                            syncMsgExtra.setProgramValue("");
                            invCountHeaderDTO.setRelatedWmsOrderCode(responsePayloadDTO.getPayload());

                            successList.add(invCountHeaderDTO);
                        } else  {
                            syncStatusExtra.setProgramValue("ERROR");
                            syncMsgExtra.setProgramValue(response.get("returnMsg"));

                            errorMessage.append(response.get("returnMsg"));
                            failedList.add(invCountHeaderDTO);
                            totalErrorMsg.append(errorMessage);
                        }
                    } catch (Exception e) {
                        syncStatusExtra.setProgramValue("FAILED");
                        syncMsgExtra.setProgramValue(e.getMessage());
                        errorMessage.append("Error during WMS sync: " + e.getMessage());
                        failedList.add(invCountHeaderDTO);
                        totalErrorMsg.append(errorMessage);
                    }
                } else {
                    // For Skip
                    syncStatusExtra.setProgramValue("SKIP");
                    syncMsgExtra.setProgramValue("");

                    successList.add(invCountHeaderDTO);
                }

                // Insert or update sync status extra
                if (syncStatusExtra.getExtraInfoId() == null) {
                    invCountExtraRepository.insertSelective(syncStatusExtra);
                    invCountExtraRepository.insertSelective(syncMsgExtra);
                } else {
                    invCountExtraRepository.updateOptional(syncStatusExtra,
                            InvCountExtra.FIELD_PROGRAM_VALUE);
                    invCountExtraRepository.updateOptional(syncMsgExtra,
                            InvCountExtra.FIELD_PROGRAM_VALUE);
                }
            }
        }

        invCountInfo.setFailedList(failedList);
        invCountInfo.setTotalErrorMsg(String.valueOf(totalErrorMsg));
        invCountInfo.setSuccessList(successList);

        return invCountInfo;
    }

    @Override
    public InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeader) {
        String errorMessage = "";
        String status = "S";

        // Set TenantId and WarehouseId in InvWarehouse
        InvWarehouse invWarehouse = new InvWarehouse();
        invWarehouse.setTenantId(invCountHeader.getTenantId());
        invWarehouse.setWarehouseId(invCountHeader.getWarehouseId());
        List<InvWarehouse> invWarehouseList = invWarehouseRepository.selectList(invWarehouse);

        // Get isWMSWarehouse
        int isWmsWarehouse = invWarehouseList.stream()
                .filter(value -> Objects.equals(value.getWarehouseId(), invCountHeader.getWarehouseId())) // Filter by warehouseID
                .mapToInt(InvWarehouse::getIsWmsWarehouse) // Map to the isWmsWarehouse (int value)
                .findFirst()
                .orElse(0);

        if (isWmsWarehouse == 0){
            errorMessage = errorMessage + "The current warehouse is not a WMS warehouse, operations are not allowed";
            status = "E";
        }

        // Create condition for line headerId and header headerId is equal
        Condition condition = new Condition(InvCountLine.class);
        condition.createCriteria().andEqualTo(InvCountLine.FIELD_COUNT_HEADER_ID, invCountHeader.getCountHeaderId());
        int lines = invCountLineRepository.selectCountByCondition(condition);
        if (invCountHeader.getCountOrderLineList().size() != lines){
            errorMessage = errorMessage + "The counting order line data is inconsistent with the INV system, please check the data";
            status = "E";
        }

        // Logic
        if (status.equals("S")){
            invCountHeader.setStatus(status);
            invCountHeader.setErrorMsg(errorMessage);

            // Update Line
            List<InvCountLine> invCountLineList = new ArrayList<>();
            for (InvCountLineDTO invCountLineDTO : invCountHeader.getCountOrderLineList()) {

                // Create Criteria to get objectVersionNumber
                Criteria criteria = new Criteria();
                criteria.select(InvCountLine.FIELD_OBJECT_VERSION_NUMBER);

                // To set objectVersionNumber in InvCountLine
                Long versionNumber = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId()).getObjectVersionNumber();
                invCountLineDTO.setObjectVersionNumber(versionNumber);

                // To get invCountLine
                InvCountLine line = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId());

                // Set the UnitDiffQty
                invCountLineDTO.setUnitDiffQty(invCountLineDTO.getUnitQty().subtract(invCountLineDTO.getSnapshotUnitQty()));

                // To Update the CounterIds
                invCountLineDTO.setCounterIds(line.getLastUpdatedBy().toString());

                // To copy properties between InvCountLineDTO and InvCountLine
                InvCountLine invCountLine = new InvCountLine();
                BeanUtils.copyProperties(invCountLineDTO, invCountLine);
                invCountLineList.add(invCountLine);
            }
            // For Update
            invCountLineRepository.batchUpdateByPrimaryKeySelective(invCountLineList);
        }else {
            invCountHeader.setStatus(status);
            invCountHeader.setErrorMsg(errorMessage);
        }
        return invCountHeader;
    }

    @Override
    public InvCountInfoDTO submitCheck(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfo = new InvCountInfoDTO();

        // 1. Check document status
        for (InvCountHeaderDTO invCountHeader : invCountHeaders) {
            StringBuilder errorMessage = new StringBuilder();
            String[] validStatus = {"PROCESSING", "INCOUNTING", "REJECTED", "WITHDRAWN"};
            String status = invCountHeader.getCountStatus();
            if (!Arrays.asList(validStatus).contains(status)) {
                invCountInfo.setFailedList(invCountHeaders);
                errorMessage.append("The operation is allowed only when the status in in counting, processing, rejected, withdrawn.");
            }

            // To Collect supervisor
            List<Long> supervisorList = Arrays.stream(invCountHeader.getSupervisorIds().split(","))
                            .map(Long::parseLong)
                            .collect(Collectors.toList());

            boolean isSupervisor = false;

            // To Collect UserId by DetailsHelper
            Long currentUserId = DetailsHelper.getUserDetails().getUserId();

            // 2. current login user validation
                if (supervisorList.contains(currentUserId)){
                    isSupervisor = true;
                }
                if (!isSupervisor){
                    invCountInfo.setFailedList(invCountHeaders);
                    errorMessage.append("Only the current login user is the supervisor can submit document.");
                }

                // 3. Data integrity check
                for (InvCountLineDTO invCountLineDTO : invCountHeader.getCountOrderLineList()) {
                    if (invCountLineDTO.getUnitQty() == null ||
                        invCountLineDTO.getSnapshotUnitQty() == null ||
                        invCountLineDTO.getUnitDiffQty() == null){
                            invCountInfo.setFailedList(invCountHeaders);
                            errorMessage.append("There are data rows with empty count quantity. Please check the data.");
                    }

                    // To get invCountLine
                    InvCountLineDTO invCountLine = new InvCountLineDTO();
                    InvCountLine line = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId());

                    // Copy Properties invCountLine to invCountLineDTO
                    BeanUtils.copyProperties(line, invCountLine);

                    // Logic for check the unit quantity
                    if (!invCountLine.getUnitQty().equals(invCountLineDTO.getUnitQty()) && StringUtils.isEmpty(invCountHeader.getReason())){
                        invCountInfo.setFailedList(invCountHeaders);
                        errorMessage.append("there is a difference in counting");
                    }
                }

                // To collect error message
                if (errorMessage.length() > 0){
                    invCountInfo.setFailedList(invCountHeaders);
                    invCountInfo.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
                    return invCountInfo;
                }
            }
        invCountInfo.setSuccessList(invCountHeaders);
        return invCountInfo;
    }

    @Override
    public List<InvCountHeaderDTO> submit(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfo = submitCheck(invCountHeaders);
        if (!CollectionUtils.isEmpty(invCountInfo.getFailedList())){
            throw new CommonException(invCountInfo.getTotalErrorMsg());
        }

        // Start Workflow
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            String workflow = profileClient.getProfileValueByOptions(invCountHeaderDTO.getTenantId(),
                    null,
                    null,
                    Constants.PROFILE_NAME);
            if (workflow.equals("1")){

                // To get the User by DetailsHelper
                Map<String, Object> variableMap = new HashMap<>();
                String starter = Constants.WORKFLOW_DEFAULT_DIMENSION.equals("USER") ?
                        String.valueOf(DetailsHelper.getUserDetails().getUserId()) :
                        DetailsHelper.getUserDetails().getUsername();

                // To put the Department Code
                variableMap.put("departmentCode", iamDepartmentRepository.selectByPrimary(invCountHeaderDTO.getDepartmentId()).getDepartmentCode());

                // To set the RunInstance by FlowKey
                RunInstance workFlowStart = workflowClient.startInstanceByFlowKey(invCountHeaderDTO.getTenantId(),
                        Constants.FLOW_KEY,
                        invCountHeaderDTO.getCountNumber(),
                        Constants.WORKFLOW_DEFAULT_DIMENSION,
                        starter,
                        variableMap);

                // Copy Properties from invCountHeaderDTO to invCountHeader
                InvCountHeader invCountHeader = new InvCountHeader();
                BeanUtils.copyProperties(invCountHeaderDTO, invCountHeader);

                // To update the invCountHeader
                invCountHeaderRepository.updateOptional(invCountHeader, InvCountHeader.FIELD_REASON);
            } else {
                String status = "CONFIRMED";
                invCountHeaderDTO.setCountStatus(status);
                InvCountHeader invCountHeader = new InvCountHeader();

                // Copy Properties from invCountHeaderDTO to invCountHeader
                BeanUtils.copyProperties(invCountHeaderDTO, invCountHeader);

                // To update the invCountHeader
                invCountHeaderRepository.updateOptional(invCountHeader,
                        InvCountHeader.FIELD_COUNT_STATUS,
                        InvCountHeader.FIELD_REASON);
            }
        }
        return invCountHeaders;
    }

    @Override
    public void callBack(Long organizationId, WorkFlowDTO workFlowDTO) {

        InvCountHeader header = new InvCountHeader();

        // To set Count Number and Tenant ID
        header.setCountNumber(workFlowDTO.getBusinessKey());
        header.setTenantId(organizationId);

        // To get InvCountHeader with condition in header
        InvCountHeader invCountHeader = invCountHeaderRepository.selectOne(header);

        // To update countStatus, workFlowId and approvedTime
        invCountHeader.setCountStatus(workFlowDTO.getDocStatus());
        invCountHeader.setWorkflowId(workFlowDTO.getWorkflowId());
        invCountHeader.setApprovedTime(workFlowDTO.getApprovedTime());

        invCountHeaderRepository.updateByPrimaryKeySelective(invCountHeader);
    }

    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeaderDTO) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Set a custom date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. Build the query condition
        Condition condition = new Condition(InvCountHeader.class);
        condition.createCriteria().andEqualTo(InvCountHeader.FIELD_COUNT_HEADER_ID, invCountHeaderDTO.getCountHeaderId());

        if (invCountHeaderDTO.getCountStatus() != null) {
            condition.and().andIn(InvCountHeader.FIELD_COUNT_STATUS, Collections.singleton(invCountHeaderDTO.getCountStatus()));
        }
        if (invCountHeaderDTO.getCountType() != null) {
            condition.and().andEqualTo(InvCountHeader.FIELD_COUNT_TYPE, invCountHeaderDTO.getCountType());
        }
        if (invCountHeaderDTO.getCountDimension() != null) {
            condition.and().andEqualTo(InvCountHeader.FIELD_COUNT_DIMENSION, invCountHeaderDTO.getCountDimension());
        }
        if (invCountHeaderDTO.getCountMode() != null) {
            condition.and().andEqualTo(InvCountHeader.FIELD_COUNT_MODE, invCountHeaderDTO.getCountMode());
        }

        // 4. Fetch data from the database
        List<InvCountHeader> dataInvCountHeader = invCountHeaderRepository.selectByCondition(condition);

        IamDepartment department = iamDepartmentRepository.selectByPrimary(invCountHeaderDTO.getDepartmentId());

        // 5. Process the data and set the lines
        List<InvCountHeaderDTO> result = null;
        for (InvCountHeader header : dataInvCountHeader) {
            result = new ArrayList<>();

            // Create InvCountLineDTO for query
            InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
            invCountLineDTO.setCountHeaderId(header.getCountHeaderId());

            // Fetch InvCountLineDTO data from repository
            List<InvCountLineDTO> invCountLineDTOList = invCountLineRepository.selectList(invCountLineDTO);
            invCountLineDTOList.sort(Comparator.comparing(InvCountLineDTO::getCountLineId));
            InvCountHeaderDTO dto = invCountHeaderRepository.selectByPrimary(header.getCountHeaderId());

            // Get counterIds
            String[] counterIds = dto.getCounterIds().split(",");
            List<UserDTO> userDTOList = new ArrayList<>();
            for (String counterId : counterIds) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(Long.parseLong(counterId));
                userDTOList.add(userDTO);
            }
            dto.setCounterIdList(userDTOList);

            // Get supervisorIds
            String[] supervisorIds = dto.getSupervisorIds().split(",");
            List<UserDTO> supervisorIdList = new ArrayList<>();
            for (String supervisorId : supervisorIds) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(Long.parseLong(supervisorId));
                supervisorIdList.add(userDTO);
            }
            dto.setSupervisorIdList(supervisorIdList);

            // Get department name
            dto.setDepartmentName(department.getDepartmentName());

            // Get Material Code
            List<InvMaterial> invMaterials = invMaterialRepository.selectByIds(dto.getSnapshotMaterialIds());
            List<MaterialDTO> materialIdList = new ArrayList<>();
            invMaterials.forEach(invMaterials1 -> {
                MaterialDTO materialDTO = new MaterialDTO();
                materialDTO.setMaterialId(invMaterials1.getMaterialId());
                materialDTO.setMaterialCode(invMaterials1.getMaterialCode());
                materialIdList.add(materialDTO);
            });
            dto.setSnapshotMaterialList(materialIdList);

            // Get Batch Code
            List<InvBatch> invBatches = invBatchRepository.selectByIds(dto.getSnapshotBatchIds());
            List<BatchDTO> batchIdList = new ArrayList<>();
            invBatches.forEach(invBatches1 -> {
                BatchDTO batchDTO = new BatchDTO();
                batchDTO.setBatchId(invBatches1.getBatchId());
                batchDTO.setBatchCode(invBatches1.getBatchCode());
                batchIdList.add(batchDTO);
            });
            dto.setSnapshotBatchList(batchIdList);

            // Get History Approval
            List<RunTaskHistory> historyList = new ArrayList<>();
            try {
                historyList = workflowClient.approveHistoryByFlowKey(header.getTenantId(),
                        Constants.FLOW_KEY, header.getCountNumber());
            }catch (Exception e) {
                historyList = null;
            }
            dto.setHistory(historyList);

            dto.setCountOrderLineList(invCountLineDTOList);
            result.add(dto);
        }
        return result;
    }

    private List<InvStockDTO> getInvStock(InvCountHeaderDTO header) {

        // Set the InvStock
        InvStockDTO invStock = new InvStockDTO();
        invStock.setTenantId(header.getTenantId());
        invStock.setDepartmentId(header.getDepartmentId());
        invStock.setCompanyId(header.getCompanyId());
        invStock.setWarehouseId(header.getWarehouseId());

        // Get Dimension
        String dimension = header.getCountDimension();

        // TODO : bukan seperti ini, dibaca kembali di document
        if (dimension.equals("SKU")){
            // get Data Material ID and change it to Long
            invStock.setMaterialIdLongList(Arrays.stream(header.getSnapshotMaterialIds()
                    .split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));

        } else if (dimension.equals("LOT")) {
            // get Data Material ID and change it to Long
            invStock.setMaterialIdLongList(Arrays.stream(header.getSnapshotMaterialIds()
                    .split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));

            // get Data Batch ID and change it to Long
            invStock.setBatchIdLongList(Arrays.stream(header.getSnapshotBatchIds()
                    .split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }
        return invStockRepository.selectSummary(invStock);
    }

    private void validation(InvCountHeader invCountHeader){
        if (!isValidStatus(invCountHeader.getCountStatus())){
            throw new CommonException(
                    "Only draft, in counting, rejected, and withdrawn status can be modified");
        }
    }

    private boolean isValidStatus(String status) {
        return "DRAFT".equals(status) || "INCOUNTING".equals(status) || "REJECTED".equals(status) || "WITHDRAWN".equals(status);
    }

    // Method for processing lines
    private void processLines(List<InvCountLine> lines, Long countHeaderId) {
        List<InvCountLine> linesToInsert = new ArrayList<>();
        List<InvCountLine> linesToUpdate = new ArrayList<>();

        for (InvCountLine line : lines) {
            if (line.getCountLineId() != null) {
                linesToUpdate.add(line);
            } else {
                line.setCountHeaderId(countHeaderId);
                linesToInsert.add(line);
            }
        }
        if (!linesToUpdate.isEmpty()) {
            invCountLineRepository.batchUpdateByPrimaryKeySelective(linesToUpdate);
        }
        if (!linesToInsert.isEmpty()) {
            invCountLineRepository.batchInsertSelective(linesToInsert);
        }
    }

    // Method for insert headers
    private void processInsertHeaders(List<InvCountHeader> insertList) {
        for (InvCountHeader header : insertList) {
            Map<String, String> codeBuilderMap = new HashMap<>();
            Long tenantId = header.getTenantId();
            codeBuilderMap.put("customSegment", tenantId.toString());

            String countNumber = codeRuleBuilder.generateCode(Constants.CODE_RULE_COUNT_NUMBER, codeBuilderMap);
            header.setCountNumber(countNumber);
            header.setDelFlag(0);
            header.setCountStatus("DRAFT");
        }
        invCountHeaderRepository.batchInsertSelective(insertList);
    }

    // Method for update headers dan lines
    private void processUpdateHeaders(List<InvCountHeader> updateList) {
        StringBuilder errorMessage = new StringBuilder();
        InvCountInfoDTO invCountInfo = new InvCountInfoDTO();

        for (InvCountHeader header : updateList) {
            if (!header.getCountStatus().equals("DRAFT")) {
                errorMessage.append("Only draft status allows updates");
            }
        }
        invCountInfo.setTotalErrorMsg(errorMessage.toString());

        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    // Method for Stock Validation
    private void stockValidation(List<InvStock> invStock) {
        List<InvStock> stockList = invStock.stream()
                .filter(line -> line.getStockId() != null)
                .collect(Collectors.toList());
        for (InvStock stock : stockList) {
            if (stock.getAvailableQuantity() == null ||
                    stock.getAvailableQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("Unable to query on hand quantity data");
            }
        }
    }

    private InvCountExtra createInvCountExtra(InvCountHeaderDTO invCountHeaderDTO, String programKey) {
        InvCountExtra invCountExtra = new InvCountExtra();
        invCountExtra.setSourceId(invCountHeaderDTO.getCountHeaderId());
        invCountExtra.setTenantId(invCountHeaderDTO.getTenantId());
        invCountExtra.setEnabledFlag(1);
        invCountExtra.setProgramKey(programKey);
        invCountExtra.setProgramValue("");
        return invCountExtra;
    }
}
