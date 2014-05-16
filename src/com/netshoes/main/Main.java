package com.netshoes.main;

import java.util.Scanner;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class Main {
	/**
	 * Netshoes app Id at Facebook
	 */
	private static String appId = "584226881641018";
	/**
	 * Netshoes app password
	 */
	private static String appSecure = "9defd12aba5f591912b629bcc2215439";
	/**
	 * Netshoes app access token
	 */
	private static String appAccessToken = "69013158914213541ccae66be5ba59b4";
	/**
	 * The Netshoes endpopint URL, it must be the same defined at Facebook
	 */
	private static String callback = "http://localhost:8080/WS/teste/";
	/**
	 * Facebook endpoint url to authenticate the token
	 */
	private static String tokenAuthentitorUrl = "oauth/access_token";
	/**
	 * Facebook endpoint url to create access token from a access code
	 */
	private static String accessCodeAuthentitorUrl = "oauth/client_code";
	/**
	 * facebook graph url, you must use it on every request, this URL is "https://graph.facebook.com/"
	 */
	private static String graphUrl = "https://graph.facebook.com/";
	
	
	public static void main(String[] args) {
		Main main = new Main();
		
		main.requestAccessCodeFromTerminal();
	}
	
	/**
	 * Store the Access Token inside a file
	 * 
	 * @param token A {@code String} with the access token
	 */
	private void storeAccessToken(String token) {
		System.out.println("Token de acesso recuperado = " + token);
	}
	
	/**
	 * Creates a authentication service
	 * 
	 * @return An <code>OAuthService</code> object to create urls, add parameters and other
	 */
	public static OAuthService getAuthService() {
		return new ServiceBuilder()
			.provider(FacebookApi.class)
			.apiKey(appId)
			.apiSecret(appSecure)
			.callback(callback)
			.build();
	}
	
	/**
	 * Request at Facebook an access code to generate an access token wich will be received by the
	 * <link>com.netshoes.svc.ws.AccessCodeReceiverImpl</link>
	 */
	public void requestAccessCodeFromTerminal() {
		
		Token empty_token = null;
		
		OAuthService oAuthService = getAuthService();
		
		System.out.println("Fetching the Authorization URL...");
	    String authorizationUrl = oAuthService.getAuthorizationUrl(empty_token);
	    System.out.println("Authorize Facebook Audience Manager on this URL:");
	    System.out.println(authorizationUrl);
	    System.out.println("Paste the authorization code here");
	    System.out.print(">>");
	    Scanner in = new Scanner(System.in);
	    String accessCode = in.nextLine();
	    System.out.println();
		in.close();
		generateNewAccessToken(accessCode);
	}
	
	/**
	 * create a new short lived access token
	 * 
	 * @param accessCode the user access code previously retrieved at Facebook
	 * @return <code>TRUE</code> if the access token was successfully generated, <code>FALSE</code> otherwise.
	 */
	public Boolean generateNewAccessToken(String accessCode) {
		Boolean answer = Boolean.FALSE;
		//Creates a service to authenticate
		OAuthService authService = getAuthService();
		//Creates the app access token to identify the Netshoes Custom Audience app
		Token requestToken = new Token(appAccessToken, appSecure);
		//Checks if the access code is valid
		Verifier verifier = new Verifier(accessCode);
		//Retrives the short lived access token
		Token shortToken = authService.getAccessToken(requestToken, verifier);
		
		if (!shortToken.isEmpty()) {
			answer = true;
			System.out.println("Token criado!");
			
		} else {
			answer = false;
			System.out.println("Token não foi criado!");;
		}
		//Send it to the access token manager
		manageAccessToken(shortToken);
		return answer;
	}
	
	/**
	 * Generate a long lived access token
	 * 
	 * @param fbAppShortLivedToken a previously created short lived access token
	 * @return a long lived access token, which can be used to sign requests against Facebook endpoints
	 */
	private static Token generateFbAppLongLivedAccessToken(Token fbAppShortLivedToken) {
		System.out.println("Criando token de acesso extendido");
		Token fbAppLongLivedAccessToken = null;
		
		//Setting the request type
		OAuthRequest request = new OAuthRequest(Verb.GET, graphUrl + tokenAuthentitorUrl);
		
		//The redirect URI is the Netshoes callback URL
		request.addQuerystringParameter("redirect_uri", callback);
		//Grant type, is the type of the request, which means that we want to replace the old token with new one
		request.addQuerystringParameter("grant_type", "fb_exchange_token");
		//client_id is the Netshoes Custom Audience app Id inside Facebook
		request.addQuerystringParameter("client_id", appId);
		//client_secret is the Netshoes Custom Audience password
		request.addQuerystringParameter("client_secret", appSecure);
		//fb_exchange_token is the old access token, which will be replaced
		request.addQuerystringParameter("fb_exchange_token", fbAppShortLivedToken.getToken());
		//To sign this request we will use the short lived access token
		request.addQuerystringParameter("access_token", fbAppShortLivedToken.getToken());
		
		//Send the request and retrieve it's response
		Response response = request.send();
		
		if (response != null) {
			String responseBody = response.getBody();
			
			if (responseBody != null && responseBody.contains("access_token=")) {
				//Retrieves the requested token from the response
				fbAppLongLivedAccessToken = new Token(responseBody.split("=")[1].trim(), "");
				System.out.println("token criado = " + fbAppLongLivedAccessToken);
			}
			
		} else {
			System.out.println("não foi possível criar o token de acesso, token de acesso restrito = " + fbAppShortLivedToken);
		}
		
		return fbAppLongLivedAccessToken;
	}
	
	/**
	 * Method that generate an extended access token (valid for 60 days) and stores it
	 * 
	 * @param longLivedAccessToken a previously created long lived access token
	 * @return a {@code String} containing the new access token
	 */
	public String generateExtendedAccessToken(String longLivedAccessToken) {
		String extendedToken = "";
		
		System.out.println("Extendendo token de acesso");;
		
		//Set the request type
		OAuthRequest authRequest = new OAuthRequest(Verb.GET, accessCodeAuthentitorUrl);
		
		//the access_token is the long lived access token previously created
		authRequest.addQuerystringParameter("access_token", longLivedAccessToken);
		//client_id is the Netshoes Custom Audience app Id inside Facebook
		authRequest.addQuerystringParameter("client_id", appId);
		//client_secure is the Netshoes Custom Audience password
		authRequest.addQuerystringParameter("client_secret", appSecure);
		//redirect_uri is the callback registered at facebook
		authRequest.addQuerystringParameter("redirect_uri", callback);
		//Grant type, is the type of the request, which means that we want to replace the old token with new one
		authRequest.addQuerystringParameter("grant_type", "exchange_token");
		//fb_exchange_token is the old access token, which will be replaced		
		authRequest.addQuerystringParameter("fb_exchange_token", longLivedAccessToken);
		
		//Send te request and retrieve the response
		Response response = authRequest.send();
		
		if (response != null && response.getBody().contains("access_token")) {
			//Store te created access token, so it can be retrieved later
			System.out.println("O token de acesso foi renovado");
			storeAccessToken(response.getBody());
		
		} else {
			System.out.println("não foi possível re autorizar o token de acesso");
		}
		return extendedToken;
	}
	
	/**
	 * Converts a shotLivedToken into a extendedLivedToken
	 * 
	 * @param shortLivedAccessToken a <code>token</code> to convert
	 */
	private void manageAccessToken(Token shortLivedAccessToken) {
		//Creates a long lived access token, wich have a valid of 60 days
		Token longLivedAccessToken = generateFbAppLongLivedAccessToken(shortLivedAccessToken);
		//Creates a new long lived access token, the answer will have it's expiration time
		generateExtendedAccessToken(longLivedAccessToken.getToken());
	}
}
