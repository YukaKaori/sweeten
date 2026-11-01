package com.sweeten.analytics.service;

import com.sweeten.analytics.model.JobOption;

import java.util.List;
import java.util.Map;

/**
 * 员工统计（/report/empJobData、/report/empGenderData）。
 * 当前工作台未调用这两个接口，保留以兼容既有 API。
 */
public interface ReportService {
    JobOption getEmpJobDate();

    List<Map<String, Object>> getEmpGenderData();
}
