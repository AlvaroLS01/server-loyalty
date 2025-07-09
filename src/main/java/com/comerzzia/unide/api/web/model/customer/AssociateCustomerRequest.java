package com.comerzzia.unide.api.web.model.customer;

import java.util.List;

import com.comerzzia.api.loyalty.web.model.customer.NewCustomer;

import lombok.Data;

@Data
public class AssociateCustomerRequest extends NewCustomer {

	private List<Card> cards;

	@Data
	public static class Card {

		private String cardNumber;
	}
}