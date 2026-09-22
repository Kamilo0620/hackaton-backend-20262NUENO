package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Playthrough;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {
    List<Decision> findByPlaythroughAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Playthrough playthrough);
}
