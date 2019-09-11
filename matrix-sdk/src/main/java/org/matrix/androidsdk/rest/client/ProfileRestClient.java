/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.androidsdk.rest.client;

import android.net.Uri;
import android.text.TextUtils;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.RestClient;
import org.matrix.androidsdk.core.JsonUtils;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.features.identityserver.IdentityServerManager;
import org.matrix.androidsdk.features.identityserver.IdentityServerNotConfiguredException;
import org.matrix.androidsdk.rest.api.ProfileApi;
import org.matrix.androidsdk.rest.callback.RestAdapterCallback;
import org.matrix.androidsdk.rest.model.ChangePasswordParams;
import org.matrix.androidsdk.rest.model.DeactivateAccountParams;
import org.matrix.androidsdk.rest.model.ForgetPasswordParams;
import org.matrix.androidsdk.rest.model.ForgetPasswordResponse;
import org.matrix.androidsdk.rest.model.RequestEmailValidationParams;
import org.matrix.androidsdk.rest.model.RequestEmailValidationResponse;
import org.matrix.androidsdk.rest.model.RequestPhoneNumberValidationParams;
import org.matrix.androidsdk.rest.model.RequestPhoneNumberValidationResponse;
import org.matrix.androidsdk.rest.model.ThreePidCreds;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.login.AuthParamsEmailIdentity;
import org.matrix.androidsdk.rest.model.login.AuthParamsLoginPassword;
import org.matrix.androidsdk.rest.model.login.ThreePidCredentials;
import org.matrix.androidsdk.rest.model.login.TokenRefreshParams;
import org.matrix.androidsdk.rest.model.login.TokenRefreshResponse;
import org.matrix.androidsdk.rest.model.pid.AccountThreePidsResponse;
import org.matrix.androidsdk.rest.model.pid.AddThreePidsParams;
import org.matrix.androidsdk.rest.model.pid.DeleteThreePidParams;
import org.matrix.androidsdk.rest.model.pid.ThirdPartyIdentifier;
import org.matrix.androidsdk.rest.model.pid.ThreePid;

import java.util.List;

import retrofit2.Response;

/**
 * Class used to make requests to the profile API.
 */
public class ProfileRestClient extends RestClient<ProfileApi> {
    private static final String LOG_TAG = ProfileRestClient.class.getSimpleName();

