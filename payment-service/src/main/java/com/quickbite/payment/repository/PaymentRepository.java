package com.quickbite.payment.repository;

import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByCustomerId(Long customerId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByPaidAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.customerId = :customerId AND p.status = 'PAID'")
    Double sumAmountByCustomerId(Long customerId);
}
