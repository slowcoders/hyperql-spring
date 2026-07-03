package org.slowcoders.basecamp.app.model;

import lombok.*;

@ToString
@Getter
@Setter
@Builder
// @DynamoDbBean
public class SessionUserDto { //implements BaseDto {

    private String userId;
    private String chainId;
    private String propertyId;
    private String userTypeCd;
    // zz private String languageCd;

    private String apiUserYn;
    private String duplicateUserYn;
    private String ownerId;
    private String cashierId;
    private String loginId;

    private String userName;
    private String userEmail;
    private String phoneNo;
    private String mobilePhoneNo;
    private String jobPositionName;


    // @DynamoDbPartitionKey
    public final String getUserId() {
        return userId;
    }
}
