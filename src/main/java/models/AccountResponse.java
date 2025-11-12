package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse extends BaseModel {
    private Long id;          // уникальный идентификатор аккаунта
    private Double balance;   // баланс аккаунта
    private String currency;  // валюта (если есть)
    private String name;      // имя или описание (если возвращается)
}
