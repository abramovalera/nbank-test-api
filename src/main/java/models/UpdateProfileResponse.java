package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileResponse extends BaseModel {
    private String message;
    private Customer customer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private int id;
        private String username;
        private String password;
        private String name;
        private String role;
        private List<Account> accounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Account {
        private int id;
        private String accountNumber;
        private double balance;
        private List<Transaction> transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transaction {
        private int id;
        private double amount;
        private String type;
        private String timestamp;
        private Integer relatedAccountId;
    }
}
