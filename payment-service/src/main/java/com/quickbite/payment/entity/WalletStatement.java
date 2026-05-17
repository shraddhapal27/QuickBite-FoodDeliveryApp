package com.quickbite.payment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnore
    private Wallet wallet;

    @Column(nullable = false)
    private Double amount;

    // CREDIT for deposit, DEBIT for payment/deduction
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String description;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
