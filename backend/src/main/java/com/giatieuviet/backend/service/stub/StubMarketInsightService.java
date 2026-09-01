package com.giatieuviet.backend.service.stub;

import com.giatieuviet.backend.api.dto.MarketInsightResponse;
import com.giatieuviet.backend.service.MarketInsightService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TEMPORARY in-memory data source, standing in for the PostgreSQL-backed
 * implementation until the database is wired up.
 */
@Service
public class StubMarketInsightService implements MarketInsightService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private static final String INSIGHT_TEXT = """
            Giá tiêu trong nước tiếp tục xu hướng tăng nhẹ nhờ nguồn cung nội địa thu hẹp cuối vụ \
            và nhu cầu xuất khẩu ổn định sang Trung Quốc, Mỹ. Mưa lớn kéo dài tại Đắk Lắk và Đắk Nông \
            trong tuần qua có thể ảnh hưởng tiến độ thu hoạch, tạo áp lực tăng giá ngắn hạn. \
            Mô hình giữ mức tăng trung bình 1,5–2% trong 30 ngày tới, biên độ dao động nới rộng dần \
            theo thời gian dự báo.""";

    @Override
    public MarketInsightResponse latestInsight() {
        return new MarketInsightResponse(INSIGHT_TEXT, "08:00, " + LocalDate.now().format(DAY_LABEL));
    }
}
