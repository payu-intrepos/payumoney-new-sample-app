package com.rahulhooda.integrationsampleapp_payumoneypnp;

/**
 * Created by Rahul Hooda on 14/7/17.
 */

/**
 * This class keeps all the app prefernces
 * */
public class AppPreference {

    private String dummyMobile = "9881716157";
    private String dummyAmount = "10";
    private String dummyEmail = "xyz@gmail.com";
    private String productInfo = "product_info";
    private String firstName = "firstname";
    private boolean isOverrideResultScreen = false;
 
    public static final String USER_EMAIL = "user_email";
    public static final String USER_MOBILE = "user_mobile";
    public static final String PHONE_PATTERN = "^[987]\\d{9}$";
    public static final long MENU_DELAY = 300;
    public static String USER_DETAILS = "user_details";
    public static int selectedTheme = -1;

    private boolean isDisableWallet, isDisableSavedCards, isDisableNetBanking;

    boolean isDisableWallet() {
        return isDisableWallet;
    }

    void setDisableWallet(boolean disableWallet) {
        isDisableWallet = disableWallet;
    }

    boolean isDisableSavedCards() {
        return isDisableSavedCards;
    }

    void setDisableSavedCards(boolean disableSavedCards) {
        isDisableSavedCards = disableSavedCards;
    }

    boolean isDisableNetBanking() {
        return isDisableNetBanking;
    }

    void setDisableNetBanking(boolean disableNetBanking) {
        isDisableNetBanking = disableNetBanking;
    }

    public String getDummyMobile() {
        return dummyMobile;
    }

    public void setDummyMobile(String dummyMobile) {
        this.dummyMobile = dummyMobile;
    }

    public String getDummyAmount() {
        return dummyAmount;
    }

    public void setDummyAmount(String dummyAmount) {
        this.dummyAmount = dummyAmount;
    }

    public String getDummyEmail() {
        return dummyEmail;
    }

    public void setDummyEmail(String dummyEmail) {
        this.dummyEmail = dummyEmail;
    }

    public boolean isOverrideResultScreen() {
        return isOverrideResultScreen;
    }

    public void setOverrideResultScreen(boolean overrideResultScreen) {
        isOverrideResultScreen = overrideResultScreen;
    }
    public String getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(String productInfo) {
        this.productInfo = productInfo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
