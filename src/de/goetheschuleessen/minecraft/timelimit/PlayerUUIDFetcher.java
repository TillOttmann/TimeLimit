package de.goetheschuleessen.minecraft.timelimit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PlayerUUIDFetcher {
	
	static String getUUID(String username) {
		
		try {
			URI uri = new URI("https://playerdb.co/api/player/minecraft/" + username);
			HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			
			if (connection.getResponseCode() == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuilder response = new StringBuilder();
				
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				
				JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
				String uuid = jsonResponse.getAsJsonObject("data").getAsJsonObject("player").get("id").getAsString();
				
				return uuid;
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return "";
	}
}
