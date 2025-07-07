package com.comerzzia.unide.api.web.model.customer;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class AssociateCustomerRequest {
    private String lyCustomerCode;
    private String name;
    private String lastName;
    private String address;
    private String city;
    private String location;
    private String province;
    private String postalCode;
    private String countryCode;
    private String identificationTypeCode;
    private String vatNumber;
    private String remarks;
    private Date dateOfBirth;
    private String genderName;
    private String maritalStatusCode;
    private Boolean active;
    private String languageCode;
    private List<Collective> collectives;
    private List<Contact> contacts;
    private List<Tag> tags;
    private CustomerLink customerLink;
    private NewCustomerAccess newCustomerAccess;
    private List<Card> cards;

    @Data
    public static class Collective {
        private String collectiveCode;
        private String collectiveDes;
    }

    @Data
    public static class Contact {
        private String contactTypeCode;
        private String value;
        private Boolean getNotifications;
        private Boolean comGetNotifications;
    }

    @Data
    public static class Tag {
        private String tagUid;
        private String tag;
        private Integer priority;
    }

    @Data
    public static class CustomerLink {
        private String classUid;
        private String objectUid;
        private Long lyCustomerId;
    }

    @Data
    public static class NewCustomerAccess {
        private String user;
        private String password;
    }

    @Data
    public static class Card {
        private String cardNumber;
    }
}
