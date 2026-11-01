package com.sweeten.analytics.service;

import com.sweeten.analytics.mapper.AnalyticsMapper;
import com.sweeten.analytics.model.JobOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AnalyticsMapper analyticsMapper;

    @Override
    public JobOption getEmpJobDate() {
        List<Map<String, Object>> list = analyticsMapper.countStaffByJob();
        List<Object> jobList = list.stream().map(dataMap -> dataMap.get("pos")).toList();
        List<Object> dataList = list.stream().map(dataMap -> dataMap.get("total")).toList();
        return new JobOption(jobList, dataList);
    }

    @Override
    public List<Map<String, Object>> getEmpGenderData() {
        return analyticsMapper.countStaffByGender();
    }
}
