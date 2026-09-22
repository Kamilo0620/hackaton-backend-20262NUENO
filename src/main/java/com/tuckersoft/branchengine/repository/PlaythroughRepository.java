package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
    List<Playthrough> findByUserOrderByCreatedAtDesc(User user);
    List<Playthrough> findAllByOrderByCreatedAtDesc();
    boolean existsByPlayerTag(String playerTag);
    Optional<Playthrough> findByPlayerTag(String playerTag);
}
