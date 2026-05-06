package com.events.platform.repo;

import com.events.platform.domain.ContentReport;
import com.events.platform.domain.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    @Query(
            """
            SELECT r FROM ContentReport r
            LEFT JOIN FETCH r.targetEvent te
            LEFT JOIN FETCH r.targetPhoto tp
            LEFT JOIN FETCH tp.event tpe
            WHERE r.status = :status
            AND (
              (te IS NOT NULL AND te.host.id = :hostId)
              OR (tp IS NOT NULL AND tpe.host.id = :hostId)
            )
            ORDER BY r.createdAt DESC
            """)
    List<ContentReport> findOpenForHost(@Param("hostId") Long hostId, @Param("status") ReportStatus status);
}
