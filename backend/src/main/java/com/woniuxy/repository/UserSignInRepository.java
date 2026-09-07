package com.woniuxy.repository;

import com.woniuxy.entity.UserSignIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserSignInRepository extends JpaRepository<UserSignIn, Integer> {

    // 查询某天是否签到
    Optional<UserSignIn> findByUserIdAndSignDate(Integer userId, LocalDate signDate);

    // 查询某用户某月的所有签到记录
    @Query("SELECT s FROM UserSignIn s WHERE s.userId = ?1 AND s.monthYear = ?2 ORDER BY s.signDate ASC")
    List<UserSignIn> findByUserIdAndMonthYear(Integer userId, String monthYear);

    // 查询某用户某月签到天数
    @Query("SELECT COUNT(s) FROM UserSignIn s WHERE s.userId = ?1 AND s.monthYear = ?2")
    Integer countByUserIdAndMonthYear(Integer userId, String monthYear);

    // 查询用户所有签到记录（用于连续签到计算）
    List<UserSignIn> findByUserIdOrderBySignDateDesc(Integer userId);
}