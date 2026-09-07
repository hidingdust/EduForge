package com.woniuxy.repository;

import com.woniuxy.entity.PO.UserPO;
import jakarta.transaction.Transactional;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

// ✅ 把 Long 改成 Integer
public interface UserRepository extends JpaRepository<UserPO, Integer> {

    Optional<UserPO> findByPhone(String phone);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserPO u SET u.gold = COALESCE(u.gold, 0) + :gold WHERE u.userId = :userId")
    int addGold(@Param("userId") Integer userId, @Param("gold") Integer gold);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserPO u SET u.gold = COALESCE(u.gold, 0) - :gold, u.score = COALESCE(u.score, 0) + :score WHERE u.userId = :userId AND COALESCE(u.gold, 0) >= :gold")
    int exchange(@Param("userId") Integer userId, @Param("gold") Integer gold, @Param("score") Integer score);
}