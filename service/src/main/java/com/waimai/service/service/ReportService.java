package com.waimai.service.service;

import com.waimai.common.vo.MerchantReportVO;
import java.time.LocalDate;

public interface ReportService {
    MerchantReportVO dailyReport(Long merchantId, LocalDate date);
    MerchantReportVO weeklyReport(Long merchantId, LocalDate startDate);
    MerchantReportVO monthlyReport(Long merchantId, String month);
}
