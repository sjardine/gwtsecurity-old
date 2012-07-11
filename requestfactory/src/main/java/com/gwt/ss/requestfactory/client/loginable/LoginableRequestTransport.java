/**
 * $Id$
 */
package com.gwt.ss.requestfactory.client.loginable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.gwt.ss.client.GwtLogin;
import com.gwt.ss.client.exceptions.GwtSecurityException;
import com.gwt.ss.client.loginable.AbstractLoginHandler;
import com.gwt.ss.client.loginable.HasLoginHandler;
import com.gwt.ss.client.loginable.LoginCancelException;
import com.gwt.ss.client.loginable.LoginHandler;
import com.gwt.ss.shared.GwtConst;

/**
 * Intercepts the requestfactory requests and starts the login process if necessary.
 * 
 * @version $Rev$
 * @author Steven Jardine
 */
public class LoginableRequestTransport extends DefaultRequestTransport {

    private static final String EXCEPTION_PREFIX = "//EX[";

    private static SerializationStreamFactory streamFactory = null;

    /**
     * Deserialize the rpc exception.
     * 
     * @param payload the exception payload to deserialize.
     * @return the deserialized security exception.
     */
    private static GwtSecurityException deserializeSecurityException(String payload) {
        if (payload != null && payload.indexOf(EXCEPTION_PREFIX) != -1) {
            try {
                if (streamFactory == null) {
                    streamFactory = GWT.create(GwtLogin.class);
                }
                return (GwtSecurityException) streamFactory.createStreamReader(payload).readObject();
            } catch (Exception e) {
                GWT.log(e.getMessage(), e);
            }
        }
        return null;
    }

    private final HasLoginHandler loginHandler;

    /**
     * Constructor.
     * 
     * @param loginHandler the login handler.
     */
    public LoginableRequestTransport(final HasLoginHandler loginHandler) {
        super();
        this.loginHandler = loginHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void send(final String payload, final TransportReceiver receiver) {
        RequestBuilder builder = createRequestBuilder();
        builder.setHeader(GwtConst.GWT_RF_HEADER, "true");
        configureRequestBuilder(builder);
        builder.setRequestData(payload);
        builder.setCallback(new RequestCallback() {
            /** {@inheritDoc} */
            @Override
            public void onError(final Request request, final Throwable exception) {
                receiver.onTransportFailure(new ServerFailure(exception.getMessage()));
            }

            /** {@inheritDoc} */
            @Override
            public void onResponseReceived(final Request request, final Response response) {
                if (Response.SC_OK == response.getStatusCode()) {
                    String responsePayload = response.getText();
                    GwtSecurityException caught = deserializeSecurityException(responsePayload);
                    if (loginHandler != null && caught != null) {
                        LoginHandler lh = new AbstractLoginHandler() {
                            @Override
                            public void onCancelled() {
                                ServerFailure failure = new ServerFailure(CANCELLED_MSG, LoginCancelException.class
                                    .getName(), null, false);
                                receiver.onTransportFailure(failure);
                            }

                            @Override
                            public void resendPayload() {
                                send(payload, receiver);
                            }
                        };
                        lh.setLoginHandlerRegistration(loginHandler.addLoginHandler(lh));
                        loginHandler.startLogin(caught);
                    } else {
                        receiver.onTransportSuccess(responsePayload);
                    }
                } else {
                    String message = response.getStatusCode() + " " + response.getText();
                    receiver.onTransportFailure(new ServerFailure(message));
                }
            }
        });
        try {
            builder.send();
        } catch (RequestException e) {
            GWT.log(e.getMessage(), e);
        }
    }

}
