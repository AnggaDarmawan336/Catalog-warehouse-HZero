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
import com.hand.demo.infra.mapper.InvMaterialMapper;
import com.hand.demo.infra.mapper.InvStockMapper;
import com.hand.demo.infra.mapper.InvWarehouseMapper;
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
import org.hzero.boot.platform.profile.ProfileClient;
import org.hzero.boot.workflow.WorkflowClient;
import org.hzero.boot.workflow.dto.RunInstance;
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

    private static Logger logger = Logger.getLogger(InvCountHeaderServiceImpl.class.getName());

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
    RedisTemplate redisTemplate;

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
        if (Boolean.TRUE.equals(userVO.getTenantAdminFlag())) {
            invCountHeader.setTenantId(userVO.getTenantId());
        } else {
            if (userVO.getId() != null) {
                invCountHeader.setCreatedBy(userVO.getId());
            } else {
                return PageHelper.doPageAndSort(pageRequest, ArrayList::new);
            }
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
    public List<InvCountHeaderDTO> orderSave(List<InvCountHeaderDTO> invCountHeaderDTO) {
        // Checking manualSave
        InvCountInfoDTO checkHeader = manualSaveCheck(invCountHeaderDTO);
        if (checkHeader.getTotalErrorMsg()!= null) {
            throw new CommonException(checkHeader.getTotalErrorMsg());
        }

        // Separate Header Insert dan Update
        List<InvCountHeader> insertList = invCountHeaderDTO.stream()
                .filter(line -> line.getCountHeaderId() == null)
                .collect(Collectors.toList());
        List<InvCountHeader> updateList = invCountHeaderDTO.stream()
                .filter(line -> line.getCountHeaderId() != null)
                .collect(Collectors.toList());

        // Insert Header Process
        processInsertHeaders(insertList);

        // Update Header dan Line Process
        processUpdateHeaders(updateList);

        return invCountHeaderDTO;
    }


    @Override
    public InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaderDTO) {

        // Get The Data for Insert and Update
        List<InvCountHeaderDTO> insertList = invCountHeaderDTO.stream()
                .filter(header -> header.getCountHeaderId() == null)
                .collect(Collectors.toList());
        List<InvCountHeaderDTO> updateList = invCountHeaderDTO.stream()
                .filter(header -> header.getCountHeaderId() != null)
                .collect(Collectors.toList());

        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();

        // Get User Details
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long currentUser = userDetails.getUserId();
        StringBuilder errorMessage = new StringBuilder();

        // For the Insert
        for (InvCountHeader invCountHeader : insertList) {
            if (invCountHeader.getCountHeaderId() == null) {
                return invCountInfoDTO;
            }
        }

        // Validate the new Header
        for (InvCountHeader invCountHeader : updateList) {
            // get the newest Data
            InvCountHeader existingHeader = invCountHeaderRepository.selectByPrimary(invCountHeader.getCountHeaderId());
            // Validate Current User
            if (!currentUser.equals(existingHeader.getCreatedBy())) {
                throw new CommonException("You can only update your own data.");
            }
            // Validate header
            validation(invCountHeader);
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
                    errorMessage.append("verification failed, the error message is: The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate.");
                }
                if (!invCountHeader.getCreatedBy().equals(currentUser) &&
                    !Arrays.asList(supervisorIds).contains(currentUser.toString()) &&
                    !Arrays.asList(counterId).contains(currentUser.toString())) {
                    errorMessage.append("verification failed, the error message is: only the document creator, counter, and supervisor can modify the document for the status  of in counting, rejected, withdrawn.");
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
//            invCountInfoDTO.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
            return invCountInfoDTO;
        }
        invCountInfoDTO.setSuccessList(invCountHeaderDTO);
        return invCountInfoDTO;
    }

    @Override
    public InvCountInfoDTO checkAndRemove(List<InvCountHeaderDTO> invCountHeaders) {
        // To Get Detail User
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long currentUser = userDetails.getUserId();

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
            if (!"DRAFT".equals(invCountHeader.getCountStatus())) {
                errorMessages.add("Document with ID " + invCountHeader.getCountHeaderId() + " cannot be deleted because its status is not draft.");
            }
            // Current user verification
            if (!currentUser.equals(invCountHeader.getCreatedBy())) {
                errorMessages.add("Document with ID " + invCountHeader.getCountHeaderId() + " cannot be deleted because the current user is not the creator.");
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalArgumentException(String.join(",", errorMessages));
        }

        // Delete data from repository
        for (InvCountHeaderDTO invCountHeader : invCountHeaders) {
            invCountHeaderRepository.deleteByPrimaryKey(invCountHeader.getCountHeaderId());
        }

        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        invCountInfoDTO.setTotalErrorMsg("Documents deleted successfully.");
        return invCountInfoDTO;
    }

    @Override
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

        // Set TenantAdminFlag, SnapshotMaterialList and SnapshotBatchList
        invCountHeaderDTO.setTenantAdminFlag(Boolean.TRUE.equals(invCountHeaderDTO.getTenantAdminFlag()));
        invCountHeaderDTO.setSnapshotMaterialList(invMaterialRepository.selectByIds(invCountHeaderDTO.getSnapshotMaterialIds()));
        invCountHeaderDTO.setSnapshotBatchList(invBatchRepository.selectByIds(invCountHeaderDTO.getSnapshotBatchIds()));

        // For Comparing between CountLine and CountLineDTO
        InvCountLineDTO invCountLineDTO = (InvCountLineDTO) new InvCountLineDTO().setCountHeaderId(countHeaderId);
        List<InvCountLineDTO> invCountLineDTOList = invCountLineRepository.selectList(invCountLineDTO);
        invCountLineDTOList.sort(Comparator.comparing(InvCountLineDTO::getCountLineId));
        invCountHeaderDTO.setCountOrderLineList(invCountLineDTOList);

        return invCountHeaderDTO;
    }

    @Override
    public List<InvCountHeaderDTO> executeCheck(List<InvCountHeaderDTO> invCountHeaders) {
        // Create Array for successList, failedList and totalErrorMsg
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        List<InvCountHeaderDTO> failedList = new ArrayList<>();
        StringBuilder totalErrorMsg = new StringBuilder();

        // Loop through each header
        for (InvCountHeaderDTO header : invCountHeaders){
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
                InvStock invStock = new InvStock();
                invStock.setTenantId(header.getTenantId());
                invStock.setCompanyId(header.getCompanyId());
                invStock.setDepartmentId(header.getDepartmentId());
                invStock.setWarehouseId(header.getWarehouseId());
                invStock.setMaterialIds(header.getSnapshotMaterialIds());
                invStock.setBatchIds(header.getSnapshotBatchIds());
                stockValidation(header.getStockList());
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
        return successList;
    }

    @Override
    @Transactional
    public List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaderDTO){

        // Execute Check
        List<InvCountHeaderDTO> executeCheck = executeCheck(invCountHeaderDTO);
        if(executeCheck==null){
            throw new CommonException("Invalid");
        }

        // Get Data Header
        List<InvCountHeaderDTO> headerDTO = invCountHeaderDTO.stream()
             .filter(line -> line.getCountHeaderId() != null)
             .collect(Collectors.toList());

        // To get User Details
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long tenantId = userDetails.getTenantId();

        ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();

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
                 InvStock invStock = getInvStock(header);

                 int lineNumber = 1;

                 // Update Line
                 if (header.getInvCountLines() != null){
                     for (InvCountLine line : header.getInvCountLines()){
                         line.setTenantId(tenantId);
                         line.setCountHeaderId(header.getCountHeaderId());
                         line.setCounterIds(header.getCounterIds());
                         line.setLineNumber(lineNumber);

                         line.setBatchId(invStock.getBatchId());
                         line.setMaterialId(invStock.getMaterialId());
                         line.setSnapshotUnitQty(invStock.getAvailableQuantity());
                         line.setUnitCode(invStock.getUnitCode());
                         line.setWarehouseId(invStock.getWarehouseId());
                         line.setMaterialId(invStock.getMaterialId());
                         lineNumber++;
                     }
                     processLines(header.getInvCountLines(), header.getCountHeaderId());
                 }
                 invCountHeaderRepository.updateByPrimaryKeySelective(header);
            }

        } catch (Exception e){
            throw new CommonException(e);
        }
        // For Synchronization WMS
        InvCountInfoDTO orderSynchronization = countSyncWms(headerDTO);
        if (orderSynchronization.getFailedList() != null && !orderSynchronization.getFailedList().isEmpty()) {
            throw new CommonException("WMS synchronization failed: " + orderSynchronization.getTotalErrorMsg());
        }
    return headerDTO;
    }

    @Override
    public InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfo = new InvCountInfoDTO();

        // Loop through each header
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            String errorMessage = "";
            //check and set warehouse
            InvWarehouse invWarehouse = new InvWarehouse();
            invWarehouse.setTenantId(invCountHeaderDTO.getTenantId());
            invWarehouse.setWarehouseId(invCountHeaderDTO.getWarehouseId());
            List<InvWarehouse> invWarehouseList = invWarehouseRepository.selectList(invWarehouse);
            if (CollectionUtils.isEmpty(invWarehouseList)) {
                errorMessage = errorMessage + "Warehouse validation: warehouse not found !" + ",";
            }
            // Set extra
            InvCountExtra invCountExtra = new InvCountExtra();
            invCountExtra.setSourceId(invCountHeaderDTO.getCountHeaderId());

            // Set extra too
            if (!CollectionUtils.isEmpty(invWarehouseList)) {
                // Initiate extra status [S]
                InvCountExtra invExtraStatus = new InvCountExtra();
                invExtraStatus.setSourceId(invCountHeaderDTO.getCountHeaderId());
                invExtraStatus.setTenantId(invCountHeaderDTO.getTenantId());
                invExtraStatus.setEnabledFlag(1);
                invExtraStatus.setProgramKey("wms_sync_status");

                //initiate extra status [E]
                //initiate extra message [S]
                InvCountExtra invExtraMessage = new InvCountExtra();
                invExtraMessage.setSourceId(invCountHeaderDTO.getCountHeaderId());
                invExtraMessage.setTenantId(invCountHeaderDTO.getTenantId());
                invExtraMessage.setEnabledFlag(1);
                invExtraMessage.setProgramKey("wms_sync_error_message");

                //initiate extra message [E]
                //get WMS Warehouse status [S]
                int isWmsWarehouse = invWarehouseList.stream()
                        .filter(value -> value.getWarehouseId() == invCountHeaderDTO.getWarehouseId()) // Filter by warehouseID
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
                                null,
                                null,
                                MediaType.APPLICATION_JSON);
                        Map<String, String> response = objectMapper.readValue(responsePayloadDTO.getPayload(), Map.class);

                        // The Logic for Success and Failed
                        if (response.get("returnStatus").equals("S")) {
                            invExtraStatus.setProgramValue("SUCCESS");
                            invExtraMessage.setProgramValue("");
                            invCountHeaderDTO.setRelatedWmsOrderCode(responsePayloadDTO.getPayload());
                        } else {
                            invExtraStatus.setProgramValue("FAILED");
                            invExtraMessage.setProgramValue(response.get("returnMsg"));
                            errorMessage = errorMessage + "Response Exception : " + response.get("returnMsg") + ",";
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // For Skip
                    invExtraStatus.setProgramValue("SKIP");
                }//insert to Extra Table [S]
                invCountExtraRepository.insertSelective(invExtraStatus);
                //insert to Extra Table [E]
                if (!errorMessage.isEmpty()) {
                    invCountInfo.setFailedList(invCountHeaders);
                    invCountInfo.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
                    return invCountInfo;
                }
            }
        }
        invCountInfo.setSuccessList(invCountHeaders);
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
                Criteria criteria = new Criteria();
                criteria.select(InvCountLine.FIELD_OBJECT_VERSION_NUMBER);
                Long versionNumber = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId()).getObjectVersionNumber();
                invCountLineDTO.setObjectVersionNumber(versionNumber);

                InvCountLine line = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId());

                invCountLineDTO.setUnitDiffQty(invCountLineDTO.getUnitQty().subtract(invCountLineDTO.getSnapshotUnitQty()));

                invCountLineDTO.setCounterIds(line.getLastUpdatedBy().toString());
                InvCountLine invCountLine = new InvCountLine();
                BeanUtils.copyProperties(invCountLineDTO, invCountLine);
                invCountLineList.add(invCountLine);
            }
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

            List<Long> supervisorList = Arrays.stream(invCountHeader.getSupervisorIds().split(","))
                            .map(Long::parseLong)
                            .collect(Collectors.toList());
            boolean isSupervisor = false;

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

                    InvCountLineDTO invCountLine = new InvCountLineDTO();
                    InvCountLine line = invCountLineRepository.selectByPrimary(invCountLineDTO.getCountLineId());
                    BeanUtils.copyProperties(line, invCountLine);
                    if (invCountLine.getUnitQty() != invCountLineDTO.getUnitQty() && StringUtils.isEmpty(invCountHeader.getReason())){
                        invCountInfo.setFailedList(invCountHeaders);
                        errorMessage.append("there is a difference in counting");
                    }
                }
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
//        InvCountInfoDTO invCountInfo = submitCheck(invCountHeaders);
//        if (!CollectionUtils.isEmpty(invCountInfo.getFailedList())){
//            throw new CommonException(invCountInfo.getTotalErrorMsg());
//        }

        // Start Workflow
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            String workflow = profileClient.getProfileValueByOptions(invCountHeaderDTO.getTenantId(),
                    null,
                    null,
                    "FEXAM39.INV.COUNTING.ISWORKFLO");
            if (workflow.equals("1")){
                // Start Workflow
                Map<String, Object> variableMap = new HashMap<>();
                String starter = Constants.WORKFLOW_DEFAULT_DIMENSION.equals("USER") ?
                        DetailsHelper.getUserDetails().getUserId().toString() :
                        DetailsHelper.getUserDetails().getUsername();
                variableMap.put("departmentCode", iamDepartmentRepository.selectByPrimary(invCountHeaderDTO.getDepartmentId()).getDepartmentCode());

                RunInstance workFlowStart = workflowClient.startInstanceByFlowKey(invCountHeaderDTO.getTenantId(),
                        "INV_COUNT39_RESULT_SUBMIT",
                        invCountHeaderDTO.getCountNumber(),
                        Constants.WORKFLOW_DEFAULT_DIMENSION,
                        starter,
                        variableMap);

                InvCountHeader invCountHeader = new InvCountHeader();
                BeanUtils.copyProperties(invCountHeaderDTO, invCountHeader);
                invCountHeaderRepository.updateOptional(invCountHeader, InvCountHeader.FIELD_REASON);
            } else {
                String status = "CONFIRMED";
                invCountHeaderDTO.setCountStatus(status);
                InvCountHeader invCountHeader = new InvCountHeader();
                BeanUtils.copyProperties(invCountHeaderDTO, invCountHeader);
                invCountHeaderRepository.updateOptional(invCountHeader,
                        InvCountHeader.FIELD_COUNT_STATUS,
                        InvCountHeader.FIELD_REASON);
            }
        }
        return invCountHeaders;
    }

    @Override
    public void callBack(Long organizationId, WorkFlowDTO workFlowDTO) {
        InvCountHeader invCountHeader = invCountHeaderRepository.selectOne(new InvCountHeader());
        invCountHeader.setCountNumber(workFlowDTO.getBusinessKey());
        invCountHeader.setTenantId(organizationId);

        if (invCountHeader != null){
            invCountHeader.setCountNumber(workFlowDTO.getBusinessKey());
            invCountHeader.setCountStatus(workFlowDTO.getDocStatus());
            invCountHeader.setWorkflowId(workFlowDTO.getWorkflowId());
            invCountHeader.setApprovedTime(workFlowDTO.getApprovedTime());

            invCountHeaderRepository.updateByPrimaryKeySelective(invCountHeader);
        }
    }

    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeaderDTO) {

        ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();
        // Set a custom date format that matches the API response
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
        List<InvCountHeader> reportData = invCountHeaderRepository.selectByCondition(condition);
        List<InvCountLine> lineHeader = invCountLineRepository.selectAll();

        IamDepartment department = iamDepartmentRepository.selectByPrimary(invCountHeaderDTO.getDepartmentId());

        // 5. Process the data and set the lines
        List<InvCountHeaderDTO> result = null;
        for (InvCountHeader header : reportData) {
            List<String> lineList = new ArrayList<>();
            result = new ArrayList<>();

            for (InvCountLine line : lineHeader) {
                if (header.getCountHeaderId().equals(line.getCountHeaderId())) {
                    lineList.add(String.valueOf(line.getBatchId()));
                }
            }
            header.setBatchIds(String.join(",", lineList));

            List<InvMaterial> invMaterial = invMaterialRepository.selectByIds(header.getSnapshotMaterialIds());
            List<InvBatch> invBatch = invBatchRepository.selectByIds(invCountHeaderDTO.getBatchIds());

            // Create InvCountLineDTO for query
            InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
            invCountLineDTO.setCountHeaderId(header.getCountHeaderId());

            // Fetch InvCountLineDTO data from repository
            List<InvCountLineDTO> invCountLineDTOList = invCountLineRepository.selectList(invCountLineDTO);
            invCountLineDTOList.sort(Comparator.comparing(InvCountLineDTO::getCountLineId));
            InvCountHeaderDTO dto = invCountHeaderRepository.selectByPrimary(invCountHeaderDTO.getCountHeaderId());

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
            dto.setMaterialCode(invMaterial.stream()
                    .map(InvMaterial::getMaterialCode)
                    .collect(Collectors.joining(",")));

            // Get Batch Code
            dto.setBatchCode(invBatch.stream()
                    .map(InvBatch::getBatchCode)
                    .collect(Collectors.joining(",")));


            dto.setCountOrderLineList(invCountLineDTOList);
            result.add(dto);
        }
        return result;
    }

    private static InvStock getInvStock(InvCountHeaderDTO header) {
        // Get the InvStock
        InvStock invStock = new InvStock();
        invStock.setTenantId(header.getTenantId());
        invStock.setDepartmentId(header.getDepartmentId());
        invStock.setCompanyId(header.getCompanyId());
        invStock.setWarehouseId(header.getWarehouseId());

        // Get Dimension
        String dimension = header.getCountDimension();
        if (dimension.equals("SKU")){
            invStock.setMaterialIds(header.getSnapshotMaterialIds());
        } else if (dimension.equals("LOT")) {
            invStock.setMaterialIds(header.getSnapshotMaterialIds());
            invStock.setBatchIds(header.getSnapshotBatchIds());
        }
        return invStock;
    }

    private void validation(InvCountHeader invCountHeader){
        if (!isValidStatus(invCountHeader.getCountStatus())){
            throw new CommonException(
                    "verification failed , the error message is: only draft, in counting, rejected, and withdrawn status can be modified");
        }
    }

    private boolean isValidStatus(String status) {
        return "DRAFT".equals(status) || "INCOUNTING".equals(status) || "REJECTED".equals(status) || "WITHDRAWN".equals(status);
    }

    // Metode untuk memproses lines
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

    // Metode untuk memproses insert headers
    private void processInsertHeaders(List<InvCountHeader> insertList) {
        if (insertList.isEmpty()) {
            return;
        }
        for (InvCountHeader header : insertList) {
            Map<String, String> codeBuilderMap = new HashMap<>();
            String countNumber = codeRuleBuilder.generateCode(Constants.CODE_RULE_COUNT_NUMBER, codeBuilderMap);
            header.setCountNumber(countNumber);
            header.setDelFlag(0);
            header.setCountStatus("DRAFT");
        }
        invCountHeaderRepository.batchInsertSelective(insertList);
    }

    // Metode untuk memproses update headers dan lines
    private void processUpdateHeaders(List<InvCountHeader> updateList) {
        if (updateList.isEmpty()) {
            return;
        }
        for (InvCountHeader header : updateList) {
            if (!header.getCountStatus().equals("DRAFT")) {
                throw new CommonException("Only draft status allows updates");
            }
            processLines(header.getInvCountLines(), header.getCountHeaderId());
        }
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

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


}