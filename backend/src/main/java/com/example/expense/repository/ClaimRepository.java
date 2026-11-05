package com.example.expense.repository;

import com.example.expense.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    Optional<Claim> findByIdAndEmployeeId(Long id, Long employeeId);

    /** rows: [status, count, sum(amount)] */
    @Query("select c.status, count(c), coalesce(sum(c.amount), 0) from Claim c where c.employee.id = :uid group by c.status")
    List<Object[]> employeeTotals(@Param("uid") Long uid);

    /** rows: [status, count] */
    @Query("select c.status, count(c) from Claim c group by c.status")
    List<Object[]> countByStatus();

    /** rows: [category, sum(amount)] */
    @Query("select c.category, coalesce(sum(c.amount), 0) from Claim c group by c.category order by sum(c.amount) desc")
    List<Object[]> totalsByCategory();

    /** rows: [year, month, count, sum(amount)] */
    @Query("select year(c.expenseDate), month(c.expenseDate), count(c), coalesce(sum(c.amount), 0) from Claim c " +
           "group by year(c.expenseDate), month(c.expenseDate) order by year(c.expenseDate), month(c.expenseDate)")
    List<Object[]> monthlySummary();
}
