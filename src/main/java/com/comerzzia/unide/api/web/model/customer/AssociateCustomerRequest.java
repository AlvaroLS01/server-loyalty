package com.comerzzia.unide.api.web.model.customer;

import java.util.List;

import com.comerzzia.api.loyalty.web.model.customer.NewCustomer;

import lombok.Data;

/**
 * Request body used to associate a loyalty customer. It extends the
 * {@link NewCustomer} model provided by the loyalty API and only adds the
 * information related to the cards that must be associated with the customer.
 */
@Data
public class AssociateCustomerRequest extends NewCustomer {
    /** Cards that will be associated with the customer. */
    private List<Card> cards;

    @Data
    public static class Card {
        private String cardNumber;
    }
}
