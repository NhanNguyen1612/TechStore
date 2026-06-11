package com.techstore.chat.repository;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.chat.dto.response.ChatContactResponse;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ChatContactRepository extends Repository<User, Long> {

    @Query(
            value = """
                    select new com.techstore.chat.dto.response.ChatContactResponse(
                        user.id,
                        user.fullName,
                        user.email,
                        user.role,
                        profile.avatarUrl
                    )
                    from User user
                    left join UserProfile profile on profile.user = user
                    where user.id <> :viewerId
                      and user.enabled = true
                      and user.deleted = false
                      and (
                        lower(user.fullName) like concat('%', :search, '%')
                        or lower(user.email) like concat('%', :search, '%')
                      )
                    order by lower(user.fullName), user.id
                    """,
            countQuery = """
                    select count(user)
                    from User user
                    where user.id <> :viewerId
                      and user.enabled = true
                      and user.deleted = false
                      and (
                        lower(user.fullName) like concat('%', :search, '%')
                        or lower(user.email) like concat('%', :search, '%')
                      )
                    """
    )
    Page<ChatContactResponse> findAllAvailable(
            @Param("viewerId") Long viewerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    select new com.techstore.chat.dto.response.ChatContactResponse(
                        user.id,
                        user.fullName,
                        user.email,
                        user.role,
                        profile.avatarUrl
                    )
                    from User user
                    left join UserProfile profile on profile.user = user
                    where user.id <> :viewerId
                      and user.enabled = true
                      and user.deleted = false
                      and user.role in :roles
                      and (
                        lower(user.fullName) like concat('%', :search, '%')
                        or lower(user.email) like concat('%', :search, '%')
                      )
                    order by lower(user.fullName), user.id
                    """,
            countQuery = """
                    select count(user)
                    from User user
                    where user.id <> :viewerId
                      and user.enabled = true
                      and user.deleted = false
                      and user.role in :roles
                      and (
                        lower(user.fullName) like concat('%', :search, '%')
                        or lower(user.email) like concat('%', :search, '%')
                      )
                    """
    )
    Page<ChatContactResponse> findAvailableByRole(
            @Param("viewerId") Long viewerId,
            @Param("roles") Set<Role> roles,
            @Param("search") String search,
            Pageable pageable
    );
}
