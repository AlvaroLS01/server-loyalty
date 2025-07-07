package com.comerzzia.unide.api.web.model.customer;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateCustomer {

	@QueryParam(value = "lyCustomerId")
	@NotNull
	protected Long lyCustomerId;

	@QueryParam(value = "lyCustomerCode")
	@javax.validation.constraints.Size(max = 30)
	protected String lyCustomerCode;

	@QueryParam(value = "name")
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 45)
	protected String name;

	@QueryParam(value = "lastName")
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 45)
	protected String lastName;

	@QueryParam(value = "address")
	@javax.validation.constraints.Size(max = 255)
	protected String address;

	@QueryParam(value = "city")
	@javax.validation.constraints.Size(max = 50)
	protected String city;

	@QueryParam(value = "location")
	@javax.validation.constraints.Size(max = 50)
	protected String location;

	@QueryParam(value = "province")
	@javax.validation.constraints.Size(max = 50)
	protected String province;

	@QueryParam(value = "postalCode")
	@javax.validation.constraints.Size(max = 8)
	protected String postalCode;

	@QueryParam(value = "countryCode")
	@javax.validation.constraints.Size(max = 4)
	protected String countryCode;

	@QueryParam(value = "identificationTypeCode")
	@javax.validation.constraints.Size(max = 10)
	protected String identificationTypeCode;

	@QueryParam(value = "vatNumber")
	@javax.validation.constraints.Size(max = 20)
	protected String vatNumber;

	@QueryParam(value = "remarks")
	@javax.ws.rs.DefaultValue(value = "false")
	protected Boolean remarks;

	@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
	@QueryParam(value = "dateOfBirth")
	protected java.util.Date dateOfBirth;

	@QueryParam(value = "genderName")
	@javax.validation.constraints.Size(max = 1)
	protected String genderName;

	@QueryParam(value = "maritalStatusCode")
	@javax.validation.constraints.Size(max = 1)
	protected String maritalStatusCode;

	@QueryParam(value = "languageCode")
	@javax.validation.constraints.Size(max = 6)
	protected String languageCode;

	@QueryParam(value = "paperLess")
	protected Boolean paperLess;

	@QueryParam(value = "active")
	@javax.ws.rs.DefaultValue(value = "true")
	protected Boolean active;

	@QueryParam(value = "tags")
	@NotNull(message = "Las etiquetas no pueden ser nulas")
	protected List<String> tags;
}
