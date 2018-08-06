package com.rahulhooda.integrationsampleapp_payumoneypnp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.payumoney.core.PayUmoneyConfig;
import com.payumoney.core.PayUmoneyConstants;
import com.payumoney.core.PayUmoneySdkInitializer;
import com.payumoney.core.entity.TransactionResponse;
import com.payumoney.sdkui.ui.utils.PPConfig;
import com.payumoney.sdkui.ui.utils.PayUmoneyFlowManager;
import com.payumoney.sdkui.ui.utils.ResultModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    public static final String TAG = "MainActivity : ";
    private boolean isDisableExitConfirmation = false;
    private String userMobile, userEmail;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private SharedPreferences userDetailsPreference;
    private EditText email_et, mobile_et, amount_et;
    private TextInputLayout email_til, mobile_til;
    private RadioGroup radioGroup_color_theme, radioGroup_select_env;
    private SwitchCompat switch_disable_wallet, switch_disable_netBanks, switch_disable_cards, switch_disable_ThirdPartyWallets, switch_disable_ExitConfirmation;
    private TextView logoutBtn;
    private AppCompatRadioButton radio_btn_default;
    private AppPreference mAppPreference;
    private AppCompatRadioButton radio_btn_theme_purple, radio_btn_theme_pink, radio_btn_theme_green, radio_btn_theme_grey;

    private Button payNowButton;
    private PayUmoneySdkInitializer.PaymentParam mPaymentParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppPreference = new AppPreference();
        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle(getString(R.string.app_name));
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        logoutBtn = (TextView) findViewById(R.id.logout_button);
        email_et = (EditText) findViewById(R.id.email_et);
        mobile_et = (EditText) findViewById(R.id.mobile_et);
        amount_et = (EditText) findViewById(R.id.amount_et);
        amount_et.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(7, 2)});
        email_til = (TextInputLayout) findViewById(R.id.email_til);
        mobile_til = (TextInputLayout) findViewById(R.id.mobile_til);
        radioGroup_color_theme = (RadioGroup) findViewById(R.id.radio_grp_color_theme);
        radio_btn_default = (AppCompatRadioButton) findViewById(R.id.radio_btn_theme_default);
        radio_btn_theme_pink = (AppCompatRadioButton) findViewById(R.id.radio_btn_theme_pink);
        radio_btn_theme_purple = (AppCompatRadioButton) findViewById(R.id.radio_btn_theme_purple);
        radio_btn_theme_green = (AppCompatRadioButton) findViewById(R.id.radio_btn_theme_green);
        radio_btn_theme_grey = (AppCompatRadioButton) findViewById(R.id.radio_btn_theme_grey);

        if (PayUmoneyFlowManager.isUserLoggedIn(getApplicationContext())) {
            logoutBtn.setVisibility(View.VISIBLE);
        } else {
            logoutBtn.setVisibility(View.GONE);
        }

        logoutBtn.setOnClickListener(this);
        switch_disable_wallet = (SwitchCompat) findViewById(R.id.switch_disable_wallet);
        switch_disable_netBanks = (SwitchCompat) findViewById(R.id.switch_disable_netbanks);
        switch_disable_cards = (SwitchCompat) findViewById(R.id.switch_disable_cards);
        switch_disable_ThirdPartyWallets = (SwitchCompat) findViewById(R.id.switch_disable_ThirdPartyWallets);
        switch_disable_ExitConfirmation = (SwitchCompat) findViewById(R.id.switch_disable_ExitConfirmation);
        AppCompatRadioButton radio_btn_sandbox = (AppCompatRadioButton) findViewById(R.id.radio_btn_sandbox);
        AppCompatRadioButton radio_btn_production = (AppCompatRadioButton) findViewById(R.id.radio_btn_production);
        radioGroup_select_env = (RadioGroup) findViewById(R.id.radio_grp_env);

        payNowButton = (Button) findViewById(R.id.pay_now_button);
        payNowButton.setOnClickListener(this);

        initListeners();

        //Set Up SharedPref
        setUpUserDetails();

        if (settings.getBoolean("is_prod_env", false)) {
            ((BaseApplication) getApplication()).setAppEnvironment(AppEnvironment.PRODUCTION);
            radio_btn_production.setChecked(true);
        } else {
            ((BaseApplication) getApplication()).setAppEnvironment(AppEnvironment.SANDBOX);
            radio_btn_sandbox.setChecked(true);
        }
        setupCitrusConfigs();
    }

    public static String hashCal(String str) {
        byte[] hashseq = str.getBytes();
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-512");
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();
            for (byte aMessageDigest : messageDigest) {
                String hex = Integer.toHexString(0xFF & aMessageDigest);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException ignored) {
        }
        return hexString.toString();
    }


    public static void setErrorInputLayout(EditText editText, String msg, TextInputLayout textInputLayout) {
        textInputLayout.setError(msg);
        editText.requestFocus();
    }

    public static boolean isValidEmail(String strEmail) {
        return strEmail != null && android.util.Patterns.EMAIL_ADDRESS.matcher(strEmail).matches();
    }

    public static boolean isValidPhone(String phone) {
        Pattern pattern = Pattern.compile(AppPreference.PHONE_PATTERN);

        Matcher matcher = pattern.matcher(phone);
        return matcher.matches();
    }

    private void setUpUserDetails() {
        userDetailsPreference = getSharedPreferences(AppPreference.USER_DETAILS, MODE_PRIVATE);
        userEmail = userDetailsPreference.getString(AppPreference.USER_EMAIL, mAppPreference.getDummyEmail());
        userMobile = userDetailsPreference.getString(AppPreference.USER_MOBILE,"");
        email_et.setText(userEmail);
        mobile_et.setText(userMobile);
        amount_et.setText(mAppPreference.getDummyAmount());
        restoreAppPref();
    }

    private void restoreAppPref() {
        //Set Up Disable Options
        switch_disable_wallet.setChecked(mAppPreference.isDisableWallet());
        switch_disable_cards.setChecked(mAppPreference.isDisableSavedCards());
        switch_disable_netBanks.setChecked(mAppPreference.isDisableNetBanking());
        switch_disable_ThirdPartyWallets.setChecked(mAppPreference.isDisableThirdPartyWallets());
        switch_disable_ExitConfirmation.setChecked(mAppPreference.isDisableExitConfirmation());

        //Set Up saved theme pref
        switch (AppPreference.selectedTheme) {
            case -1:
                radio_btn_default.setChecked(true);
                break;
            case R.style.AppTheme_pink:
                radio_btn_theme_pink.setChecked(true);
                break;
            case R.style.AppTheme_Grey:
                radio_btn_theme_grey.setChecked(true);
                break;
            case R.style.AppTheme_purple:
                radio_btn_theme_purple.setChecked(true);
                break;
            case R.style.AppTheme_Green:
                radio_btn_theme_green.setChecked(true);
                break;
            default:
                radio_btn_default.setChecked(true);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        payNowButton.setEnabled(true);

        if (PayUmoneyFlowManager.isUserLoggedIn(getApplicationContext())) {
            logoutBtn.setVisibility(View.VISIBLE);
        } else {
            logoutBtn.setVisibility(View.GONE);
        }
    }

    /**
     * This function sets the mode to PRODUCTION in Shared Preference
     */
    private void selectProdEnv() {

        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ((BaseApplication) getApplication()).setAppEnvironment(AppEnvironment.PRODUCTION);
                editor = settings.edit();
                editor.putBoolean("is_prod_env", true);
                editor.apply();

                if (PayUmoneyFlowManager.isUserLoggedIn(getApplicationContext())) {
                    logoutBtn.setVisibility(View.VISIBLE);
                } else {
                    logoutBtn.setVisibility(View.GONE);
                }

                setupCitrusConfigs();
            }
        }, AppPreference.MENU_DELAY);
    }

    /**
     * This function sets the mode to SANDBOX in Shared Preference
     */
    private void selectSandBoxEnv() {
        ((BaseApplication) getApplication()).setAppEnvironment(AppEnvironment.SANDBOX);
        editor = settings.edit();
        editor.putBoolean("is_prod_env", false);
        editor.apply();

        if (PayUmoneyFlowManager.isUserLoggedIn(getApplicationContext())) {
            logoutBtn.setVisibility(View.VISIBLE);
        } else {
            logoutBtn.setVisibility(View.GONE);

        }
        setupCitrusConfigs();
    }

    private void setupCitrusConfigs() {
        AppEnvironment appEnvironment = ((BaseApplication) getApplication()).getAppEnvironment();
        if (appEnvironment == AppEnvironment.PRODUCTION) {
            Toast.makeText(MainActivity.this, "Environment Set to Production", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Environment Set to SandBox", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This function sets the layout for activity
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result Code is -1 send from Payumoney activity
        Log.d("MainActivity", "request code " + requestCode + " resultcode " + resultCode);
        if (requestCode == PayUmoneyFlowManager.REQUEST_CODE_PAYMENT && resultCode == RESULT_OK && data !=
                null) {
            TransactionResponse transactionResponse = data.getParcelableExtra(PayUmoneyFlowManager
                    .INTENT_EXTRA_TRANSACTION_RESPONSE);

            ResultModel resultModel = data.getParcelableExtra(PayUmoneyFlowManager.ARG_RESULT);

            // Check which object is non-null
            if (transactionResponse != null && transactionResponse.getPayuResponse() != null) {
                if (transactionResponse.getTransactionStatus().equals(TransactionResponse.TransactionStatus.SUCCESSFUL)) {
                    //Success Transaction
                } else {
                    //Failure Transaction
                }

                // Response from Payumoney
                String payuResponse = transactionResponse.getPayuResponse();

                // Response from SURl and FURL
                String merchantResponse = transactionResponse.getTransactionDetails();

               new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("Payu's Data : " + payuResponse + "\n\n\n Merchant's Data: " + merchantResponse)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();

            } else if (resultModel != null && resultModel.getError() != null) {
                Log.d(TAG, "Error response : " + resultModel.getError().getTransactionResponse());
            } else {
                Log.d(TAG, "Both objects are null!");
            }
        }
    }

    @Override
    public void onClick(View v) {
        userEmail = email_et.getText().toString().trim();
        userMobile = mobile_et.getText().toString().trim();
        if (v.getId() == R.id.logout_button || validateDetails(userEmail, userMobile)) {
            switch (v.getId()) {
                case R.id.pay_now_button:
                    payNowButton.setEnabled(false);
                    launchPayUMoneyFlow();
                    break;
                case R.id.logout_button:
                    PayUmoneyFlowManager.logoutUser(getApplicationContext());
                    logoutBtn.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void initListeners() {
        email_et.addTextChangedListener(new EditTextInputWatcher(email_til));
        mobile_et.addTextChangedListener(new EditTextInputWatcher(mobile_til));


        radioGroup_color_theme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                mAppPreference.setOverrideResultScreen(true);

                switch (i) {
                    case R.id.radio_btn_theme_default:
                        AppPreference.selectedTheme = -1;
                        break;
                    case R.id.radio_btn_theme_pink:
                        AppPreference.selectedTheme = R.style.AppTheme_pink;
                        break;
                    case R.id.radio_btn_theme_grey:
                        AppPreference.selectedTheme = R.style.AppTheme_Grey;
                        break;
                    case R.id.radio_btn_theme_purple:
                        AppPreference.selectedTheme = R.style.AppTheme_purple;
                        break;
                    case R.id.radio_btn_theme_green:
                        AppPreference.selectedTheme = R.style.AppTheme_Green;
                        break;
                    default:
                        AppPreference.selectedTheme = -1;
                        break;
                }
            }
        });

        radioGroup_select_env.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                switch (i) {
                    case R.id.radio_btn_sandbox:
                        selectSandBoxEnv();
                        break;
                    case R.id.radio_btn_production:
                        selectProdEnv();
                        break;
                    default:
                        selectSandBoxEnv();
                        break;
                }
            }
        });

        switch_disable_cards.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PPConfig.getInstance().disableSavedCards(b);
            }
        });

        switch_disable_netBanks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PPConfig.getInstance().disableNetBanking(b);
            }
        });

        switch_disable_wallet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PPConfig.getInstance().disableWallet(b);
            }
        });

        switch_disable_ThirdPartyWallets.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PPConfig.getInstance().disableThirdPartyWallets(b);
            }
        });

        switch_disable_ExitConfirmation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isDisableExitConfirmation = b;
            }
        });
    }

    /**
     * This fucntion checks if email and mobile number are valid or not.
     *
     * @param email  email id entered in edit text
     * @param mobile mobile number entered in edit text
     * @return boolean value
     */
    public boolean validateDetails(String email, String mobile) {
        email = email.trim();
        mobile = mobile.trim();

        if (TextUtils.isEmpty(mobile)) {
            setErrorInputLayout(mobile_et, getString(R.string.err_phone_empty), mobile_til);
            return false;
        } else if (!isValidPhone(mobile)) {
            setErrorInputLayout(mobile_et, getString(R.string.err_phone_not_valid), mobile_til);
            return false;
        } else if (TextUtils.isEmpty(email)) {
            setErrorInputLayout(email_et, getString(R.string.err_email_empty), email_til);
            return false;
        } else if (!isValidEmail(email)) {
            setErrorInputLayout(email_et, getString(R.string.email_not_valid), email_til);
            return false;
        } else {
            return true;
        }
    }

    /**
     * This function prepares the data for payment and launches payumoney plug n play sdk
     */
    private void launchPayUMoneyFlow() {

        PayUmoneyConfig payUmoneyConfig = PayUmoneyConfig.getInstance();

        //Use this to set your custom text on result screen button
        payUmoneyConfig.setDoneButtonText(((EditText) findViewById(R.id.status_page_et)).getText().toString());

        //Use this to set your custom title for the activity
        payUmoneyConfig.setPayUmoneyActivityTitle(((EditText) findViewById(R.id.activity_title_et)).getText().toString());

        payUmoneyConfig.disableExitConfirmation(isDisableExitConfirmation);

        PayUmoneySdkInitializer.PaymentParam.Builder builder = new PayUmoneySdkInitializer.PaymentParam.Builder();

        double amount = 0;
        try {
            amount = Double.parseDouble(amount_et.getText().toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        String txnId = System.currentTimeMillis() + "";
        String phone = mobile_til.getEditText().getText().toString().trim();
        String productName = mAppPreference.getProductInfo();
        String firstName = mAppPreference.getFirstName();
        String email = email_til.getEditText().getText().toString().trim();
        String udf1 = "";
        String udf2 = "";
        String udf3 = "";
        String udf4 = "";
        String udf5 = "";
        String udf6 = "";
        String udf7 = "";
        String udf8 = "";
        String udf9 = "";
        String udf10 = "";

        AppEnvironment appEnvironment = ((BaseApplication) getApplication()).getAppEnvironment();
        builder.setAmount(amount)
                .setTxnId(txnId)
                .setPhone(phone)
                .setProductName(productName)
                .setFirstName(firstName)
                .setEmail(email)
                .setsUrl(appEnvironment.surl())
                .setfUrl(appEnvironment.furl())
                .setUdf1(udf1)
                .setUdf2(udf2)
                .setUdf3(udf3)
                .setUdf4(udf4)
                .setUdf5(udf5)
                .setUdf6(udf6)
                .setUdf7(udf7)
                .setUdf8(udf8)
                .setUdf9(udf9)
                .setUdf10(udf10)
                .setIsDebug(appEnvironment.debug())
                .setKey(appEnvironment.merchant_Key())
                .setMerchantId(appEnvironment.merchant_ID());

        try {
            mPaymentParams = builder.build();

            /*
            * Hash should always be generated from your server side.
            * */
            generateHashFromServer(mPaymentParams);

/*            *//**
             * Do not use below code when going live
             * Below code is provided to generate hash from sdk.
             * It is recommended to generate hash from server side only.
             * */
           /* mPaymentParams = calculateServerSideHashAndInitiatePayment1(mPaymentParams);

           if (AppPreference.selectedTheme != -1) {
                PayUmoneyFlowManager.startPayUMoneyFlow(mPaymentParams,MainActivity.this, AppPreference.selectedTheme,mAppPreference.isOverrideResultScreen());
            } else {
                PayUmoneyFlowManager.startPayUMoneyFlow(mPaymentParams,MainActivity.this, R.style.AppTheme_default, mAppPreference.isOverrideResultScreen());
            }*/

        } catch (Exception e) {
            // some exception occurred
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            payNowButton.setEnabled(true);
        }
    }

    /**
     * Thus function calculates the hash for transaction
     *
     * @param paymentParam payment params of transaction
     * @return payment params along with calculated merchant hash
     */
    private PayUmoneySdkInitializer.PaymentParam calculateServerSideHashAndInitiatePayment1(final PayUmoneySdkInitializer.PaymentParam paymentParam) {

        StringBuilder stringBuilder = new StringBuilder();
        HashMap<String, String> params = paymentParam.getParams();
        stringBuilder.append(params.get(PayUmoneyConstants.KEY) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.TXNID) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.AMOUNT) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.PRODUCT_INFO) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.FIRSTNAME) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.EMAIL) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.UDF1) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.UDF2) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.UDF3) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.UDF4) + "|");
        stringBuilder.append(params.get(PayUmoneyConstants.UDF5) + "||||||");

        AppEnvironment appEnvironment = ((BaseApplication) getApplication()).getAppEnvironment();
        stringBuilder.append(appEnvironment.salt());

        String hash = hashCal(stringBuilder.toString());
        paymentParam.setMerchantHash(hash);

        return paymentParam;
    }

    /**
     * This method generates hash from server.
     *
     * @param paymentParam payments params used for hash generation
     */
    public void generateHashFromServer(PayUmoneySdkInitializer.PaymentParam paymentParam) {
        //nextButton.setEnabled(false); // lets not allow the user to click the button again and again.

        HashMap<String, String> params = paymentParam.getParams();

        // lets create the post params
        StringBuffer postParamsBuffer = new StringBuffer();
        postParamsBuffer.append(concatParams(PayUmoneyConstants.KEY, params.get(PayUmoneyConstants.KEY)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.AMOUNT, params.get(PayUmoneyConstants.AMOUNT)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.TXNID, params.get(PayUmoneyConstants.TXNID)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.EMAIL, params.get(PayUmoneyConstants.EMAIL)));
        postParamsBuffer.append(concatParams("productinfo", params.get(PayUmoneyConstants.PRODUCT_INFO)));
        postParamsBuffer.append(concatParams("firstname", params.get(PayUmoneyConstants.FIRSTNAME)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.UDF1, params.get(PayUmoneyConstants.UDF1)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.UDF2, params.get(PayUmoneyConstants.UDF2)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.UDF3, params.get(PayUmoneyConstants.UDF3)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.UDF4, params.get(PayUmoneyConstants.UDF4)));
        postParamsBuffer.append(concatParams(PayUmoneyConstants.UDF5, params.get(PayUmoneyConstants.UDF5)));

        String postParams = postParamsBuffer.charAt(postParamsBuffer.length() - 1) == '&' ? postParamsBuffer.substring(0, postParamsBuffer.length() - 1).toString() : postParamsBuffer.toString();

        // lets make an api call
        GetHashesFromServerTask getHashesFromServerTask = new GetHashesFromServerTask();
        getHashesFromServerTask.execute(postParams);
    }


    protected String concatParams(String key, String value) {
        return key + "=" + value + "&";
    }

    /**
     * This AsyncTask generates hash from server.
     */
    private class GetHashesFromServerTask extends AsyncTask<String, String, String> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... postParams) {

            String merchantHash = "";
            try {
                //TODO Below url is just for testing purpose, merchant needs to replace this with their server side hash generation url
                URL url = new URL("https://payu.herokuapp.com/get_hash");

                String postParam = postParams[0];

                byte[] postParamsByte = postParam.getBytes("UTF-8");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postParamsByte.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postParamsByte);

                InputStream responseInputStream = conn.getInputStream();
                StringBuffer responseStringBuffer = new StringBuffer();
                byte[] byteContainer = new byte[1024];
                for (int i; (i = responseInputStream.read(byteContainer)) != -1; ) {
                    responseStringBuffer.append(new String(byteContainer, 0, i));
                }

                JSONObject response = new JSONObject(responseStringBuffer.toString());

                Iterator<String> payuHashIterator = response.keys();
                while (payuHashIterator.hasNext()) {
                    String key = payuHashIterator.next();
                    switch (key) {
                        /**
                         * This hash is mandatory and needs to be generated from merchant's server side
                         *
                         */
                        case "payment_hash":
                            merchantHash = response.getString(key);
                            break;
                        default:
                            break;
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return merchantHash;
        }

        @Override
        protected void onPostExecute(String merchantHash) {
            super.onPostExecute(merchantHash);

            progressDialog.dismiss();
            payNowButton.setEnabled(true);

            if (merchantHash.isEmpty() || merchantHash.equals("")) {
                Toast.makeText(MainActivity.this, "Could not generate hash", Toast.LENGTH_SHORT).show();
            } else {
                mPaymentParams.setMerchantHash(merchantHash);

                if (AppPreference.selectedTheme != -1) {
                    PayUmoneyFlowManager.startPayUMoneyFlow(mPaymentParams, MainActivity.this, AppPreference.selectedTheme, mAppPreference.isOverrideResultScreen());
                } else {
                    PayUmoneyFlowManager.startPayUMoneyFlow(mPaymentParams, MainActivity.this, R.style.AppTheme_default, mAppPreference.isOverrideResultScreen());
                }
            }
        }
    }

    public static class EditTextInputWatcher implements TextWatcher {

        private TextInputLayout textInputLayout;

        EditTextInputWatcher(TextInputLayout textInputLayout) {
            this.textInputLayout = textInputLayout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().length() > 0) {
                textInputLayout.setError(null);
                textInputLayout.setErrorEnabled(false);
            }
        }
    }

    public class DecimalDigitsInputFilter implements InputFilter {

        Pattern mPattern;

        public DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
            mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Matcher matcher = mPattern.matcher(dest);
            if (!matcher.matches())
                return "";
            return null;
        }

    }
}
