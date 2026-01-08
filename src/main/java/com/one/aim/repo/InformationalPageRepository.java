package com.one.aim.repo;

import com.one.aim.bo.InformationalPageBO;
import com.one.aim.constants.PageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InformationalPageRepository extends JpaRepository<InformationalPageBO, Long> {
    Optional<InformationalPageBO> findByPageType(PageType pageType);

}
