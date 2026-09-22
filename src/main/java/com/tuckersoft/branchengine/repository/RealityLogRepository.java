package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.RealityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {
    List<RealityLog> findByDecisionOrderByCreatedAtAsc(Decision decision);
}
