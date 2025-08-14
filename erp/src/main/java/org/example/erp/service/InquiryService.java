package org.example.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.erp.dto.InquiryCreateDTO;
import org.example.erp.dto.InquiryPageResult;
import org.example.erp.dto.InquiryQueryParam;
import org.example.erp.dto.InquiryStatusUpdateDTO;
import org.example.erp.entity.inquiries;

/**
 * 询价单服务接口
 * 定义询价单相关的业务操作方法，继承MyBatis-Plus的IService接口，
 * 提供基础CRUD操作的同时扩展询价单特有业务逻辑
 */
public interface InquiryService extends IService<inquiries> {

    /**
     * 创建询价单
     * 根据传入的询价单创建DTO对象，生成新的询价单记录
     * @param inquiryDTO 询价单创建数据传输对象，包含创建询价单所需的信息
     * @return 创建成功的询价单实体对象
     */
    inquiries createInquiry(InquiryCreateDTO inquiryDTO);

    /**
     * 分页查询询价单列表
     * 根据查询参数进行条件筛选和分页，返回符合条件的询价单列表
     * @param queryParam 询价单查询参数对象，包含分页信息和筛选条件
     * @return 封装了询价单列表和分页信息的查询结果对象
     */
    InquiryPageResult getInquiries(InquiryQueryParam queryParam);

    /**
     * 更新询价单状态
     * 根据询价单ID和状态更新DTO，修改指定询价单的状态信息
     * @param inquiryId 询价单ID
     * @param updateDTO 询价单状态更新数据传输对象，包含新的状态信息
     * @return 更新后的询价单实体对象
     */
    inquiries updateInquiryStatus(String inquiryId, InquiryStatusUpdateDTO updateDTO);
}