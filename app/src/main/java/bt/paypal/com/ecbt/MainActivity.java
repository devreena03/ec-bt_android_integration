package bt.paypal.com.ecbt;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.HttpGet;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.net.HttpURLConnection;
import java.net.URL;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements PaymentMethodNonceCreatedListener {

    private String clientToken=null;
    private String nonce;
    private BraintreeFragment mBraintreeFragment;
    private static Activity mActivityRef = null;
    private static final String BASE_URL = "https://paypal-integration-sample.herokuapp.com";
   // private static final String BASE_URL = "https://iocor.serveo.net";
    private static final String CLIENT_TOKEN_URL = "/api/paypal/ecbt/client_token";
    private static final String CHECKOUT = "/api/paypal/ecbt/checkout";


    public void showLoader(Boolean setVisibilty) {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        int colorCodeDark = Color.parseColor("#253B80");
        progress.setIndeterminateTintList(ColorStateList.valueOf(colorCodeDark));
        if(setVisibilty) {
            progress.setVisibility(View.VISIBLE);
        }else {
            progress.setVisibility(View.INVISIBLE);
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("enter  ","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivityRef = this;



    }
    public void payNow(View v){
        Button btn = (Button) findViewById(R.id.button);
        btn.setEnabled(false);
        Log.e("enter  ","payNow");
        showLoader(true); // set loader visible


        try {
                AsyncHttpClient client = new AsyncHttpClient();
                client.get(BASE_URL + CLIENT_TOKEN_URL, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.i("fail", responseString);
                    }
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
                        Log.i("DONE clientToken",response);
                        clientToken = response;
                        try {
                           mBraintreeFragment = BraintreeFragment.newInstance(mActivityRef, clientToken);
                           setupBraintreeAndStartExpressCheckout();
                           Log.e("exit  ","payNow");
                           showLoader(false); // set loader visible
                        } catch (InvalidArgumentException e) {
                            Log.e("error  ",e.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            Log.e("error  ",e.getMessage());
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        Log.e("enter  ","onPaymentMethodNonceCreated");
        // Send nonce to server
        String nonce = paymentMethodNonce.getNonce();
        if (paymentMethodNonce instanceof PayPalAccountNonce) {
            PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce)paymentMethodNonce;

            // Access additional information
            String email = payPalAccountNonce.getEmail();
            String firstName = payPalAccountNonce.getFirstName();
            String lastName = payPalAccountNonce.getLastName();
            String phone = payPalAccountNonce.getPhone();

            // See PostalAddress.java for details
            PostalAddress billingAddress = payPalAccountNonce.getBillingAddress();
            PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
        }
        postNonceToServer(nonce);
        Log.e("exit  ","onPaymentMethodNonceCreated");
    }

    private void postNonceToServer(String nonce) {
        Log.e("enter  ","postNonceToServer");
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("nonce", nonce);
        params.put("amount", "20");
        params.put("currency", "INR");
        Log.e("nonce  ",nonce);
        client.post(BASE_URL + CHECKOUT, params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String body = new String(responseBody);
                        Log.d("payment  ",body);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Payment "+body,
                                Toast.LENGTH_SHORT);

                        toast.show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e("checkout  ",error.getMessage());
                    }
                    // Your implementation here
                }
        );
        Log.e("exit  ","postNonceToServer");
    }

    private void setupBraintreeAndStartExpressCheckout() {
        Log.e("enter  ","setupBraintreeAndStartExpressCheckout");
        PostalAddress add = new PostalAddress();
        add.recipientName("S2S store");
        add.streetAddress("test street");
        add.locality("city");
        add.region("state");
        add.postalCode("600044");
        add.countryCodeAlpha2("IN");


        PayPalRequest request = new PayPalRequest("20")
                .currencyCode("INR")
                .intent(PayPalRequest.INTENT_SALE)
                .userAction(PayPalRequest.USER_ACTION_COMMIT)
                .shippingAddressRequired(true)
                .shippingAddressOverride(add);
        PayPal.requestOneTimePayment(mBraintreeFragment, request);
        Log.e("exit  ","setupBraintreeAndStartExpressCheckout");
    }

}
