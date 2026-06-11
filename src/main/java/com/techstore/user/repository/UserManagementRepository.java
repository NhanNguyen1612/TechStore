package com.techstore.user.repository;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserManagementRepository extends JpaRepository<User, Long> {

    long countByRoleAndEnabledTrue(Role role);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.role = :role,
                user.enabled = :enabled,
                user.tokenVersion = user.tokenVersion + 1
            where user.id = :userId
            """)
    int updateAccessAndInvalidateTokens(
            @Param("userId") Long userId,
            @Param("role") Role role,
            @Param("enabled") boolean enabled
    );
}