    /**
     * {@inheritDoc}
     */
    public ProfileRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, ProfileApi.class, "", JsonUtils.getGson(false));
    }

    /**
     * Get the user's display name.
     *
     * @param userId   the user id
     * @param callback the callback to return the name on success
     */
    public void displayname(final String userId, final ApiCallback<String> callback) {
        final String description = "display name userId : " + userId;

        mApi.displayname(userId)
                .enqueue(new RestAdapterCallback<User>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        displayname(userId, callback);
                    }
                }) {
                    @Override
                    public void success(User user, Response<User> response) {
                        onEventSent();
                        callback.onSuccess(user.displayname);
                    }
                });
    }

    /**
     * Update this user's own display name.
     *
     * @param userId   the userId
     * @param newName  the new name
     * @param callback the callback if the call succeeds
     */
    public void updateDisplayname(final String userId, final String newName, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "updateDisplayname newName : " + newName;
        final String description = "update display name";

        // TODO Do not create a User for this
        User user = new User();
        user.displayname = newName;

        // don't retry if the network comes back
        // let the user chooses what he want to do
        mApi.displayname(userId, user)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateDisplayname(userId, newName, callback);
                    }
                }));
    }

    /**
     * Get the user's avatar URL.
     *
     * @param userId   the user id
     * @param callback the callback to return the URL on success
     */
    public void avatarUrl(final String userId, final ApiCallback<String> callback) {
        final String description = "avatarUrl userId : " + userId;

        mApi.avatarUrl(userId)
                .enqueue(new RestAdapterCallback<User>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        avatarUrl(userId, callback);
                    }
                }) {
                    @Override
                    public void success(User user, Response response) {
                        onEventSent();
                        callback.onSuccess(user.getAvatarUrl());
                    }
                });
    }

    /**
     * Update this user's own avatar URL.
     *
     * @param userId   the userId
     * @param newUrl   the new name
     * @param callback the callback if the call succeeds
     */
    public void updateAvatarUrl(final String userId, final String newUrl, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "updateAvatarUrl newUrl : " + newUrl;
        final String description = "updateAvatarUrl";

        // TODO Do not create a User for this
        User user = new User();
        user.setAvatarUrl(newUrl);

        mApi.avatarUrl(userId, user)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateAvatarUrl(userId, newUrl, callback);
                    }
                }));
    }

    /**
     * Update the password
     *
     * @param userId      the user id
     * @param oldPassword the former password
     * @param newPassword the new password
     * @param callback    the callback
     */
    public void updatePassword(final String userId, final String oldPassword, final String newPassword, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "update password : " + userId + " oldPassword " + oldPassword + " newPassword " + newPassword;
        final String description = "update password";

        ChangePasswordParams passwordParams = new ChangePasswordParams();

        AuthParamsLoginPassword auth = new AuthParamsLoginPassword();
        auth.user = userId;
        auth.password = oldPassword;

        passwordParams.auth = auth;
        passwordParams.new_password = newPassword;

        mApi.updatePassword(passwordParams)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updatePassword(userId, oldPassword, newPassword, callback);
                    }
                }
                ));
    }

    /**
     * Reset the password to a new one.
     *
     * @param newPassword         the new password
     * @param threePidCredentials the three pids.
     * @param callback            the callback
     */
    public void resetPassword(final String newPassword, final ThreePidCredentials threePidCredentials, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "Reset password : " + threepid_creds + " newPassword " + newPassword;
        final String description = "Reset password";

        ChangePasswordParams passwordParams = new ChangePasswordParams();

        AuthParamsEmailIdentity auth = new AuthParamsEmailIdentity();
        auth.threePidCredentials = threePidCredentials;

        passwordParams.auth = auth;
        passwordParams.new_password = newPassword;

        mApi.updatePassword(passwordParams)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        resetPassword(newPassword, threePidCredentials, callback);
                    }
                }));
    }

    /**
     * Reset the password server side.
     *
     * @param email    the email to send the password reset.
     * @param callback the callback
     */
    public void forgetPassword(final String email, final ApiCallback<ThreePid> callback) {
        final String description = "forget password";

        if (!TextUtils.isEmpty(email)) {
            final ThreePid pid = new ThreePid(email, ThreePid.MEDIUM_EMAIL);

            final ForgetPasswordParams forgetPasswordParams = new ForgetPasswordParams();
            forgetPasswordParams.email = email;
            forgetPasswordParams.client_secret = pid.clientSecret;
            forgetPasswordParams.send_attempt = 1;

            // TODO privacy: there is something to do here, we cannot use identity server from hsConfig
            Uri identityServerUri = mHsConfig.getIdentityServerUri();
            if (identityServerUri == null) {
                callback.onUnexpectedError(new IdentityServerNotConfiguredException());
                return;
            }

            forgetPasswordParams.id_server = identityServerUri.getHost();

            mApi.forgetPassword(forgetPasswordParams)
                    .enqueue(new RestAdapterCallback<ForgetPasswordResponse>(description, mUnsentEventsManager, callback,
                            new RestAdapterCallback.RequestRetryCallBack() {
                                @Override
                                public void onRetry() {
                                    forgetPassword(email, callback);
                                }
                            }) {
                        @Override
                        public void success(ForgetPasswordResponse forgetPasswordResponse, Response response) {
                            onEventSent();

                            pid.sid = forgetPasswordResponse.sid;
                            callback.onSuccess(pid);
                        }
                    });
        }
    }

    /**
     * Deactivate account
     *
     * @param userId        current user id
     * @param userPassword  current password
     * @param eraseUserData true to also erase all the user data
     * @param callback      the callback
     */
    public void deactivateAccount(final String userId,
                                  final String userPassword,
                                  final boolean eraseUserData,
                                  final ApiCallback<Void> callback) {
        final String description = "deactivate account";

        final DeactivateAccountParams params = new DeactivateAccountParams();
        params.auth = new AuthParamsLoginPassword();
        params.auth.user = userId;
        params.auth.password = userPassword;

        params.erase = eraseUserData;

        mApi.deactivate(params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        deactivateAccount(userId, userPassword, eraseUserData, callback);
                    }
                }));
    }

    /**
     * Refresh access/refresh tokens, using the current refresh token.
     *
     * @param refreshToken the refreshToken
     * @param callback     the callback success and failure callback
     */
    public void refreshTokens(final String refreshToken, final ApiCallback<TokenRefreshResponse> callback) {
        final String description = "refreshTokens";

        TokenRefreshParams params = new TokenRefreshParams();
        params.refresh_token = refreshToken;

        mApi.tokenRefresh(params)
                .enqueue(new RestAdapterCallback<TokenRefreshResponse>(description,
                        mUnsentEventsManager,
                        new SimpleApiCallback<TokenRefreshResponse>(callback) {
                            @Override
                            public void onSuccess(TokenRefreshResponse info) {
                                setAccessToken(info.accessToken);
                                if (null != callback) {
                                    callback.onSuccess(info);
                                }
                            }
                        },
                        null) {
                });
    }

    /**
     * List all 3PIDs linked to the Matrix user account.
     *
     * @param callback the asynchronous callback called with the response
     */
    public void threePIDs(final ApiCallback<List<ThirdPartyIdentifier>> callback) {
        final String description = "threePIDs";

        mApi.threePIDs()
                .enqueue(new RestAdapterCallback<AccountThreePidsResponse>(description, mUnsentEventsManager, callback, null) {
                    @Override
                    public void success(AccountThreePidsResponse accountThreePidsResponse, Response<AccountThreePidsResponse> response) {
                        onEventSent();
                        if (null != callback) {
                            callback.onSuccess(accountThreePidsResponse.threepids);
                        }

                    }
                });
    }

    /**
     * Request an email validation token.
     *
     * @param identityServerUri the identity server to use
     * @param address              the email address
     * @param clientSecret         the client secret number
     * @param attempt              the attempt count
     * @param nextLink             the next link
     * @param isDuringRegistration true if it occurs during a registration flow
     * @param callback             the callback
     */
    public void requestEmailValidationToken(Uri identityServerUri, final String address, final String clientSecret, final int attempt,
                                            final String nextLink, final boolean isDuringRegistration,
                                            final ApiCallback<RequestEmailValidationResponse> callback) {
        final String description = "requestEmailValidationToken";

        RequestEmailValidationParams params = new RequestEmailValidationParams();
        params.email = address;
        params.clientSecret = clientSecret;
        params.sendAttempt = attempt;

        // Smart default to hs config
        if (identityServerUri == null) {
            identityServerUri = mHsConfig.getIdentityServerUri();
        }
        if (identityServerUri == null) {
            callback.onUnexpectedError(new IdentityServerNotConfiguredException());
            return;
        }

        params.id_server = identityServerUri.getHost();
        if (!TextUtils.isEmpty(nextLink)) {
            params.next_link = nextLink;
        }

        Uri finalIdentityServerUri = identityServerUri;
        final RestAdapterCallback<RequestEmailValidationResponse> adapterCallback
                = new RestAdapterCallback<RequestEmailValidationResponse>(description, mUnsentEventsManager, callback,
                new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        requestEmailValidationToken(finalIdentityServerUri, address, clientSecret, attempt, nextLink, isDuringRegistration, callback);
                    }
                }
        ) {
            @Override
            public void success(RequestEmailValidationResponse requestEmailValidationResponse, Response response) {
                onEventSent();
                requestEmailValidationResponse.email = address;
                requestEmailValidationResponse.clientSecret = clientSecret;
                requestEmailValidationResponse.sendAttempt = attempt;

                callback.onSuccess(requestEmailValidationResponse);
            }
        };

        if (isDuringRegistration) {
            // URL differs in that case
            mApi.requestEmailValidationForRegistration(params).enqueue(adapterCallback);
        } else {
            mApi.requestEmailValidation(params).enqueue(adapterCallback);
        }
    }

    /**
     * Request a phone number validation token.
     *
     * @param phoneNumber          the phone number
     * @param countryCode          the country code of the phone number
     * @param clientSecret         the client secret number
     * @param attempt              the attempt count
     * @param isDuringRegistration true if it occurs during a registration flow
     * @param callback             the callback
     */
    public void requestPhoneNumberValidationToken(final String phoneNumber, final String countryCode,
                                                  final String clientSecret, final int attempt,
                                                  final boolean isDuringRegistration, final ApiCallback<RequestPhoneNumberValidationResponse> callback) {
        final String description = "requestPhoneNumberValidationToken";

        RequestPhoneNumberValidationParams params = new RequestPhoneNumberValidationParams();
        params.phone_number = phoneNumber;
        params.country = countryCode;
        params.clientSecret = clientSecret;
        params.sendAttempt = attempt;

        // TODO privacy: there is something to do here, we cannot use identity server from hsConfig
        Uri identityServerUri = mHsConfig.getIdentityServerUri();
        if (identityServerUri == null) {
            callback.onUnexpectedError(new IdentityServerNotConfiguredException());
            return;
        }

        params.id_server = identityServerUri.getHost();

        final RestAdapterCallback<RequestPhoneNumberValidationResponse> adapterCallback
                = new RestAdapterCallback<RequestPhoneNumberValidationResponse>(description, mUnsentEventsManager, callback,
                new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        requestPhoneNumberValidationToken(phoneNumber, countryCode, clientSecret, attempt, isDuringRegistration, callback);
                    }
                }
        ) {
            @Override
            public void success(RequestPhoneNumberValidationResponse requestPhoneNumberValidationResponse, Response response) {
                onEventSent();
                requestPhoneNumberValidationResponse.clientSecret = clientSecret;
                requestPhoneNumberValidationResponse.sendAttempt = attempt;

                callback.onSuccess(requestPhoneNumberValidationResponse);
            }
        };

        if (isDuringRegistration) {
            // URL differs in that case
            mApi.requestPhoneNumberValidationForRegistration(params).enqueue(adapterCallback);
        } else {
            mApi.requestPhoneNumberValidation(params).enqueue(adapterCallback);
        }
    }

    /**
     * Add an 3Pids to an user
     *
     * @param pid      the 3Pid to add
     * @param bind     bind the email
     * @param callback the asynchronous callback called with the response
     */
    public void add3PID(Uri identityServerUri, final ThreePid pid, final boolean bind, final ApiCallback<Void> callback) {
        final String description = "add3PID";

        AddThreePidsParams params = new AddThreePidsParams();
        params.three_pid_creds = new ThreePidCreds();

        // Smart default to hs config
        if (identityServerUri == null) {
            identityServerUri = mHsConfig.getIdentityServerUri();
        }
        if (identityServerUri == null) {
            callback.onUnexpectedError(new IdentityServerNotConfiguredException());
            return;
        }

        String identityServerHost = identityServerUri.toString();
        if (identityServerHost.startsWith("http://")) {
            identityServerHost = identityServerHost.substring("http://".length());
        } else if (identityServerHost.startsWith("https://")) {
            identityServerHost = identityServerHost.substring("https://".length());
        }

        params.three_pid_creds.id_server = identityServerHost;
        params.three_pid_creds.sid = pid.sid;
        params.three_pid_creds.client_secret = pid.clientSecret;

        params.bind = bind;

        Uri finalIdentityServerUri = identityServerUri;
        mApi.add3PID(params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        add3PID(finalIdentityServerUri, pid, bind, callback);
                    }
                }));
    }

    /**
     * Delete a 3pid of the user
     *
     * @param pid      the 3Pid to delete
     * @param callback the asynchronous callback called with the response
     */
    public void delete3PID(final ThirdPartyIdentifier pid, final ApiCallback<Void> callback) {
        final String description = "delete3PID";

        final DeleteThreePidParams params = new DeleteThreePidParams();
        params.medium = pid.medium;
        params.address = pid.address;

        mApi.delete3PID(params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                delete3PID(pid, callback);
                            }
                        })
                );
    }
}
