package com.waimai.common.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartVO {
    private Long merchantId;
    private String merchantName;
    private List<CartItemVO> items;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private Integer itemCount;

    public BigDecimal getTotal() {
        if (total != null) return total;
        return totalAmount != null ? totalAmount.add(deliveryFee != null ? deliveryFee : BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public void setTotal(BigDecimal total) { this.total = total; }

    public Integer getItemCount() {
        return items != null ? items.size() : 0;
    }
}
